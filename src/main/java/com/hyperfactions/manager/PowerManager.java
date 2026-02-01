package com.hyperfactions.manager;

import com.hyperfactions.config.HyperFactionsConfig;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.PlayerPower;
import com.hyperfactions.storage.PlayerStorage;
import com.hyperfactions.util.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages player power for faction mechanics.
 */
public class PowerManager {

    private final PlayerStorage storage;
    private final FactionManager factionManager;

    // Cache: player UUID -> PlayerPower
    private final Map<UUID, PlayerPower> powerCache = new ConcurrentHashMap<>();

    // Track online players for regen
    private final Set<UUID> onlinePlayers = ConcurrentHashMap.newKeySet();

    public PowerManager(@NotNull PlayerStorage storage, @NotNull FactionManager factionManager) {
        this.storage = storage;
        this.factionManager = factionManager;
    }

    /**
     * Loads all player power data from storage.
     * <p>
     * SAFETY: This method will NOT clear existing data if loading fails or returns
     * suspiciously empty results when data was expected.
     *
     * @return a future that completes when loading is done
     */
    public CompletableFuture<Void> loadAll() {
        final int previousCount = powerCache.size();

        return storage.loadAllPlayerPower().thenAccept(loaded -> {
            // SAFETY CHECK: If we had data before but loading returned nothing,
            // this is likely a load failure - DO NOT clear existing data
            if (previousCount > 0 && loaded.isEmpty()) {
                Logger.severe("CRITICAL: Load returned 0 player power records but %d were previously loaded!",
                    previousCount);
                Logger.severe("Keeping existing in-memory data to prevent data loss.");
                return;
            }

            // Build new cache before clearing old one
            Map<UUID, PlayerPower> newCache = new HashMap<>();
            for (PlayerPower power : loaded) {
                newCache.put(power.uuid(), power);
            }

            // Atomic swap
            powerCache.clear();
            powerCache.putAll(newCache);

            Logger.info("Loaded %d player power records", powerCache.size());
        }).exceptionally(ex -> {
            Logger.severe("CRITICAL: Exception during player power loading - keeping existing data", (Throwable) ex);
            return null;
        });
    }

