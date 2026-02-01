package com.hyperfactions.manager;

import com.hyperfactions.Permissions;
import com.hyperfactions.config.HyperFactionsConfig;
import com.hyperfactions.data.ChunkKey;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionClaim;
import com.hyperfactions.data.FactionLog;
import com.hyperfactions.integration.PermissionManager;
import com.hyperfactions.util.ChunkUtil;
import com.hyperfactions.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages territory claims with O(1) chunk lookups.
 */
public class ClaimManager {

    private final FactionManager factionManager;
    private final PowerManager powerManager;

    // Index: ChunkKey -> faction ID for fast lookups
    private final Map<ChunkKey, UUID> claimIndex = new ConcurrentHashMap<>();

    // Reverse index: faction ID -> Set<ChunkKey> for O(1) getFactionClaims()
    private final Map<UUID, Set<ChunkKey>> factionClaimsIndex = new ConcurrentHashMap<>();

    // Callback for when claims change (used to refresh world map)
    @Nullable
    private Runnable onClaimChangeCallback;

    public ClaimManager(@NotNull FactionManager factionManager, @NotNull PowerManager powerManager) {
        this.factionManager = factionManager;
        this.powerManager = powerManager;
    }

    /**
     * Sets a callback to be invoked when claims change.
     * Used to trigger world map refresh.
     *
     * @param callback the callback to run on claim changes
     */
    public void setOnClaimChangeCallback(@Nullable Runnable callback) {
        this.onClaimChangeCallback = callback;
    }

    /**
     * Notifies that claims have changed (triggers world map refresh).
     */
    private void notifyClaimChange() {
        Logger.debugClaim("Claim change notification triggered");
        if (onClaimChangeCallback != null) {
            try {
                onClaimChangeCallback.run();
            } catch (Exception e) {
                Logger.warn("Error in claim change callback: %s", e.getMessage());
            }
        }
    }

    /**
     * Builds the claim index from all factions.
     * Call after FactionManager.loadAll()
     */
    public void buildIndex() {
        claimIndex.clear();
        factionClaimsIndex.clear();

        for (Faction faction : factionManager.getAllFactions()) {
            Set<ChunkKey> factionClaims = ConcurrentHashMap.newKeySet();
            for (FactionClaim claim : faction.claims()) {
                ChunkKey key = claim.toChunkKey();
                claimIndex.put(key, faction.id());
                factionClaims.add(key);
            }
            if (!factionClaims.isEmpty()) {
                factionClaimsIndex.put(faction.id(), factionClaims);
            }
        }

        Logger.info("Built claim index with %d claims for %d factions", claimIndex.size(), factionClaimsIndex.size());
    }

    /**
     * Result of a claim operation.
     */
    public enum ClaimResult {
        SUCCESS,
        NO_PERMISSION,
        NOT_IN_FACTION,
        NOT_OFFICER,
        ALREADY_CLAIMED_SELF,
        ALREADY_CLAIMED_OTHER,
        ALREADY_CLAIMED_ALLY,
        ALREADY_CLAIMED_ENEMY,
        NOT_ADJACENT,
        MAX_CLAIMS_REACHED,
        INSUFFICIENT_POWER,
        WORLD_NOT_ALLOWED,
        CHUNK_NOT_CLAIMED,
        CANNOT_UNCLAIM_HOME,
        NOT_YOUR_CLAIM,
        OVERCLAIM_NOT_ALLOWED,
        TARGET_HAS_POWER
    }

    // === Queries ===

    /**
     * Gets the faction ID that owns a chunk.
     *
     * @param world  the world name
     * @param chunkX the chunk X
     * @param chunkZ the chunk Z
     * @return the faction ID, or null if unclaimed
     */
    @Nullable
    public UUID getClaimOwner(@NotNull String world, int chunkX, int chunkZ) {
        return claimIndex.get(new ChunkKey(world, chunkX, chunkZ));
    }

    /**
     * Gets the faction ID that owns a chunk at world coordinates.
     *
     * @param world the world name
     * @param x     the world X coordinate
     * @param z     the world Z coordinate
     * @return the faction ID, or null if unclaimed
     */
    @Nullable
    public UUID getClaimOwnerAt(@NotNull String world, double x, double z) {
        return claimIndex.get(ChunkKey.fromWorldCoords(world, x, z));
    }

