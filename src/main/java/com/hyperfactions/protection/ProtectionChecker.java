package com.hyperfactions.protection;

import com.hyperfactions.config.HyperFactionsConfig;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.RelationType;
import com.hyperfactions.data.Zone;
import com.hyperfactions.data.ZoneFlags;
import com.hyperfactions.integration.HyperPermsIntegration;
import com.hyperfactions.manager.*;
import com.hyperfactions.util.ChunkUtil;
import com.hyperfactions.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Central class for all protection checks.
 */
public class ProtectionChecker {

    private final FactionManager factionManager;
    private final ClaimManager claimManager;
    private final ZoneManager zoneManager;
    private final RelationManager relationManager;
    private final CombatTagManager combatTagManager;

    public ProtectionChecker(
        @NotNull FactionManager factionManager,
        @NotNull ClaimManager claimManager,
        @NotNull ZoneManager zoneManager,
        @NotNull RelationManager relationManager,
        @NotNull CombatTagManager combatTagManager
    ) {
        this.factionManager = factionManager;
        this.claimManager = claimManager;
        this.zoneManager = zoneManager;
        this.relationManager = relationManager;
        this.combatTagManager = combatTagManager;
    }

    /**
     * Result of a protection check for interactions.
     */
    public enum ProtectionResult {
        ALLOWED,
        ALLOWED_BYPASS,
        ALLOWED_WILDERNESS,
        ALLOWED_OWN_CLAIM,
        ALLOWED_ALLY_CLAIM,
        ALLOWED_WARZONE,
        DENIED_SAFEZONE,
        DENIED_ENEMY_CLAIM,
        DENIED_NEUTRAL_CLAIM,
        DENIED_NO_PERMISSION
    }

    /**
     * Result of a PvP check.
     */
    public enum PvPResult {
        ALLOWED,
        ALLOWED_WARZONE,
        DENIED_SAFEZONE,
        DENIED_SAME_FACTION,
        DENIED_ALLY,
        DENIED_ATTACKER_SAFEZONE,
        DENIED_DEFENDER_SAFEZONE,
        DENIED_SPAWN_PROTECTED
    }

    /**
     * Types of interactions to check.
     */
    public enum InteractionType {
        BUILD,      // Place/break blocks
        INTERACT,   // Use doors, buttons, etc.
        CONTAINER,  // Open chests, etc.
        DAMAGE,     // Damage entities (not players)
        USE         // Use items
    }

    // === Interaction Protection ===

    /**
     * Checks if a player can interact at a location.
     *
     * @param playerUuid the player's UUID
     * @param world      the world name
     * @param x          the world X coordinate
     * @param z          the world Z coordinate
     * @param type       the interaction type
     * @return the protection result
     */
    @NotNull
    public ProtectionResult canInteract(@NotNull UUID playerUuid, @NotNull String world,
                                        double x, double z, @NotNull InteractionType type) {
        int chunkX = ChunkUtil.toChunkCoord(x);
        int chunkZ = ChunkUtil.toChunkCoord(z);
        return canInteractChunk(playerUuid, world, chunkX, chunkZ, type);
    }

