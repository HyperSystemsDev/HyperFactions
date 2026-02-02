package com.hyperfactions.data;

import org.jetbrains.annotations.NotNull;

/**
 * Constants for zone flag names.
 * These flags control various behaviors within zones.
 *
 * Flags are verified against actual Hytale server events:
 * - Combat flags: Use Damage event with different source types
 * - Building flags: Use BreakBlockEvent, PlaceBlockEvent, UseBlockEvent
 * - Item flags: Use DropItemEvent, InteractivelyPickupItemEvent
 *
 * Removed Minecraft-specific flags:
 * - hunger_loss - No hunger system in Hytale
 * - mob_spawning - Not exposed to plugins
 * - container_access + interact_allowed - Consolidated to block_interact
 */
public final class ZoneFlags {

    private ZoneFlags() {} // Prevent instantiation

    // ==========================================================================
    // COMBAT FLAGS (4)
    // ==========================================================================

    /** Whether PvP is enabled. Uses Damage event with EntitySource (player). */
    public static final String PVP_ENABLED = "pvp_enabled";

    /** Whether friendly fire (same faction) is allowed. Uses Damage + faction check. */
    public static final String FRIENDLY_FIRE = "friendly_fire";

    /** Whether projectile damage is allowed. Uses Damage event with ProjectileSource. */
    public static final String PROJECTILE_DAMAGE = "projectile_damage";

    /** Whether mobs can damage players. Uses Damage event with EntitySource (non-player). */
    public static final String MOB_DAMAGE = "mob_damage";

    // ==========================================================================
    // BUILDING FLAGS (2)
    // ==========================================================================

    /** Whether players can place/break blocks. Uses BreakBlockEvent, PlaceBlockEvent. */
    public static final String BUILD_ALLOWED = "build_allowed";

    /** Whether players can interact with blocks (general fallback). Uses UseBlockEvent. */
    public static final String BLOCK_INTERACT = "block_interact";

    // ==========================================================================
    // INTERACTION FLAGS (5) - Specific block interaction types
    // ==========================================================================

    /** Whether players can use doors and gates. Uses DoorInteraction. */
    public static final String DOOR_USE = "door_use";

    /** Whether players can use storage containers (chests, backpacks). Uses OpenContainerInteraction. */
    public static final String CONTAINER_USE = "container_use";

    /** Whether players can use crafting benches/tables. Uses OpenBenchPageInteraction. */
    public static final String BENCH_USE = "bench_use";

    /** Whether players can use processing blocks (furnaces, smelters). Uses OpenProcessingBenchInteraction. */
    public static final String PROCESSING_USE = "processing_use";

    /** Whether players can sit on seats/chairs/mounts. Uses MountInteraction. */
    public static final String SEAT_USE = "seat_use";

    // ==========================================================================
    // ITEM FLAGS (2)
    // ==========================================================================

    /** Whether players can drop items. Uses DropItemEvent. */
    public static final String ITEM_DROP = "item_drop";

    /** Whether players can pick up items. Uses InteractivelyPickupItemEvent. */
    public static final String ITEM_PICKUP = "item_pickup";

    // ==========================================================================
    // DAMAGE FLAGS (2)
    // ==========================================================================

    /** Whether players take fall damage. Uses Damage event with DamageCause.FALL. */
    public static final String FALL_DAMAGE = "fall_damage";

    /** Whether players take environmental damage (drowning, suffocation). Uses Damage with EnvironmentSource. */
    public static final String ENVIRONMENTAL_DAMAGE = "environmental_damage";

    /**
     * All available flag names for validation.
     */
    public static final String[] ALL_FLAGS = {
        // Combat (4)
        PVP_ENABLED,
        FRIENDLY_FIRE,
        PROJECTILE_DAMAGE,
        MOB_DAMAGE,
        // Building (2)
        BUILD_ALLOWED,
        BLOCK_INTERACT,
        // Interaction (5)
        DOOR_USE,
        CONTAINER_USE,
        BENCH_USE,
        PROCESSING_USE,
        SEAT_USE,
        // Items (2)
        ITEM_DROP,
        ITEM_PICKUP,
        // Damage (2)
        FALL_DAMAGE,
        ENVIRONMENTAL_DAMAGE
    };

