package com.hyperfactions.protection.ecs;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.protection.ProtectionListener;
import com.hyperfactions.protection.zone.ZoneInteractionProtection;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.InteractivelyPickupItemEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * ECS system for handling item pickup protection (interactive pickup only).
 * NOTE: This only handles interactive pickup (clicking to pick up items).
 * Automatic pickup (walking near items) is handled internally by the engine and cannot be cancelled.
 * Checks zone flags first, then faction permissions.
 */
public class ItemPickupProtectionSystem extends EntityEventSystem<EntityStore, InteractivelyPickupItemEvent> {

    private final HyperFactions hyperFactions;
    private final ProtectionListener protectionListener;

    public ItemPickupProtectionSystem(@NotNull HyperFactions hyperFactions,
                                       @NotNull ProtectionListener protectionListener) {
        super(InteractivelyPickupItemEvent.class);
        this.hyperFactions = hyperFactions;
        this.protectionListener = protectionListener;
    }

    @Override
    public Query<EntityStore> getQuery() {
        // Must query for PlayerRef to receive events for player entities
        return PlayerRef.getComponentType();
    }

    @Override
    public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk,
                       Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer,
                       InteractivelyPickupItemEvent event) {
        try {
            PlayerRef playerRef = chunk.getComponent(entityIndex, PlayerRef.getComponentType());
            if (playerRef == null) return;

            // Get world name and position from PlayerRef (like Hyfaction does)
            Player playerComponent = store.getComponent(chunk.getReferenceTo(entityIndex), Player.getComponentType());
            if (playerComponent == null) return;

            String worldName = playerComponent.getWorld().getName();
            var position = playerRef.getTransform().getPosition();
            int x = (int) Math.floor(position.getX());
            int y = (int) Math.floor(position.getY());
            int z = (int) Math.floor(position.getZ());

            // 1. First check zone flags (no message to avoid spam)
            ZoneInteractionProtection zoneProtection = hyperFactions.getZoneInteractionProtection();
            boolean zoneAllows = zoneProtection.isItemPickupAllowed(worldName, x, z);

            if (!zoneAllows) {
                event.setCancelled(true);
                return;
            }

            // 2. If zone allows (or not in zone), check faction permissions (no message to avoid spam)
            boolean blocked = protectionListener.onItemPickup(
                playerRef.getUuid(),
                worldName,
                x, y, z
            );

            if (blocked) {
                event.setCancelled(true);
            }
        } catch (Exception e) {
            Logger.severe("Error processing item pickup event", e);
        }
    }
}
