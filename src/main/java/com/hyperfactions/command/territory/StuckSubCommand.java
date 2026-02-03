package com.hyperfactions.command.territory;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.manager.TeleportManager;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hyperfactions.util.ChunkUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Subcommand: /f stuck
 * Teleports the player to the nearest safe chunk when stuck in enemy territory.
 */
public class StuckSubCommand extends FactionSubCommand {

    public StuckSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
        super("stuck", "Escape from enemy territory", hyperFactions, plugin);
    }

    @Override
    protected void execute(@NotNull CommandContext ctx,
                          @NotNull Store<EntityStore> store,
                          @NotNull Ref<EntityStore> ref,
                          @NotNull PlayerRef player,
                          @NotNull World currentWorld) {

        if (!hasPermission(player, Permissions.STUCK)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to use /f stuck.", COLOR_RED)));
            return;
        }

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        Vector3d pos = transform.getPosition();
        UUID playerUuid = player.getUuid();

        int chunkX = ChunkUtil.toChunkCoord(pos.getX());
        int chunkZ = ChunkUtil.toChunkCoord(pos.getZ());

        // Check if in enemy/neutral territory
        UUID claimOwner = hyperFactions.getClaimManager().getClaimOwner(currentWorld.getName(), chunkX, chunkZ);
        Faction playerFaction = hyperFactions.getFactionManager().getPlayerFaction(playerUuid);

        if (claimOwner == null) {
            ctx.sendMessage(prefix().insert(msg("You're not stuck - this is wilderness.", COLOR_RED)));
            return;
        }

        if (playerFaction != null && claimOwner.equals(playerFaction.id())) {
            ctx.sendMessage(prefix().insert(msg("You're not stuck - this is your territory.", COLOR_RED)));
            return;
        }

        if (playerFaction != null && playerFaction.isAlly(claimOwner)) {
            ctx.sendMessage(prefix().insert(msg("You're not stuck - this is ally territory.", COLOR_RED)));
            return;
        }

        // Combat check
        if (hyperFactions.getCombatTagManager().isTagged(playerUuid)) {
            ctx.sendMessage(prefix().insert(msg("You cannot use /f stuck while in combat!", COLOR_RED)));
            return;
        }

        // Find nearest safe chunk
        int[] safeChunk = findNearestSafeChunk(currentWorld.getName(), chunkX, chunkZ, playerFaction);
        if (safeChunk == null) {
            ctx.sendMessage(prefix().insert(msg("Could not find a safe location.", COLOR_RED)));
            return;
        }

        // Create teleport location (center of safe chunk)
        double targetX = (safeChunk[0] << 4) + 8;
        double targetZ = (safeChunk[1] << 4) + 8;
        double targetY = pos.getY();

        // Use extended warmup for stuck (30 seconds by default)
        int warmupSeconds = ConfigManager.get().getStuckWarmupSeconds();

        TeleportManager.StartLocation startLoc = new TeleportManager.StartLocation(
            currentWorld.getName(), pos.getX(), pos.getY(), pos.getZ()
        );

        ctx.sendMessage(prefix().insert(msg("Teleporting to safety in " + warmupSeconds + " seconds. Don't move!", COLOR_YELLOW)));

        // Schedule teleport with extended warmup
        final double finalTargetX = targetX;
        final double finalTargetZ = targetZ;
        final double finalTargetY = targetY;
        final World world = currentWorld;

        int taskId = hyperFactions.scheduleDelayedTask(warmupSeconds * 20, () -> {
            // Recheck combat tag
            if (hyperFactions.getCombatTagManager().isTagged(playerUuid)) {
                player.sendMessage(prefix().insert(msg("Teleportation cancelled - you are in combat!", COLOR_RED)));
                return;
            }

            // Recheck if still pending
            TeleportManager.PendingTeleport pending = hyperFactions.getTeleportManager().getPending(playerUuid);
            if (pending == null) {
                return;
            }
            hyperFactions.getTeleportManager().cancelPending(playerUuid, hyperFactions::cancelTask);

            // Execute teleport using proper Teleport component
            Vector3d position = new Vector3d(finalTargetX, finalTargetY, finalTargetZ);
            Vector3f rotation = new Vector3f(0, 0, 0);
            Teleport teleport = new Teleport(world, position, rotation);
            store.addComponent(ref, Teleport.getComponentType(), teleport);

            player.sendMessage(prefix().insert(msg("Teleported to safety!", COLOR_GREEN)));
        });

        // Store as pending teleport
        TeleportManager.PendingTeleport pending = new TeleportManager.PendingTeleport(
            playerUuid, null, startLoc,
            System.currentTimeMillis(), warmupSeconds, taskId, null
        );
        // Note: We're directly tracking this since TeleportManager.teleportToHome doesn't fit this use case
    }

    /**
     * Finds the nearest safe chunk (wilderness, own claim, or ally claim).
     */
    @Nullable
    private int[] findNearestSafeChunk(String world, int startX, int startZ, @Nullable Faction playerFaction) {
        int maxRadius = 10;

        for (int radius = 1; radius <= maxRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) continue;

                    int checkX = startX + dx;
                    int checkZ = startZ + dz;
                    UUID owner = hyperFactions.getClaimManager().getClaimOwner(world, checkX, checkZ);

                    // Safe if: wilderness, own claim, or ally claim
                    if (owner == null) return new int[]{checkX, checkZ};
                    if (playerFaction != null) {
                        if (owner.equals(playerFaction.id())) return new int[]{checkX, checkZ};
                        if (playerFaction.isAlly(owner)) return new int[]{checkX, checkZ};
                    }
                }
            }
        }
        return null;
    }
}
