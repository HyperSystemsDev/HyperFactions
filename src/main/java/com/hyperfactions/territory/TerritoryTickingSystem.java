package com.hyperfactions.territory;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.config.HyperFactionsConfig;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * ECS ticking system that provides player position updates to the territory
 * notifier. This system ticks every game tick for all player entities,
 * providing reliable position data regardless of how the player moves.
 *
 * This replaces the previous approach which used PlayerMouseMotionEvent
 * (only fires when looking around) and a scheduled task (position data may
 * be stale). The ECS approach is more reliable as it operates directly on
 * the entity component data each tick.
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
        // Skip if territory notifications are disabled
        if (!HyperFactionsConfig.get().isTerritoryNotificationsEnabled()) {
            return;
        }

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
            double posZ = playerRef.getTransform().getPosition().getZ();

            // Pass position to TerritoryNotifier - it handles chunk change detection
            hyperFactions.getTerritoryNotifier().onPlayerMove(playerRef, worldName, posX, posZ);

        } catch (Exception e) {
            // Silently ignore - this ticks frequently and errors shouldn't spam logs
            Logger.debugTerritory("Error in territory tick: %s", e.getMessage());
        }
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
