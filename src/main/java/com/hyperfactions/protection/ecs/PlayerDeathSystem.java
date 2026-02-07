package com.hyperfactions.protection.ecs;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.RelationType;
import com.hyperfactions.manager.PowerManager;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * ECS system that detects player death via DeathComponent addition.
 * Applies power penalty when a player dies.
 *
 * <p>This follows Hytale's built-in pattern from {@code DeathSystems.OnDeathSystem}:
 * when a player dies, a {@link DeathComponent} is added to their entity.
 * This system listens for that addition and applies faction power loss.</p>
 *
 * @see DeathComponent
 * @see com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems
 */
public class PlayerDeathSystem extends RefChangeSystem<EntityStore, DeathComponent> {

    private final HyperFactions hyperFactions;

    /**
     * Creates a new PlayerDeathSystem.
     *
     * @param hyperFactions the HyperFactions instance
     */
    public PlayerDeathSystem(@NotNull HyperFactions hyperFactions) {
        this.hyperFactions = hyperFactions;
    }

    @NotNull
    @Override
    public ComponentType<EntityStore, DeathComponent> componentType() {
        return DeathComponent.getComponentType();
    }

    @NotNull
    @Override
    public Query<EntityStore> getQuery() {
        // Only process player entities
        return Player.getComponentType();
    }

    @Override
    public void onComponentAdded(@NotNull Ref<EntityStore> ref,
                                  @NotNull DeathComponent component,
                                  @NotNull Store<EntityStore> store,
                                  @NotNull CommandBuffer<EntityStore> commandBuffer) {
        try {
            // Get the player reference from the entity
            PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef == null) {
                return;
            }

            UUID victimUuid = playerRef.getUuid();

            // Apply death power penalty
            double newPower = hyperFactions.getPowerManager().applyDeathPenalty(victimUuid);
            Logger.debugPower("Player %s died, power now %.2f", victimUuid, newPower);

            // Kill reward / neutral kill penalty
            UUID killerUuid = hyperFactions.getCombatTagManager().getLastAttacker(victimUuid);
            if (killerUuid != null) {
                ConfigManager config = ConfigManager.get();
                PowerManager pm = hyperFactions.getPowerManager();

                // Kill reward (all PvP kills)
                double reward = config.getKillReward();
                if (reward > 0) {
                    double killerPower = pm.applyKillReward(killerUuid, reward);
                    Logger.debugPower("Kill reward: killer=%s gained %.2f power (now %.2f)", killerUuid, reward, killerPower);
                }

                // Neutral kill penalty
                double neutralPenalty = config.getNeutralAttackPenalty();
                if (neutralPenalty > 0) {
                    RelationType relation = hyperFactions.getRelationManager()
                        .getPlayerRelation(killerUuid, victimUuid);
                    if (relation == null || relation == RelationType.NEUTRAL) {
                        double killerPower = pm.applyNeutralKillPenalty(killerUuid, neutralPenalty);
                        Logger.debugPower("Neutral kill penalty: killer=%s lost %.2f power (now %.2f)", killerUuid, neutralPenalty, killerPower);
                    }
                }
            }
        } catch (Exception e) {
            Logger.severe("Error handling player death in ECS system", e);
        }
    }

    @Override
    public void onComponentSet(@NotNull Ref<EntityStore> ref,
                               @Nullable DeathComponent oldComponent,
                               @NotNull DeathComponent newComponent,
                               @NotNull Store<EntityStore> store,
                               @NotNull CommandBuffer<EntityStore> commandBuffer) {
        // Not needed - death component is added once, not updated
    }

    @Override
    public void onComponentRemoved(@NotNull Ref<EntityStore> ref,
                                    @NotNull DeathComponent component,
                                    @NotNull Store<EntityStore> store,
                                    @NotNull CommandBuffer<EntityStore> commandBuffer) {
        // Respawn handling is done by PlayerRespawnSystem
    }
}
