package com.hyperfactions.manager;

import com.hyperfactions.config.HyperFactionsConfig;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.TeleportContext;
import com.hyperfactions.integration.HyperPermsIntegration;
import com.hyperfactions.util.Logger;
import com.hyperfactions.util.TimeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Manages faction home teleportation with warmup and cooldown.
 */
public class TeleportManager {

    private final FactionManager factionManager;

    // Pending teleports: player UUID -> PendingTeleport
    private final Map<UUID, PendingTeleport> pendingTeleports = new ConcurrentHashMap<>();

    // Cooldowns: player UUID -> cooldown end time
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    /**
     * Represents a pending teleport.
     */
    public record PendingTeleport(
        UUID playerUuid,
        UUID factionId,
        StartLocation startLocation,
        long startTime,
        int warmupSeconds,
        int taskId,
        Consumer<TeleportResult> callback
    ) {}

    /**
     * Represents a starting location for movement checking.
     */
    public record StartLocation(String world, double x, double y, double z) {}

    /**
     * Result of a teleport attempt.
     */
    public enum TeleportResult {
        SUCCESS_INSTANT,     // Teleport completed immediately (no warmup)
        SUCCESS_WARMUP,      // Warmup scheduled, teleport pending
        NO_HOME,
        NOT_IN_FACTION,
        ON_COOLDOWN,
        COMBAT_TAGGED,
        CANCELLED_MOVED,
        CANCELLED_DAMAGE,
        CANCELLED_MANUAL,
        WORLD_NOT_FOUND
    }

    public TeleportManager(@NotNull FactionManager factionManager) {
        this.factionManager = factionManager;
    }

    // === Queries ===

    /**
     * Checks if a player has a pending teleport.
     *
     * @param playerUuid the player's UUID
     * @return true if pending
     */
    public boolean hasPending(@NotNull UUID playerUuid) {
        return pendingTeleports.containsKey(playerUuid);
    }

    /**
     * Gets the pending teleport for a player.
     *
     * @param playerUuid the player's UUID
     * @return the pending teleport, or null
     */
    @Nullable
    public PendingTeleport getPending(@NotNull UUID playerUuid) {
        return pendingTeleports.get(playerUuid);
    }

    /**
     * Checks if a player is on cooldown.
     *
     * @param playerUuid the player's UUID
     * @return true if on cooldown
     */
    public boolean isOnCooldown(@NotNull UUID playerUuid) {
        Long endTime = cooldowns.get(playerUuid);
        if (endTime == null) {
            return false;
        }
        if (System.currentTimeMillis() >= endTime) {
            cooldowns.remove(playerUuid);
            return false;
        }
        return true;
    }

    /**
     * Gets the remaining cooldown time in seconds.
     *
     * @param playerUuid the player's UUID
     * @return remaining seconds, 0 if not on cooldown
     */
    public int getCooldownRemaining(@NotNull UUID playerUuid) {
        Long endTime = cooldowns.get(playerUuid);
        if (endTime == null) {
            return 0;
        }
        long remaining = endTime - System.currentTimeMillis();
        if (remaining <= 0) {
            cooldowns.remove(playerUuid);
            return 0;
        }
        return (int) Math.ceil(remaining / 1000.0);
    }

    // === Operations ===