    /**
     * Checks if a chunk is claimed.
     *
     * @param world  the world name
     * @param chunkX the chunk X
     * @param chunkZ the chunk Z
     * @return true if claimed
     */
    public boolean isClaimed(@NotNull String world, int chunkX, int chunkZ) {
        return claimIndex.containsKey(new ChunkKey(world, chunkX, chunkZ));
    }

    /**
     * Checks if any adjacent chunk is owned by the given faction.
     *
     * @param world     the world name
     * @param chunkX    the chunk X
     * @param chunkZ    the chunk Z
     * @param factionId the faction ID
     * @return true if at least one adjacent chunk is owned by the faction
     */
    public boolean hasAdjacentClaim(@NotNull String world, int chunkX, int chunkZ, @NotNull UUID factionId) {
        ChunkKey key = new ChunkKey(world, chunkX, chunkZ);
        return factionId.equals(claimIndex.get(key.north())) ||
               factionId.equals(claimIndex.get(key.south())) ||
               factionId.equals(claimIndex.get(key.east())) ||
               factionId.equals(claimIndex.get(key.west()));
    }

    /**
     * Gets the total claim count.
     *
     * @return number of claimed chunks
     */
    public int getTotalClaimCount() {
        return claimIndex.size();
    }

    // === Operations ===

    /**
     * Claims a chunk for a faction.
     *
     * @param playerUuid the player claiming (must be in faction)
     * @param world      the world name
     * @param chunkX     the chunk X
     * @param chunkZ     the chunk Z
     * @return the result
     */
    public ClaimResult claim(@NotNull UUID playerUuid, @NotNull String world, int chunkX, int chunkZ) {
        // Check permission first
        if (!PermissionManager.get().hasPermission(playerUuid, Permissions.CLAIM)) {
            return ClaimResult.NO_PERMISSION;
        }

        // Get player's faction
        Faction faction = factionManager.getPlayerFaction(playerUuid);
        if (faction == null) {
            return ClaimResult.NOT_IN_FACTION;
        }

        // Check permission
        var member = faction.getMember(playerUuid);
        if (member == null || !member.isOfficerOrHigher()) {
            return ClaimResult.NOT_OFFICER;
        }

        // Check world
        if (!HyperFactionsConfig.get().isWorldAllowed(world)) {
            return ClaimResult.WORLD_NOT_ALLOWED;
        }

        ChunkKey key = new ChunkKey(world, chunkX, chunkZ);

        // Check if already claimed
        UUID existingOwner = claimIndex.get(key);
        if (existingOwner != null) {
            if (existingOwner.equals(faction.id())) {
                return ClaimResult.ALREADY_CLAIMED_SELF;
            }
            return ClaimResult.ALREADY_CLAIMED_OTHER;
        }

        // Check max claims
        double factionPower = powerManager.getFactionPower(faction.id());
        int maxClaims = HyperFactionsConfig.get().calculateMaxClaims(factionPower);
        if (faction.getClaimCount() >= maxClaims) {
            return ClaimResult.MAX_CLAIMS_REACHED;
        }

        // Check adjacency if required
        HyperFactionsConfig config = HyperFactionsConfig.get();
        if (config.isOnlyAdjacent() && faction.getClaimCount() > 0) {
            if (!hasAdjacentClaim(world, chunkX, chunkZ, faction.id())) {
                return ClaimResult.NOT_ADJACENT;
            }
        }

        // Create claim
        FactionClaim claim = FactionClaim.create(world, chunkX, chunkZ, playerUuid);
        Faction updated = faction.withClaim(claim)
            .withLog(FactionLog.create(FactionLog.LogType.CLAIM,
                String.format("Claimed chunk at %d, %d in %s", chunkX, chunkZ, world), playerUuid));

        // Update indices and faction
        claimIndex.put(key, faction.id());
        factionClaimsIndex.computeIfAbsent(faction.id(), k -> ConcurrentHashMap.newKeySet()).add(key);
        factionManager.updateFaction(updated);

        Logger.debugClaim("Claim success: chunk=%s, faction=%s, player=%s, claimCount=%d/%d",
            key, faction.name(), playerUuid, updated.getClaimCount(), maxClaims);
        notifyClaimChange();
        return ClaimResult.SUCCESS;
    }