    /**
     * Checks if a player can interact in a chunk.
     *
     * @param playerUuid the player's UUID
     * @param world      the world name
     * @param chunkX     the chunk X coordinate
     * @param chunkZ     the chunk Z coordinate
     * @param type       the interaction type
     * @return the protection result
     */
    @NotNull
    public ProtectionResult canInteractChunk(@NotNull UUID playerUuid, @NotNull String world,
                                             int chunkX, int chunkZ, @NotNull InteractionType type) {
        // 1. Check bypass permission
        String bypassPerm = switch (type) {
            case BUILD -> "hyperfactions.bypass.build";
            case INTERACT -> "hyperfactions.bypass.interact";
            case CONTAINER -> "hyperfactions.bypass.container";
            case DAMAGE -> "hyperfactions.bypass.damage";
            case USE -> "hyperfactions.bypass.use";
        };

        if (HyperPermsIntegration.hasPermission(playerUuid, bypassPerm) ||
            HyperPermsIntegration.hasPermission(playerUuid, "hyperfactions.bypass.*")) {
            return ProtectionResult.ALLOWED_BYPASS;
        }

        // 2. Check zone
        Zone zone = zoneManager.getZone(world, chunkX, chunkZ);
        if (zone != null) {
            // Get the appropriate flag for the interaction type
            String flagName = switch (type) {
                case BUILD -> ZoneFlags.BUILD_ALLOWED;
                case INTERACT -> ZoneFlags.INTERACT_ALLOWED;
                case CONTAINER -> ZoneFlags.CONTAINER_ACCESS;
                case DAMAGE -> ZoneFlags.PVP_ENABLED; // For entity damage
                case USE -> ZoneFlags.INTERACT_ALLOWED;
            };

            boolean allowed = zone.getEffectiveFlag(flagName);
            if (!allowed) {
                return zone.isSafeZone() ? ProtectionResult.DENIED_SAFEZONE : ProtectionResult.DENIED_NO_PERMISSION;
            }
            // If zone allows this interaction, still need to check claim ownership below
            // For WarZones with build allowed, anyone can interact
            if (zone.isWarZone() && allowed) {
                return ProtectionResult.ALLOWED_WARZONE;
            }
        }

        // 3. Check claim owner
        UUID claimOwner = claimManager.getClaimOwner(world, chunkX, chunkZ);

        if (claimOwner == null) {
            // Wilderness - anyone can interact
            return ProtectionResult.ALLOWED_WILDERNESS;
        }

        // 4. Get player's faction
        UUID playerFactionId = factionManager.getPlayerFactionId(playerUuid);

        // 5. Check if same faction
        if (playerFactionId != null && playerFactionId.equals(claimOwner)) {
            return ProtectionResult.ALLOWED_OWN_CLAIM;
        }

        // 6. Check ally relation
        if (playerFactionId != null) {
            RelationType relation = relationManager.getRelation(playerFactionId, claimOwner);
            if (relation == RelationType.ALLY) {
                // Allies can interact in each other's territory (configurable per-faction in future)
                return ProtectionResult.ALLOWED_ALLY_CLAIM;
            }
        }

        // 7. Denied - either enemy or neutral claim
        if (playerFactionId != null) {
            RelationType relation = relationManager.getRelation(playerFactionId, claimOwner);
            if (relation == RelationType.ENEMY) {
                Logger.debugProtection("Interaction denied: player=%s, chunk=%s/%d/%d, type=%s, result=ENEMY_CLAIM, claimOwner=%s",
                    playerUuid, world, chunkX, chunkZ, type, claimOwner);
                return ProtectionResult.DENIED_ENEMY_CLAIM;
            }
        }

        Logger.debugProtection("Interaction denied: player=%s, chunk=%s/%d/%d, type=%s, result=NEUTRAL_CLAIM, claimOwner=%s",
            playerUuid, world, chunkX, chunkZ, type, claimOwner);
        return ProtectionResult.DENIED_NEUTRAL_CLAIM;
    }

    // === PvP Protection ===

    /**
     * Checks if a player can damage another player.
     *
     * @param attackerUuid the attacker's UUID
     * @param defenderUuid the defender's UUID
     * @param world        the world name
     * @param x            the location X
     * @param z            the location Z
     * @return the PvP result
     */
    @NotNull
    public PvPResult canDamagePlayer(@NotNull UUID attackerUuid, @NotNull UUID defenderUuid,
                                     @NotNull String world, double x, double z) {
        int chunkX = ChunkUtil.toChunkCoord(x);
        int chunkZ = ChunkUtil.toChunkCoord(z);
        return canDamagePlayerChunk(attackerUuid, defenderUuid, world, chunkX, chunkZ);
    }