    /**
     * Initiates a teleport to faction home.
     *
     * @param playerUuid    the player's UUID
     * @param startLocation the player's starting location
     * @param scheduleTask  function to schedule a delayed task, returns task ID
     * @param cancelTask    function to cancel a task by ID
     * @param doTeleport    function to actually perform the teleport
     * @param sendMessage   function to send a message to the player
     * @param isTagged      supplier to check if player is combat tagged
     * @return the initial result
     */
    public TeleportResult teleportToHome(
        @NotNull UUID playerUuid,
        @NotNull StartLocation startLocation,
        @NotNull TaskScheduler scheduleTask,
        @NotNull Consumer<Integer> cancelTask,
        @NotNull TeleportExecutor doTeleport,
        @NotNull Consumer<String> sendMessage,
        @NotNull java.util.function.Supplier<Boolean> isTagged
    ) {
        HyperFactionsConfig config = HyperFactionsConfig.get();

        // Get player's faction
        Faction faction = factionManager.getPlayerFaction(playerUuid);
        if (faction == null) {
            return TeleportResult.NOT_IN_FACTION;
        }

        // Check if faction has home
        if (!faction.hasHome()) {
            return TeleportResult.NO_HOME;
        }

        // Check combat tag
        if (isTagged.get()) {
            return TeleportResult.COMBAT_TAGGED;
        }

        // Check cooldown
        if (!HyperPermsIntegration.hasPermission(playerUuid, "hyperfactions.bypass.cooldown")) {
            if (isOnCooldown(playerUuid)) {
                int remaining = getCooldownRemaining(playerUuid);
                sendMessage.accept(config.getPrefix() + "\u00A7cYou must wait " +
                    TimeUtil.formatDurationSeconds(remaining) + " before teleporting again.");
                return TeleportResult.ON_COOLDOWN;
            }
        }

        // Cancel any existing pending teleport
        cancelPending(playerUuid, cancelTask);

        // Check if warmup is needed
        int warmup = getWarmupSeconds(playerUuid);
        if (warmup <= 0) {
            // Instant teleport
            TeleportResult result = doTeleport.execute(faction);
            if (result == TeleportResult.SUCCESS_INSTANT) {
                applyCooldown(playerUuid);
            }
            return result;
        }

        // Send warmup message
        sendMessage.accept(config.getPrefix() + "\u00A7eTeleporting to faction home in " + warmup + " seconds...");

        // Schedule the teleport
        int taskId = scheduleTask.schedule(warmup * 20, () -> {
            PendingTeleport pending = pendingTeleports.remove(playerUuid);
            if (pending != null) {
                // Recheck combat tag
                if (isTagged.get()) {
                    sendMessage.accept(config.getPrefix() + "\u00A7cTeleportation cancelled - you are in combat!");
                    return;
                }

                TeleportResult result = doTeleport.execute(faction);
                handleResult(result, sendMessage);
                if (result == TeleportResult.SUCCESS_INSTANT) {
                    applyCooldown(playerUuid);
                }
            }
        });

        // Store pending teleport
        PendingTeleport pending = new PendingTeleport(
            playerUuid, faction.id(), startLocation,
            System.currentTimeMillis(), warmup, taskId, null
        );
        pendingTeleports.put(playerUuid, pending);

        return TeleportResult.SUCCESS_WARMUP;
    }

    /**
     * Initiates a teleport to faction home using a TeleportContext.
     * This is the recommended method signature for cleaner code.
     *
     * @param context the teleport context containing all required parameters
     * @return the initial result
     */
    public TeleportResult teleportToHome(@NotNull TeleportContext context) {
        return teleportToHome(
            context.playerUuid(),
            context.startLocation(),
            context.scheduleTask(),
            context.cancelTask(),
            context.doTeleport(),
            context.sendMessage(),
            context.isTagged()
        );
    }

    /**
     * Checks if a player moved and should cancel teleport.
     *
     * @param playerUuid the player's UUID
     * @param currentX   current X position
     * @param currentY   current Y position
     * @param currentZ   current Z position
     * @param cancelTask function to cancel a task
     * @param sendMessage function to send a message
     * @return true if teleport was cancelled
     */
    public boolean checkMovement(
        @NotNull UUID playerUuid,
        double currentX, double currentY, double currentZ,
        @NotNull Consumer<Integer> cancelTask,
        @NotNull Consumer<String> sendMessage
    ) {
        if (!HyperFactionsConfig.get().isCancelOnMove()) {
            return false;
        }

        PendingTeleport pending = pendingTeleports.get(playerUuid);
        if (pending == null) {
            return false;
        }

        StartLocation start = pending.startLocation();
        double dx = currentX - start.x();
        double dy = currentY - start.y();
        double dz = currentZ - start.z();
        double distSq = dx * dx + dy * dy + dz * dz;

        if (distSq > 0.25) { // 0.5 blocks
            cancelPending(playerUuid, cancelTask);
            sendMessage.accept(HyperFactionsConfig.get().getPrefix() + "\u00A7cTeleportation cancelled - you moved!");
            return true;
        }

        return false;
    }