    /**
     * Unclaims a chunk.
     *
     * @param playerUuid the player unclaiming
     * @param world      the world name
     * @param chunkX     the chunk X
     * @param chunkZ     the chunk Z
     * @return the result
     */
    public ClaimResult unclaim(@NotNull UUID playerUuid, @NotNull String world, int chunkX, int chunkZ) {
        // Check permission first
        if (!PermissionManager.get().hasPermission(playerUuid, Permissions.UNCLAIM)) {
            return ClaimResult.NO_PERMISSION;
        }

        Faction faction = factionManager.getPlayerFaction(playerUuid);
        if (faction == null) {
            return ClaimResult.NOT_IN_FACTION;
        }

        var member = faction.getMember(playerUuid);
        if (member == null || !member.isOfficerOrHigher()) {
            return ClaimResult.NOT_OFFICER;
        }

        ChunkKey key = new ChunkKey(world, chunkX, chunkZ);
        UUID owner = claimIndex.get(key);

        if (owner == null) {
            return ClaimResult.CHUNK_NOT_CLAIMED;
        }

        if (!owner.equals(faction.id())) {
            return ClaimResult.NOT_YOUR_CLAIM;
        }

        // Check if home is in this chunk
        if (faction.hasHome()) {
            var home = faction.home();
            int homeChunkX = ChunkUtil.toChunkCoord(home.x());
            int homeChunkZ = ChunkUtil.toChunkCoord(home.z());
            if (home.world().equals(world) && homeChunkX == chunkX && homeChunkZ == chunkZ) {
                return ClaimResult.CANNOT_UNCLAIM_HOME;
            }
        }

        // Remove claim
        Faction updated = faction.withoutClaimAt(world, chunkX, chunkZ)
            .withLog(FactionLog.create(FactionLog.LogType.UNCLAIM,
                String.format("Unclaimed chunk at %d, %d in %s", chunkX, chunkZ, world), playerUuid));

        claimIndex.remove(key);
        Set<ChunkKey> factionClaims = factionClaimsIndex.get(faction.id());
        if (factionClaims != null) {
            factionClaims.remove(key);
            if (factionClaims.isEmpty()) {
                factionClaimsIndex.remove(faction.id());
            }
        }
        factionManager.updateFaction(updated);

        Logger.debugClaim("Unclaim success: chunk=%s, faction=%s, player=%s",
            key, faction.name(), playerUuid);
        notifyClaimChange();
        return ClaimResult.SUCCESS;
    }