    /**
     * Flag categories for UI organization.
     */
    public static final String[] COMBAT_FLAGS = { PVP_ENABLED, FRIENDLY_FIRE, PROJECTILE_DAMAGE, MOB_DAMAGE };
    public static final String[] BUILDING_FLAGS = { BUILD_ALLOWED, BLOCK_INTERACT };
    public static final String[] INTERACTION_FLAGS = { DOOR_USE, CONTAINER_USE, BENCH_USE, PROCESSING_USE, SEAT_USE };
    public static final String[] ITEM_FLAGS = { ITEM_DROP, ITEM_PICKUP };
    public static final String[] DAMAGE_FLAGS = { FALL_DAMAGE, ENVIRONMENTAL_DAMAGE };

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
     * SafeZones are protected areas where combat and building are disabled.
     *
     * @param flagName the flag name
     * @return the default value
     */
    public static boolean getSafeZoneDefault(String flagName) {
        return switch (flagName) {
            // Combat: All disabled in SafeZones
            case PVP_ENABLED -> false;
            case FRIENDLY_FIRE -> false;
            case PROJECTILE_DAMAGE -> false;
            case MOB_DAMAGE -> false;
            // Building: Disabled, but basic interaction allowed
            case BUILD_ALLOWED -> false;
            case BLOCK_INTERACT -> true;
            // Interaction: Doors/seats allowed, but no containers/benches/processing
            case DOOR_USE -> true;
            case CONTAINER_USE -> false;
            case BENCH_USE -> false;
            case PROCESSING_USE -> false;
            case SEAT_USE -> true;
            // Items: Pickup allowed, but no dropping (prevents littering spawn)
            case ITEM_DROP -> false;
            case ITEM_PICKUP -> true;
            // Damage: No environmental damage in safe zones
            case FALL_DAMAGE -> false;
            case ENVIRONMENTAL_DAMAGE -> false;
            default -> false;
        };
    }

    /**
     * Gets the default value for a flag in WarZones.
     * WarZones are PvP-enabled areas where building is blocked to prevent griefing.
     *
     * @param flagName the flag name
     * @return the default value
     */
    public static boolean getWarZoneDefault(String flagName) {
        return switch (flagName) {
            // Combat: PvP and mob damage enabled
            case PVP_ENABLED -> true;
            case FRIENDLY_FIRE -> false;      // Faction members still protected
            case PROJECTILE_DAMAGE -> true;
            case MOB_DAMAGE -> true;
            // Building: Disabled to prevent griefing, but basic interaction allowed
            case BUILD_ALLOWED -> false;
            case BLOCK_INTERACT -> true;
            // Interaction: Doors/seats allowed, but no containers/benches/processing
            case DOOR_USE -> true;
            case CONTAINER_USE -> false;
            case BENCH_USE -> false;
            case PROCESSING_USE -> false;
            case SEAT_USE -> true;
            // Items: Allowed
            case ITEM_DROP -> true;
            case ITEM_PICKUP -> true;
            // Damage: All environmental damage enabled
            case FALL_DAMAGE -> true;
            case ENVIRONMENTAL_DAMAGE -> true;
            default -> false;
        };
    }

    /**
     * Gets the default value for a flag based on zone type.
     *
     * @param flagName the flag name
     * @param type     the zone type
     * @return the default value
     */
    public static boolean getDefault(String flagName, @NotNull ZoneType type) {
        return type == ZoneType.SAFE ? getSafeZoneDefault(flagName) : getWarZoneDefault(flagName);
    }

    /**
     * Gets a human-readable display name for a flag.
     *
     * @param flagName the flag name
     * @return the display name
     */
    @NotNull
    public static String getDisplayName(String flagName) {
        return switch (flagName) {
            case PVP_ENABLED -> "PvP Enabled";
            case FRIENDLY_FIRE -> "Friendly Fire";
            case PROJECTILE_DAMAGE -> "Projectile Damage";
            case MOB_DAMAGE -> "Mob Damage";
            case BUILD_ALLOWED -> "Building Allowed";
            case BLOCK_INTERACT -> "Block Interaction";
            case DOOR_USE -> "Door Use";
            case CONTAINER_USE -> "Container Use";
            case BENCH_USE -> "Bench Use";
            case PROCESSING_USE -> "Processing Use";
            case SEAT_USE -> "Seat Use";
            case ITEM_DROP -> "Item Drop";
            case ITEM_PICKUP -> "Item Pickup";
            case FALL_DAMAGE -> "Fall Damage";
            case ENVIRONMENTAL_DAMAGE -> "Environmental Damage";
            default -> flagName;
        };
    }

    /**
     * Gets a short description for a flag.
     *
     * @param flagName the flag name
     * @return the description
     */
    @NotNull
    public static String getDescription(String flagName) {
        return switch (flagName) {
            case PVP_ENABLED -> "Players can damage other players";
            case FRIENDLY_FIRE -> "Same-faction players can damage each other";
            case PROJECTILE_DAMAGE -> "Projectiles deal damage";
            case MOB_DAMAGE -> "Mobs can damage players";
            case BUILD_ALLOWED -> "Players can place and break blocks";
            case BLOCK_INTERACT -> "General block interaction (fallback)";
            case DOOR_USE -> "Players can use doors and gates";
            case CONTAINER_USE -> "Players can use chests and storage";
            case BENCH_USE -> "Players can use crafting tables";
            case PROCESSING_USE -> "Players can use furnaces and smelters";
            case SEAT_USE -> "Players can sit on seats and mounts";
            case ITEM_DROP -> "Players can drop items";
            case ITEM_PICKUP -> "Players can pick up items";
            case FALL_DAMAGE -> "Fall damage applies";
            case ENVIRONMENTAL_DAMAGE -> "Drowning, suffocation, etc.";
            default -> "Unknown flag";
        };
    }
}