    /**
     * Cancels teleport due to damage.
     *
     * @param playerUuid  the player's UUID
     * @param cancelTask  function to cancel a task
     * @param sendMessage function to send a message
     * @return true if teleport was cancelled
     */
    public boolean cancelOnDamage(
        @NotNull UUID playerUuid,
        @NotNull Consumer<Integer> cancelTask,
        @NotNull Consumer<String> sendMessage
    ) {
        if (!HyperFactionsConfig.get().isCancelOnDamage()) {
            return false;
        }

        if (pendingTeleports.containsKey(playerUuid)) {
            cancelPending(playerUuid, cancelTask);
            sendMessage.accept(HyperFactionsConfig.get().getPrefix() + "\u00A7cTeleportation cancelled - you took damage!");
            return true;
        }

        return false;
    }

    /**
     * Cancels a pending teleport.
     *
     * @param playerUuid the player's UUID
     * @param cancelTask function to cancel a task
     */
    public void cancelPending(@NotNull UUID playerUuid, @NotNull Consumer<Integer> cancelTask) {
        PendingTeleport pending = pendingTeleports.remove(playerUuid);
        if (pending != null) {
            cancelTask.accept(pending.taskId());
            Logger.debug("Cancelled pending teleport for %s", playerUuid);
        }
    }

    /**
     * Gets the warmup seconds for a player.
     *
     * @param playerUuid the player's UUID
     * @return warmup seconds, 0 if bypassed
     */
    private int getWarmupSeconds(@NotNull UUID playerUuid) {
        if (HyperPermsIntegration.hasPermission(playerUuid, "hyperfactions.bypass.warmup")) {
            return 0;
        }
        return HyperFactionsConfig.get().getWarmupSeconds();
    }

    /**
     * Applies cooldown to a player.
     *
     * @param playerUuid the player's UUID
     */
    private void applyCooldown(@NotNull UUID playerUuid) {
        if (HyperPermsIntegration.hasPermission(playerUuid, "hyperfactions.bypass.cooldown")) {
            return;
        }
        int cooldownSeconds = HyperFactionsConfig.get().getCooldownSeconds();
        if (cooldownSeconds > 0) {
            cooldowns.put(playerUuid, System.currentTimeMillis() + (cooldownSeconds * 1000L));
        }
    }

    /**
     * Handles the teleport result.
     */
    private void handleResult(@NotNull TeleportResult result, @NotNull Consumer<String> sendMessage) {
        HyperFactionsConfig config = HyperFactionsConfig.get();
        switch (result) {
            case SUCCESS_INSTANT -> sendMessage.accept(config.getPrefix() + "\u00A7aTeleported to faction home!");
            case NO_HOME -> sendMessage.accept(config.getPrefix() + "\u00A7cYour faction has no home set.");
            case WORLD_NOT_FOUND -> sendMessage.accept(config.getPrefix() + "\u00A7cWorld not found.");
            case COMBAT_TAGGED -> sendMessage.accept(config.getPrefix() + "\u00A7cYou cannot teleport while in combat!");
            default -> {}
        }
    }

    /**
     * Functional interface for scheduling tasks.
     */
    @FunctionalInterface
    public interface TaskScheduler {
        int schedule(int delayTicks, Runnable task);
    }

    /**
     * Functional interface for executing teleports.
     */
    @FunctionalInterface
    public interface TeleportExecutor {
        TeleportResult execute(Faction faction);
    }
}
