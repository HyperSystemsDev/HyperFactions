package com.hyperfactions.data;

/**
 * Constants for zone flag names.
 * These flags control various behaviors within zones.
 */
public final class ZoneFlags {

    private ZoneFlags() {} // Prevent instantiation

    // === PvP Flags ===
    /** Whether PvP is enabled in this zone. Default: false for SafeZone, true for WarZone */
    public static final String PVP_ENABLED = "pvp_enabled";

    /** Whether friendly fire (same faction) is allowed */
    public static final String FRIENDLY_FIRE = "friendly_fire";

    // === Building Flags ===
    /** Whether players can place/break blocks */
    public static final String BUILD_ALLOWED = "build_allowed";

    /** Whether players can access containers (chests, etc.) */
    public static final String CONTAINER_ACCESS = "container_access";

    /** Whether players can interact with blocks (doors, buttons, etc.) */
    public static final String INTERACT_ALLOWED = "interact_allowed";

    // === Item Flags ===
    /** Whether players can drop items */
    public static final String ITEM_DROP = "item_drop";

    /** Whether players can pick up items */
    public static final String ITEM_PICKUP = "item_pickup";

    // === Mob Flags ===
    /** Whether mobs can spawn */
    public static final String MOB_SPAWNING = "mob_spawning";

    /** Whether mobs can damage players */
    public static final String MOB_DAMAGE = "mob_damage";

    // === Player Effect Flags ===
    /** Whether players lose hunger */
    public static final String HUNGER_LOSS = "hunger_loss";

    /** Whether players take fall damage */
    public static final String FALL_DAMAGE = "fall_damage";

    /**
     * All available flag names for validation.
     */
    public static final String[] ALL_FLAGS = {
        PVP_ENABLED,
        FRIENDLY_FIRE,
        BUILD_ALLOWED,
        CONTAINER_ACCESS,
        INTERACT_ALLOWED,
        ITEM_DROP,
        ITEM_PICKUP,
        MOB_SPAWNING,
        MOB_DAMAGE,
        HUNGER_LOSS,
        FALL_DAMAGE
    };

    /**
     * Checks if a flag name is valid.
     *
     * @param flagName the flag name to check
     * @return true if valid
     */
    public static boolean isValidFlag(String flagName) {
        if (flagName == null) return false;
        for (String flag : ALL_FLAGS) {
            if (flag.equals(flagName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the default value for a flag in SafeZones.
     *
     * @param flagName the flag name
     * @return the default value
     */
    public static boolean getSafeZoneDefault(String flagName) {
        return switch (flagName) {
            case PVP_ENABLED -> false;
            case FRIENDLY_FIRE -> false;
            case BUILD_ALLOWED -> false;
            case CONTAINER_ACCESS -> false;
            case INTERACT_ALLOWED -> true;
            case ITEM_DROP -> true;
            case ITEM_PICKUP -> true;
            case MOB_SPAWNING -> false;
            case MOB_DAMAGE -> false;
            case HUNGER_LOSS -> false;
            case FALL_DAMAGE -> false;
            default -> false;
        };
    }

    /**
     * Gets the default value for a flag in WarZones.
     *
     * WarZones are PvP-enabled areas (like spawn) where building is blocked
     * to prevent griefing, but combat is encouraged.
     *
     * @param flagName the flag name
     * @return the default value
     */
    public static boolean getWarZoneDefault(String flagName) {
        return switch (flagName) {
            case PVP_ENABLED -> true;      // PvP is the main purpose of WarZones
            case FRIENDLY_FIRE -> false;   // Same-faction damage still off
            case BUILD_ALLOWED -> false;   // Block building to prevent griefing
            case CONTAINER_ACCESS -> false; // Block container access
            case INTERACT_ALLOWED -> true; // Allow doors, buttons, etc.
            case ITEM_DROP -> true;
            case ITEM_PICKUP -> true;
            case MOB_SPAWNING -> false;    // No mob spawning in PvP zones
            case MOB_DAMAGE -> true;
            case HUNGER_LOSS -> true;
            case FALL_DAMAGE -> true;
            default -> false;              // Default to protected
        };
    }
}
