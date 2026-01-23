package com.hyperfactions.listener;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.protection.ProtectionChecker;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Handles protection-related events for HyperFactions.
 */
public class ProtectionListener {

    private final HyperFactions hyperFactions;

    public ProtectionListener(@NotNull HyperFactions hyperFactions) {
        this.hyperFactions = hyperFactions;
    }

    /**
     * Called when a player attempts to place a block.
     *
     * @param playerUuid the player's UUID
     * @param world      the world name
     * @param x          the block X
     * @param y          the block Y
     * @param z          the block Z
     * @return true if block placement should be cancelled
     */
    public boolean onBlockPlace(@NotNull UUID playerUuid, @NotNull String world,
                                int x, int y, int z) {
        ProtectionChecker checker = hyperFactions.getProtectionChecker();
        ProtectionChecker.ProtectionResult result = checker.canInteract(
            playerUuid, world, x, z, ProtectionChecker.InteractionType.BUILD
        );

        return !checker.isAllowed(result);
    }

    /**
     * Called when a player attempts to break a block.
     *
     * @param playerUuid the player's UUID
     * @param world      the world name
     * @param x          the block X
     * @param y          the block Y
     * @param z          the block Z
     * @return true if block break should be cancelled
     */
    public boolean onBlockBreak(@NotNull UUID playerUuid, @NotNull String world,
                                int x, int y, int z) {
        ProtectionChecker checker = hyperFactions.getProtectionChecker();
        ProtectionChecker.ProtectionResult result = checker.canInteract(
            playerUuid, world, x, z, ProtectionChecker.InteractionType.BUILD
        );

        return !checker.isAllowed(result);
    }

    /**
     * Called when a player attempts to interact with a block (doors, buttons, etc).
     *
     * @param playerUuid the player's UUID
     * @param world      the world name
     * @param x          the block X
     * @param y          the block Y
     * @param z          the block Z
     * @return true if interaction should be cancelled
     */
    public boolean onBlockInteract(@NotNull UUID playerUuid, @NotNull String world,
                                   int x, int y, int z) {
        ProtectionChecker checker = hyperFactions.getProtectionChecker();
        ProtectionChecker.ProtectionResult result = checker.canInteract(
            playerUuid, world, x, z, ProtectionChecker.InteractionType.INTERACT
        );

        return !checker.isAllowed(result);
    }

    /**
     * Called when a player attempts to open a container.
     *
     * @param playerUuid the player's UUID
     * @param world      the world name
     * @param x          the container X
     * @param y          the container Y
     * @param z          the container Z
     * @return true if container access should be cancelled
     */
    public boolean onContainerAccess(@NotNull UUID playerUuid, @NotNull String world,
                                     int x, int y, int z) {
        ProtectionChecker checker = hyperFactions.getProtectionChecker();
        ProtectionChecker.ProtectionResult result = checker.canInteract(
            playerUuid, world, x, z, ProtectionChecker.InteractionType.CONTAINER
        );

        return !checker.isAllowed(result);
    }

    /**
     * Gets the denial message for the last failed protection check.
     *
     * @param result the protection result
     * @return the denial message
     */
    @NotNull
    public String getDenialMessage(@NotNull ProtectionChecker.ProtectionResult result) {
        return hyperFactions.getProtectionChecker().getDenialMessage(result);
    }

    /**
     * Gets the denial message for a PvP result.
     *
     * @param result the PvP result
     * @return the denial message
     */
    @NotNull
    public String getDenialMessage(@NotNull ProtectionChecker.PvPResult result) {
        return hyperFactions.getProtectionChecker().getDenialMessage(result);
    }
}
