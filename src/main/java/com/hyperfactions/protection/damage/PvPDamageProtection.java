package com.hyperfactions.protection.damage;

import com.hyperfactions.protection.ProtectionChecker;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Function;

/**
 * Handles PvP damage protection based on factions, zones, and relations.
 * Uses ProtectionChecker for complex faction-based protection rules.
 */
public class PvPDamageProtection {

    private final ProtectionChecker protectionChecker;
    private final Function<ProtectionChecker.PvPResult, String> denialMessageProvider;

    public PvPDamageProtection(@NotNull ProtectionChecker protectionChecker,
                               @NotNull Function<ProtectionChecker.PvPResult, String> denialMessageProvider) {
        this.protectionChecker = protectionChecker;
        this.denialMessageProvider = denialMessageProvider;
    }

    /**
     * Checks if this is PvP damage (player vs player).
     *
     * @param entitySource  the entity source
     * @param defenderUuid  the defender's UUID
     * @param commandBuffer the command buffer
     * @return the attacker PlayerRef if this is PvP, null otherwise
     */
    @Nullable
    public PlayerRef getPvPAttacker(@NotNull Damage.EntitySource entitySource,
                                    @NotNull UUID defenderUuid,
                                    @NotNull CommandBuffer<EntityStore> commandBuffer) {
        PlayerRef attacker = commandBuffer.getComponent(entitySource.getRef(), PlayerRef.getComponentType());
        if (attacker == null) {
            return null; // Not a player attacker
        }

        // Skip self-damage
        if (attacker.getUuid().equals(defenderUuid)) {
            return null;
        }

        return attacker;
    }

    /**
     * Handles PvP damage protection.
     *
     * @param event         the damage event
     * @param entitySource  the entity source
     * @param defender      the defender PlayerRef
     * @param worldName     the world name
     * @param x             the X coordinate
     * @param z             the Z coordinate
     * @param commandBuffer the command buffer
     * @return true if the damage was handled (blocked or allowed)
     */
    public boolean handle(@NotNull Damage event,
                          @NotNull Damage.EntitySource entitySource,
                          @NotNull PlayerRef defender,
                          @NotNull String worldName,
                          double x, double z,
                          @NotNull CommandBuffer<EntityStore> commandBuffer) {

        UUID defenderUuid = defender.getUuid();
        PlayerRef attacker = getPvPAttacker(entitySource, defenderUuid, commandBuffer);

        if (attacker == null) {
            return false; // Not PvP, continue processing
        }

        UUID attackerUuid = attacker.getUuid();

        ProtectionChecker.PvPResult result = protectionChecker.canDamagePlayer(
            attackerUuid, defenderUuid, worldName, x, z
        );

        if (!protectionChecker.isAllowed(result)) {
            event.setCancelled(true);
            String message = denialMessageProvider.apply(result);
            attacker.sendMessage(Message.raw(message).color("#FF5555"));
            return true;
        }

        return true; // Handled
    }
}