    /**
     * Checks if a player can damage another player in a chunk.
     *
     * @param attackerUuid the attacker's UUID
     * @param defenderUuid the defender's UUID
     * @param world        the world name
     * @param chunkX       the chunk X
     * @param chunkZ       the chunk Z
     * @return the PvP result
     */
    @NotNull
    public PvPResult canDamagePlayerChunk(@NotNull UUID attackerUuid, @NotNull UUID defenderUuid,
                                          @NotNull String world, int chunkX, int chunkZ) {
        HyperFactionsConfig config = HyperFactionsConfig.get();

        // 0. Check defender's spawn protection
        if (combatTagManager.hasSpawnProtection(defenderUuid)) {
            return PvPResult.DENIED_SPAWN_PROTECTED;
        }

        // 0b. Break attacker's spawn protection if they attack (if configured)
        if (config.isSpawnProtectionBreakOnAttack() && combatTagManager.hasSpawnProtection(attackerUuid)) {
            combatTagManager.clearSpawnProtection(attackerUuid);
        }

        // 1. Check zone for PvP flag
        Zone zone = zoneManager.getZone(world, chunkX, chunkZ);
        if (zone != null) {
            boolean pvpEnabled = zone.getEffectiveFlag(ZoneFlags.PVP_ENABLED);
            if (!pvpEnabled) {
                return PvPResult.DENIED_SAFEZONE;
            }
            // Zone has PvP enabled - check friendly fire if in zone
            boolean friendlyFireAllowed = zone.getEffectiveFlag(ZoneFlags.FRIENDLY_FIRE);

            // Check same faction
            if (factionManager.areInSameFaction(attackerUuid, defenderUuid)) {
                if (!friendlyFireAllowed && !config.isFactionDamage()) {
                    return PvPResult.DENIED_SAME_FACTION;
                }
            }

            // Check ally
            RelationType relation = relationManager.getPlayerRelation(attackerUuid, defenderUuid);
            if (relation == RelationType.ALLY) {
                if (!friendlyFireAllowed && !config.isAllyDamage()) {
                    return PvPResult.DENIED_ALLY;
                }
            }

            // PvP is enabled in this zone
            return zone.isWarZone() ? PvPResult.ALLOWED_WARZONE : PvPResult.ALLOWED;
        }

        // Not in a zone - use standard checks

        // 2. Check same faction
        if (factionManager.areInSameFaction(attackerUuid, defenderUuid)) {
            if (!config.isFactionDamage()) {
                return PvPResult.DENIED_SAME_FACTION;
            }
        }

        // 3. Check ally
        RelationType relation = relationManager.getPlayerRelation(attackerUuid, defenderUuid);
        if (relation == RelationType.ALLY) {
            if (!config.isAllyDamage()) {
                Logger.debugProtection("PvP denied: attacker=%s, defender=%s, chunk=%s/%d/%d, result=ALLY",
                    attackerUuid, defenderUuid, world, chunkX, chunkZ);
                return PvPResult.DENIED_ALLY;
            }
        }

        // 4. Default: allow PvP
        Logger.debugProtection("PvP allowed: attacker=%s, defender=%s, chunk=%s/%d/%d, relation=%s",
            attackerUuid, defenderUuid, world, chunkX, chunkZ, relation);
        return PvPResult.ALLOWED;
    }

    // === Convenience Methods ===

    /**
     * Checks if a player can build at a location.
     *
     * @param playerUuid the player's UUID
     * @param world      the world name
     * @param x          the X coordinate
     * @param z          the Z coordinate
     * @return true if allowed
     */
    public boolean canBuild(@NotNull UUID playerUuid, @NotNull String world, double x, double z) {
        ProtectionResult result = canInteract(playerUuid, world, x, z, InteractionType.BUILD);
        return isAllowed(result);
    }

    /**
     * Checks if a player can access containers at a location.
     *
     * @param playerUuid the player's UUID
     * @param world      the world name
     * @param x          the X coordinate
     * @param z          the Z coordinate
     * @return true if allowed
     */
    public boolean canAccessContainer(@NotNull UUID playerUuid, @NotNull String world, double x, double z) {
        ProtectionResult result = canInteract(playerUuid, world, x, z, InteractionType.CONTAINER);
        return isAllowed(result);
    }