    /**
     * Attempts to overclaim an enemy faction's chunk.
     *
     * @param playerUuid the player overclaiming
     * @param world      the world name
     * @param chunkX     the chunk X
     * @param chunkZ     the chunk Z
     * @return the result
     */
    public ClaimResult overclaim(@NotNull UUID playerUuid, @NotNull String world, int chunkX, int chunkZ) {
        // Check permission first
        if (!PermissionManager.get().hasPermission(playerUuid, Permissions.OVERCLAIM)) {
            return ClaimResult.NO_PERMISSION;
        }

        Faction attackerFaction = factionManager.getPlayerFaction(playerUuid);
        if (attackerFaction == null) {
            return ClaimResult.NOT_IN_FACTION;
        }

        var member = attackerFaction.getMember(playerUuid);
        if (member == null || !member.isOfficerOrHigher()) {
            return ClaimResult.NOT_OFFICER;
        }

        ChunkKey key = new ChunkKey(world, chunkX, chunkZ);
        UUID defenderId = claimIndex.get(key);

        if (defenderId == null) {
            return ClaimResult.CHUNK_NOT_CLAIMED;
        }

        if (defenderId.equals(attackerFaction.id())) {
            return ClaimResult.ALREADY_CLAIMED_SELF;
        }

        Faction defenderFaction = factionManager.getFaction(defenderId);
        if (defenderFaction == null) {
            // Orphaned claim, just take it
            return forceClaimChunk(attackerFaction, playerUuid, world, chunkX, chunkZ);
        }

        // Check if enemy
        if (attackerFaction.isAlly(defenderId)) {
            return ClaimResult.ALREADY_CLAIMED_ALLY;
        }

        // Check defender power
        double defenderPower = powerManager.getFactionPower(defenderId);
        int defenderMaxClaims = HyperFactionsConfig.get().calculateMaxClaims(defenderPower);

        if (defenderFaction.getClaimCount() < defenderMaxClaims) {
            return ClaimResult.TARGET_HAS_POWER;
        }

        // Check attacker can claim
        double attackerPower = powerManager.getFactionPower(attackerFaction.id());
        int attackerMaxClaims = HyperFactionsConfig.get().calculateMaxClaims(attackerPower);
        if (attackerFaction.getClaimCount() >= attackerMaxClaims) {
            return ClaimResult.MAX_CLAIMS_REACHED;
        }

        // Remove from defender
        Faction updatedDefender = defenderFaction.withoutClaimAt(world, chunkX, chunkZ)
            .withLog(FactionLog.create(FactionLog.LogType.OVERCLAIM,
                String.format("Lost chunk at %d, %d to %s", chunkX, chunkZ, attackerFaction.name()), null));

        // Add to attacker
        FactionClaim claim = FactionClaim.create(world, chunkX, chunkZ, playerUuid);
        Faction updatedAttacker = attackerFaction.withClaim(claim)
            .withLog(FactionLog.create(FactionLog.LogType.OVERCLAIM,
                String.format("Overclaimed chunk at %d, %d from %s", chunkX, chunkZ, defenderFaction.name()), playerUuid));

        // Update indices - remove from defender
        Set<ChunkKey> defenderClaims = factionClaimsIndex.get(defenderId);
        if (defenderClaims != null) {
            defenderClaims.remove(key);
            if (defenderClaims.isEmpty()) {
                factionClaimsIndex.remove(defenderId);
            }
        }

        // Update indices - add to attacker
        claimIndex.put(key, attackerFaction.id());
        factionClaimsIndex.computeIfAbsent(attackerFaction.id(), k -> ConcurrentHashMap.newKeySet()).add(key);

        // Update factions
        factionManager.updateFaction(updatedDefender);
        factionManager.updateFaction(updatedAttacker);

        Logger.debugClaim("Overclaim success: chunk=%s, attacker=%s, defender=%s, defenderClaims=%d/%d",
            key, attackerFaction.name(), defenderFaction.name(), defenderFaction.getClaimCount() - 1, defenderMaxClaims);
        Logger.info("Faction '%s' overclaimed chunk from '%s'", attackerFaction.name(), defenderFaction.name());
        notifyClaimChange();
        return ClaimResult.SUCCESS;
    }

    /**
     * Unclaims all chunks for a faction (used when disbanding).
     *
     * @param factionId the faction ID
     */
    public void unclaimAll(@NotNull UUID factionId) {
        // Remove from main index
        claimIndex.entrySet().removeIf(entry -> entry.getValue().equals(factionId));
        // Remove from reverse index
        factionClaimsIndex.remove(factionId);
        notifyClaimChange();
    }

    /**
     * Gets all claims for a faction.
     * O(1) lookup using the reverse index.
     *
     * @param factionId the faction ID
     * @return set of chunk keys (unmodifiable view)
     */
    @NotNull
    public Set<ChunkKey> getFactionClaims(@NotNull UUID factionId) {
        Set<ChunkKey> claims = factionClaimsIndex.get(factionId);
        if (claims == null) {
            return Collections.emptySet();
        }
        // Return unmodifiable view to prevent external modification
        return Collections.unmodifiableSet(claims);
    }

    private ClaimResult forceClaimChunk(Faction faction, UUID playerUuid, String world, int chunkX, int chunkZ) {
        ChunkKey key = new ChunkKey(world, chunkX, chunkZ);
        FactionClaim claim = FactionClaim.create(world, chunkX, chunkZ, playerUuid);

        Faction updated = faction.withClaim(claim)
            .withLog(FactionLog.create(FactionLog.LogType.CLAIM,
                String.format("Claimed chunk at %d, %d in %s", chunkX, chunkZ, world), playerUuid));

        // Update both indices
        claimIndex.put(key, faction.id());
        factionClaimsIndex.computeIfAbsent(faction.id(), k -> ConcurrentHashMap.newKeySet()).add(key);
        factionManager.updateFaction(updated);

        return ClaimResult.SUCCESS;
    }
}
