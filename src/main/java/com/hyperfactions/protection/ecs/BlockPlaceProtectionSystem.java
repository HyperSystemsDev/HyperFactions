package com.hyperfactions.protection.ecs;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.protection.ProtectionChecker;
import com.hyperfactions.protection.ProtectionListener;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * ECS system for handling block place protection.
 * Checks both zone flags and faction permissions.
 */
public class BlockPlaceProtectionSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {

    private final HyperFactions hyperFactions;
    private final ProtectionListener protectionListener;

    public BlockPlaceProtectionSystem(@NotNull HyperFactions hyperFactions,
                                       @NotNull ProtectionListener protectionListener) {
        super(PlaceBlockEvent.class);
        this.hyperFactions = hyperFactions;
        this.protectionListener = protectionListener;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }

    @Override
    public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk,
                       Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer,
                       PlaceBlockEvent event) {
        try {
            PlayerRef player = chunk.getComponent(entityIndex, PlayerRef.getComponentType());
            if (player == null) return;

            Vector3i pos = event.getTargetBlock();
            // Get world name from store's external data (EntityStore)
            String worldName = getWorldName(store);
            if (worldName == null) return;

            boolean blocked = protectionListener.onBlockPlace(
                player.getUuid(),
                worldName,
                pos.getX(), pos.getY(), pos.getZ()
            );

            if (blocked) {
                event.setCancelled(true);
                player.sendMessage(Message.raw(protectionListener.getDenialMessage(
                    hyperFactions.getProtectionChecker().canInteract(
                        player.getUuid(), worldName, pos.getX(), pos.getZ(),
                        ProtectionChecker.InteractionType.BUILD
                    )
                )).color("#FF5555"));
            }
        } catch (Exception e) {
            Logger.severe("Error processing block place event", e);
        }
    }

    private String getWorldName(Store<EntityStore> store) {
        try {
            return store.getExternalData().getWorld().getName();
        } catch (Exception e) {
            return null;
        }
    }
}
