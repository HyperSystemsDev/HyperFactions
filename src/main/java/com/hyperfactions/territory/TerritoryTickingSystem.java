package com.hyperfactions.territory;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.manager.TeleportManager;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * ECS ticking system that provides player position updates to the territory
 * notifier and executes pending teleports.
 *
 * This system ticks every game tick for all player entities, providing reliable
 * position data regardless of how the player moves. It also handles executing
 * pending teleports when their warmup completes, ensuring they run on the
 * correct world thread.
 *
 * Note: Chunk change detection is handled by TerritoryNotifier, not here.
 */
public class TerritoryTickingSystem extends EntityTickingSystem<EntityStore> {

    private final HyperFactions hyperFactions;

    /**
     * Creates a new territory ticking system.
     *
     * @param hyperFactions the HyperFactions instance
     */
    public TerritoryTickingSystem(@NotNull HyperFactions hyperFactions) {
        this.hyperFactions = hyperFactions;
    }

    @Override
    public void tick(float dt, int index, @NotNull ArchetypeChunk<EntityStore> archetypeChunk,
                     @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer) {
        try {
            // Get entity reference
            Ref ref = archetypeChunk.getReferenceTo(index);

            // Get PlayerRef and Player components
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            Player player = store.getComponent(ref, Player.getComponentType());

            if (playerRef == null || player == null) {
                return;
            }

            // Get world name
            String worldName = player.getWorld().getName();
            if (worldName == null) {
                return;
            }

            // Get current position (as double for precision)
            double posX = playerRef.getTransform().getPosition().getX();
            double posY = playerRef.getTransform().getPosition().getY();
            double posZ = playerRef.getTransform().getPosition().getZ();

            UUID playerUuid = playerRef.getUuid();
            TeleportManager teleportManager = hyperFactions.getTeleportManager();

            // Check for pending teleport
            if (teleportManager.hasPending(playerUuid)) {
                // Check for movement cancellation first
                boolean cancelled = teleportManager.checkMovement(
                    playerUuid,
                    posX, posY, posZ,
                    playerRef::sendMessage
                );

                // If not cancelled by movement, check if ready to execute
                if (!cancelled) {
                    // Send countdown message (will only announce at certain intervals)
                    TeleportManager.PendingTeleport pending = teleportManager.getPending(playerUuid);
                    if (pending != null) {
                        teleportManager.sendCountdownMessage(pending, playerRef::sendMessage);
                    }

                    TeleportManager.PendingTeleport ready = teleportManager.checkReady(
                        playerUuid, playerRef::sendMessage
                    );

                    if (ready != null) {
                        // Execute the teleport on the world thread (we're on it!)
                        executeTeleport(store, ref, player.getWorld(), ready, playerRef);
                    }
                }
            }

            // Pass position to TerritoryNotifier if notifications enabled
            if (ConfigManager.get().isTerritoryNotificationsEnabled()) {
                hyperFactions.getTerritoryNotifier().onPlayerMove(playerRef, worldName, posX, posZ);
            }

        } catch (Exception e) {
            // Silently ignore - this ticks frequently and errors shouldn't spam logs
            Logger.debugTerritory("Error in territory tick: %s", e.getMessage());
        }
    }

    /**
     * Executes a pending teleport.
     * Uses targetWorld.execute() to ensure teleport runs on the correct world thread.
     */
    private void executeTeleport(Store<EntityStore> store, Ref<EntityStore> ref,
                                  World currentWorld, TeleportManager.PendingTeleport pending,
                                  PlayerRef playerRef) {
        TeleportManager.TeleportDestination dest = pending.destination();

        // Get target world (supports cross-world teleportation)
        World targetWorld;
        if (currentWorld.getName().equals(dest.world())) {
            targetWorld = currentWorld;
        } else {
            targetWorld = Universe.get().getWorld(dest.world());
            if (targetWorld == null) {
                hyperFactions.getTeleportManager().onTeleportFailed(
                    TeleportManager.TeleportResult.WORLD_NOT_FOUND,
                    playerRef::sendMessage
                );
                return;
            }
        }

        // Execute teleport on the target world's thread using createForPlayer for proper player teleportation
        targetWorld.execute(() -> {
            Vector3d position = new Vector3d(dest.x(), dest.y(), dest.z());
            Vector3f rotation = new Vector3f(dest.pitch(), dest.yaw(), 0);
            Teleport teleport = Teleport.createForPlayer(targetWorld, position, rotation);
            store.addComponent(ref, Teleport.getComponentType(), teleport);
        });

        // Success - apply cooldown and send message
        hyperFactions.getTeleportManager().onTeleportSuccess(
            pending.playerUuid(),
            playerRef::sendMessage
        );

        Logger.debug("Executed teleport for %s to %s (%.1f, %.1f, %.1f)",
            pending.playerUuid(), dest.world(), dest.x(), dest.y(), dest.z());
    }

    /**
     * Clears all tracking data.
     * Called on plugin shutdown.
     */
    public void shutdown() {
        // No local state to clear - TerritoryNotifier handles player tracking
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        // Query for entities with PlayerRef component (players)
        return PlayerRef.getComponentType();
    }
}