    /**
     * Saves all player power data to storage.
     *
     * @return a future that completes when saving is done
     */
    public CompletableFuture<Void> saveAll() {
        List<CompletableFuture<Void>> futures = powerCache.values().stream()
            .map(storage::savePlayerPower)
            .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    // === Player Operations ===

    /**
     * Gets or creates power data for a player.
     *
     * @param playerUuid the player's UUID
     * @return the player power data
     */
    @NotNull
    public PlayerPower getPlayerPower(@NotNull UUID playerUuid) {
        return powerCache.computeIfAbsent(playerUuid, uuid -> {
            HyperFactionsConfig config = HyperFactionsConfig.get();
            return PlayerPower.create(uuid, config.getStartingPower(), config.getMaxPlayerPower());
        });
    }

    /**
     * Loads player power, creating default if not exists.
     *
     * @param playerUuid the player's UUID
     * @return a future containing the player power
     */
    public CompletableFuture<PlayerPower> loadPlayer(@NotNull UUID playerUuid) {
        // Check cache first
        PlayerPower cached = powerCache.get(playerUuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return storage.loadPlayerPower(playerUuid).thenApply(opt -> {
            PlayerPower power = opt.orElseGet(() -> {
                HyperFactionsConfig config = HyperFactionsConfig.get();
                return PlayerPower.create(playerUuid, config.getStartingPower(), config.getMaxPlayerPower());
            });
            powerCache.put(playerUuid, power);
            return power;
        });
    }

    /**
     * Marks a player as online for power regen.
     *
     * @param playerUuid the player's UUID
     */
    public void playerOnline(@NotNull UUID playerUuid) {
        onlinePlayers.add(playerUuid);
        loadPlayer(playerUuid); // Ensure loaded
    }

    /**
     * Marks a player as offline.
     *
     * @param playerUuid the player's UUID
     */
    public void playerOffline(@NotNull UUID playerUuid) {
        onlinePlayers.remove(playerUuid);

        // Save their power data
        PlayerPower power = powerCache.get(playerUuid);
        if (power != null) {
            storage.savePlayerPower(power);
        }
    }

    /**
     * Applies death penalty to a player.
     *
     * @param playerUuid the player's UUID
     * @return the new power level
     */
    public double applyDeathPenalty(@NotNull UUID playerUuid) {
        PlayerPower power = getPlayerPower(playerUuid);
        double penalty = HyperFactionsConfig.get().getDeathPenalty();

        PlayerPower updated = power.withDeathPenalty(penalty);
        powerCache.put(playerUuid, updated);
        storage.savePlayerPower(updated);

        Logger.debugPower("Death penalty: player=%s, before=%.2f, after=%.2f, penalty=%.2f, max=%.2f",
            playerUuid, power.power(), updated.power(), penalty, power.maxPower());
        return updated.power();
    }

    /**
     * Regenerates power for a player.
     *
     * @param playerUuid the player's UUID
     * @param amount     the amount to regenerate
     */
    public void regeneratePower(@NotNull UUID playerUuid, double amount) {
        PlayerPower power = powerCache.get(playerUuid);
        if (power == null || power.isAtMax()) {
            return;
        }

        PlayerPower updated = power.withRegen(amount);
        powerCache.put(playerUuid, updated);

        Logger.debugPower("Regen: player=%s, before=%.2f, after=%.2f, amount=%.2f, max=%.2f",
            playerUuid, power.power(), updated.power(), amount, power.maxPower());
        // Don't save immediately - batch save periodically
    }

    /**
     * Called periodically to regenerate power for online players.
     */
    public void tickPowerRegen() {
        HyperFactionsConfig config = HyperFactionsConfig.get();
        double regenAmount = config.getRegenPerMinute();

        if (regenAmount <= 0) {
            return;
        }

        Set<UUID> playersToRegen = config.isRegenWhenOffline()
            ? new HashSet<>(powerCache.keySet())
            : new HashSet<>(onlinePlayers);

        for (UUID playerUuid : playersToRegen) {
            regeneratePower(playerUuid, regenAmount);
        }
    }

    // === Faction Power ===

    /**
     * Gets the total power of a faction.
     *
     * @param factionId the faction ID
     * @return the total power
     */
    public double getFactionPower(@NotNull UUID factionId) {
        Faction faction = factionManager.getFaction(factionId);
        if (faction == null) {
            return 0;
        }

        double total = 0;
        for (UUID memberUuid : faction.members().keySet()) {
            total += getPlayerPower(memberUuid).power();
        }
        return total;
    }

    /**
     * Gets the maximum power of a faction.
     *
     * @param factionId the faction ID
     * @return the maximum power
     */
    public double getFactionMaxPower(@NotNull UUID factionId) {
        Faction faction = factionManager.getFaction(factionId);
        if (faction == null) {
            return 0;
        }

        double total = 0;
        for (UUID memberUuid : faction.members().keySet()) {
            total += getPlayerPower(memberUuid).maxPower();
        }
        return total;
    }

    /**
     * Gets the claim capacity for a faction based on power.
     *
     * @param factionId the faction ID
     * @return the max claims allowed
     */
    public int getFactionClaimCapacity(@NotNull UUID factionId) {
        double power = getFactionPower(factionId);
        return HyperFactionsConfig.get().calculateMaxClaims(power);
    }

    /**
     * Checks if a faction is raidable (claims > power-based limit).
     *
     * @param factionId the faction ID
     * @return true if raidable
     */
    public boolean isFactionRaidable(@NotNull UUID factionId) {
        Faction faction = factionManager.getFaction(factionId);
        if (faction == null) {
            return false;
        }

        int claimCapacity = getFactionClaimCapacity(factionId);
        return faction.getClaimCount() > claimCapacity;
    }

    /**
     * Gets power statistics for display.
     *
     * @param factionId the faction ID
     * @return power stats
     */
    @NotNull
    public FactionPowerStats getFactionPowerStats(@NotNull UUID factionId) {
        double current = getFactionPower(factionId);
        double max = getFactionMaxPower(factionId);
        int claims = 0;
        int claimCapacity = getFactionClaimCapacity(factionId);

        Faction faction = factionManager.getFaction(factionId);
        if (faction != null) {
            claims = faction.getClaimCount();
        }

        return new FactionPowerStats(current, max, claims, claimCapacity);
    }

    /**
     * Power statistics for a faction.
     */
    public record FactionPowerStats(
        double currentPower,
        double maxPower,
        int currentClaims,
        int maxClaims
    ) {
        public int getPowerPercent() {
            if (maxPower <= 0) return 0;
            return (int) Math.round((currentPower / maxPower) * 100);
        }

        public boolean isRaidable() {
            return currentClaims > maxClaims;
        }

        public int getClaimDeficit() {
            return Math.max(0, currentClaims - maxClaims);
        }
    }
}