    /**
     * Checks if a protection result is "allowed".
     *
     * @param result the result
     * @return true if allowed
     */
    public boolean isAllowed(@NotNull ProtectionResult result) {
        return switch (result) {
            case ALLOWED, ALLOWED_BYPASS, ALLOWED_WILDERNESS,
                 ALLOWED_OWN_CLAIM, ALLOWED_ALLY_CLAIM, ALLOWED_WARZONE -> true;
            default -> false;
        };
    }

    /**
     * Checks if a PvP result is "allowed".
     *
     * @param result the result
     * @return true if allowed
     */
    public boolean isAllowed(@NotNull PvPResult result) {
        return switch (result) {
            case ALLOWED, ALLOWED_WARZONE -> true;
            default -> false;
        };
    }

    /**
     * Gets a user-friendly denial message.
     *
     * @param result the protection result
     * @return the denial message
     */
    @NotNull
    public String getDenialMessage(@NotNull ProtectionResult result) {
        return switch (result) {
            case DENIED_SAFEZONE -> "\u00A7cYou cannot do that in a SafeZone.";
            case DENIED_ENEMY_CLAIM -> "\u00A7cYou cannot do that in enemy territory.";
            case DENIED_NEUTRAL_CLAIM -> "\u00A7cYou cannot do that in claimed territory.";
            case DENIED_NO_PERMISSION -> "\u00A7cYou don't have permission to do that.";
            default -> "\u00A7cYou cannot do that here.";
        };
    }

    /**
     * Gets a user-friendly PvP denial message.
     *
     * @param result the PvP result
     * @return the denial message
     */
    @NotNull
    public String getDenialMessage(@NotNull PvPResult result) {
        return switch (result) {
            case DENIED_SAFEZONE -> "\u00A7cPvP is disabled in SafeZones.";
            case DENIED_SAME_FACTION -> "\u00A7cYou cannot attack faction members.";
            case DENIED_ALLY -> "\u00A7cYou cannot attack allies.";
            case DENIED_ATTACKER_SAFEZONE, DENIED_DEFENDER_SAFEZONE -> "\u00A7cPvP is disabled in SafeZones.";
            case DENIED_SPAWN_PROTECTED -> "\u00A7cThat player has spawn protection.";
            default -> "\u00A7cYou cannot attack this player.";
        };
    }

    /**
     * Gets the location description for action bar display.
     *
     * @param world  the world name
     * @param chunkX the chunk X
     * @param chunkZ the chunk Z
     * @param viewerFactionId the viewer's faction ID (nullable)
     * @return the formatted location description
     */
    @NotNull
    public String getLocationDescription(@NotNull String world, int chunkX, int chunkZ,
                                         @Nullable UUID viewerFactionId) {
        // Check zone first
        if (zoneManager.isInSafeZone(world, chunkX, chunkZ)) {
            return "\u00A7a\u00A7lSafeZone";
        }
        if (zoneManager.isInWarZone(world, chunkX, chunkZ)) {
            return "\u00A7c\u00A7lWarZone";
        }

        // Check claim
        UUID claimOwner = claimManager.getClaimOwner(world, chunkX, chunkZ);
        if (claimOwner == null) {
            return "\u00A78Wilderness";
        }

        Faction ownerFaction = factionManager.getFaction(claimOwner);
        if (ownerFaction == null) {
            return "\u00A78Wilderness";
        }

        // Determine relation color
        String color = "\u00A77"; // Neutral default
        if (viewerFactionId != null) {
            if (viewerFactionId.equals(claimOwner)) {
                color = "\u00A7b"; // Cyan for own
            } else {
                RelationType relation = relationManager.getRelation(viewerFactionId, claimOwner);
                color = switch (relation) {
                    case OWN -> "\u00A7b";    // Cyan (shouldn't happen via getRelation, but handle for completeness)
                    case ALLY -> "\u00A7a";   // Green
                    case ENEMY -> "\u00A7c"; // Red
                    case NEUTRAL -> "\u00A77"; // Gray
                };
            }
        }

        return color + ownerFaction.name();
    }
}
