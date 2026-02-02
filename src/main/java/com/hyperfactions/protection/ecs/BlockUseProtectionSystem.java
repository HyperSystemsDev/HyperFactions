package com.hyperfactions.protection.ecs;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.protection.ProtectionChecker;
import com.hyperfactions.protection.ProtectionListener;
import com.hyperfactions.protection.zone.ZoneInteractionProtection;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * ECS system for handling block use/interact protection.
 * Checks zone flags first, then faction permissions.
 */
public class BlockUseProtectionSystem extends EntityEventSystem<EntityStore, UseBlockEvent.Pre> {

    private final HyperFactions hyperFactions;
    private final ProtectionListener protectionListener;

    public BlockUseProtectionSystem(@NotNull HyperFactions hyperFactions,
                                     @NotNull ProtectionListener protectionListener) {
        super(UseBlockEvent.Pre.class);
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
                       UseBlockEvent.Pre event) {
        try {
            PlayerRef player = chunk.getComponent(entityIndex, PlayerRef.getComponentType());
            if (player == null) return;

            Vector3i pos = event.getTargetBlock();
            String worldName = getWorldName(store);
            if (worldName == null) return;

            // Get block state ID for zone flag checking (uses Hytale's state system)
            String stateId = getBlockStateId(event.getBlockType());

            // 1. First check zone flags (specific interaction type based on block state)
            ZoneInteractionProtection zoneProtection = hyperFactions.getZoneInteractionProtection();
            boolean zoneAllows = zoneProtection.isBlockInteractionAllowed(stateId, worldName, pos.getX(), pos.getZ());

            if (!zoneAllows) {
                event.setCancelled(true);
                ZoneInteractionProtection.InteractionBlockType detectedType =
                    zoneProtection.detectBlockTypeFromState(stateId != null ? stateId : "");
                String flagName = switch (detectedType) {
                    case DOOR -> "door use";
                    case CONTAINER -> "container use";
                    case BENCH -> "bench use";
                    case PROCESSING -> "processing use";
                    case SEAT -> "seat use";
                    case OTHER -> "block interaction";
                };
                player.sendMessage(Message.raw("You cannot use " + flagName + " in this zone.").color("#FF5555"));
                return;
            }

            // 2. If zone allows (or not in zone), check faction permissions
            boolean isDoor = isDoorState(stateId);
            boolean blocked;
            ProtectionChecker.InteractionType interactionType;

            if (isDoor) {
                interactionType = ProtectionChecker.InteractionType.INTERACT;
                blocked = protectionListener.onBlockInteract(
                    player.getUuid(),
                    worldName,
                    pos.getX(), pos.getY(), pos.getZ()
                );
            } else {
                interactionType = ProtectionChecker.InteractionType.CONTAINER;
                blocked = protectionListener.onContainerAccess(
                    player.getUuid(),
                    worldName,
                    pos.getX(), pos.getY(), pos.getZ()
                );
            }

            if (blocked) {
                event.setCancelled(true);
                player.sendMessage(Message.raw(protectionListener.getDenialMessage(
                    hyperFactions.getProtectionChecker().canInteract(
                        player.getUuid(), worldName, pos.getX(), pos.getZ(),
                        interactionType
                    )
                )).color("#FF5555"));
            }
        } catch (Exception e) {
            Logger.severe("Error processing block use event", e);
        }
    }

    /**
     * Gets the state ID from a block type (e.g., "container", "Door", "processingBench").
     * Uses Hytale's native state system which works for both vanilla and modded blocks.
     */
    private String getBlockStateId(BlockType blockType) {
        if (blockType == null) return null;
        try {
            var state = blockType.getState();
            if (state != null) {
                return state.getId();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Checks if a state ID indicates a door/gate block.
     */
    private boolean isDoorState(String stateId) {
        if (stateId == null) return false;
        return "Door".equalsIgnoreCase(stateId);
    }

    private String getWorldName(Store<EntityStore> store) {
        try {
            return store.getExternalData().getWorld().getName();
        } catch (Exception e) {
            return null;
        }
    }
}
