package com.hyperfactions.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Constants for zone flag names.
 * These flags control various behaviors within zones.
 *
 * Flags are verified against actual Hytale server events:
 * - Combat flags: Use Damage event with different source types
 * - Building flags: Use BreakBlockEvent, PlaceBlockEvent, UseBlockEvent
 * - Item flags: Use DropItemEvent, InteractivelyPickupItemEvent
 *
 * MIXIN-DEPENDENT FLAGS
 * Some flags require OrbisGuard-Mixins to function properly. When OrbisGuard-Mixins
 * is not installed, these flags will have no effect. Use requiresMixin(String)
 * to check if a flag requires mixin support, and getMixinType(String) to
 * determine which specific mixin is required.
 *
 * Mixin Types:
 * - MIXIN_PICKUP: Required for F-key item pickup blocking
 * - MIXIN_DEATH: Required for keep inventory on death
 * - MIXIN_DURABILITY: Required for invincible items (no durability loss)
 * - MIXIN_SEATING: Required for enhanced seat/mount blocking
 *
 * Removed Minecraft-specific flags:
 * - hunger_loss: No hunger system in Hytale
 * - container_access + interact_allowed: Consolidated to block_interact
 */
public final class ZoneFlags {

    private ZoneFlags() {} // Prevent instantiation

    // ==========================================================================
    // MIXIN TYPE CONSTANTS
    // ==========================================================================

    /** Mixin type for F-key item pickup interception. */
    public static final String MIXIN_PICKUP = "pickup";

    /** Mixin type for death event interception (keep inventory). */
    public static final String MIXIN_DEATH = "death";

    /** Mixin type for durability/damage interception (invincible items). */
    public static final String MIXIN_DURABILITY = "durability";

    /** Mixin type for seating/mounting interception. */
    public static final String MIXIN_SEATING = "seating";

    /** Mixin type for NPC spawn interception. */
    public static final String MIXIN_SPAWN = "spawn";

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
    // ITEM FLAGS (4)
    // ==========================================================================

    /** Whether players can drop items. Uses DropItemEvent. */
    public static final String ITEM_DROP = "item_drop";

    /** Whether players can pick up items automatically (walking over them). Uses InteractivelyPickupItemEvent. */
    public static final String ITEM_PICKUP = "item_pickup";

    /**
     * Whether players can manually pick up items (F-key).
     * REQUIRES: OrbisGuard-Mixins (pickup mixin)
     * Uses OrbisGuard-Mixins pickup hook.
     */
    public static final String ITEM_PICKUP_MANUAL = "item_pickup_manual";

    /**
     * Whether items are invincible (no durability loss).
     * REQUIRES: OrbisGuard-Mixins (durability mixin)
     * Uses OrbisGuard-Mixins durability hook.
     */
    public static final String INVINCIBLE_ITEMS = "invincible_items";

    // ==========================================================================
    // DAMAGE FLAGS (2)
    // ==========================================================================

    /** Whether players take fall damage. Uses Damage event with DamageCause.FALL. */
    public static final String FALL_DAMAGE = "fall_damage";

    /** Whether players take environmental damage (drowning, suffocation). Uses Damage with EnvironmentSource. */
    public static final String ENVIRONMENTAL_DAMAGE = "environmental_damage";

    // ==========================================================================
    // DEATH FLAGS (1)
    // ==========================================================================

    /**
     * Whether players keep their inventory on death.
     * REQUIRES: OrbisGuard-Mixins (death mixin)
     * Uses OrbisGuard-Mixins death hook to prevent item drops.
     */
    public static final String KEEP_INVENTORY = "keep_inventory";

    // ==========================================================================
    // MOB SPAWNING FLAGS (4) - Uses SpawnSuppressionController
    // ==========================================================================

    /**
     * Master toggle for mob spawning. When false, blocks ALL mob spawning.
     * When true, spawning is controlled by the specific group flags below.
     * Uses Hytale's SpawnSuppressionController for chunk-based suppression.
     */
    public static final String MOB_SPAWNING = "mob_spawning";

    /**
     * Whether hostile mobs can spawn. Only applies when MOB_SPAWNING is true.
     * Uses NPCGroup "hostile" to determine which mobs are hostile.
     */
    public static final String HOSTILE_MOB_SPAWNING = "hostile_mob_spawning";

    /**
     * Whether passive mobs can spawn. Only applies when MOB_SPAWNING is true.
     * Uses NPCGroup "passive" to determine which mobs are passive.
     */
    public static final String PASSIVE_MOB_SPAWNING = "passive_mob_spawning";

    /**
     * Whether neutral mobs can spawn. Only applies when MOB_SPAWNING is true.
     * Uses NPCGroup "neutral" to determine which mobs are neutral (conditionally aggressive).
     */
    public static final String NEUTRAL_MOB_SPAWNING = "neutral_mob_spawning";

    /**
     * Whether NPC spawning is allowed (via mixin hook).
     * REQUIRES: OrbisGuard-Mixins (spawn hook via WorldSpawnJobSystemsMixin)
     * This is separate from the native hostile/passive/neutral flags.
     * Uses OrbisGuard-Mixins spawn hook to intercept world spawn jobs.
     */
    public static final String NPC_SPAWNING = "npc_spawning";

    // ==========================================================================
    // INTEGRATION FLAGS (1)
    // ==========================================================================

    /** Whether non-owners can access (collect/break) other players' gravestones in this zone. Owners can always access their own. */
    public static final String GRAVESTONE_ACCESS = "gravestone_access";

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
        // Items (4)
        ITEM_DROP,
        ITEM_PICKUP,
        ITEM_PICKUP_MANUAL,
        INVINCIBLE_ITEMS,
        // Damage (2)
        FALL_DAMAGE,
        ENVIRONMENTAL_DAMAGE,
        // Death (1)
        KEEP_INVENTORY,
        // Mob Spawning (5)
        MOB_SPAWNING,
        HOSTILE_MOB_SPAWNING,
        PASSIVE_MOB_SPAWNING,
        NEUTRAL_MOB_SPAWNING,
        NPC_SPAWNING,
        // Integration (1)
        GRAVESTONE_ACCESS
    };

    /**
     * Flag categories for UI organization.
     * Note: BLOCK_INTERACT is the parent of INTERACTION_FLAGS, MOB_SPAWNING is the parent of its children.
     */
    public static final String[] COMBAT_FLAGS = { PVP_ENABLED, FRIENDLY_FIRE, PROJECTILE_DAMAGE, MOB_DAMAGE };
    public static final String[] BUILDING_FLAGS = { BUILD_ALLOWED };
    public static final String[] DAMAGE_FLAGS = { FALL_DAMAGE, ENVIRONMENTAL_DAMAGE };
    public static final String[] DEATH_FLAGS = { KEEP_INVENTORY };
    public static final String[] SPAWNING_FLAGS = { MOB_SPAWNING, HOSTILE_MOB_SPAWNING, PASSIVE_MOB_SPAWNING, NEUTRAL_MOB_SPAWNING, NPC_SPAWNING };
    public static final String[] INTERACTION_FLAGS = { BLOCK_INTERACT, DOOR_USE, CONTAINER_USE, BENCH_USE, PROCESSING_USE, SEAT_USE };
    public static final String[] ITEM_FLAGS = { ITEM_DROP, ITEM_PICKUP, ITEM_PICKUP_MANUAL, INVINCIBLE_ITEMS };
    public static final String[] INTEGRATION_FLAGS = { GRAVESTONE_ACCESS };

    /**
     * Flags that require OrbisGuard-Mixins to function.
     * When mixins are not installed, these flags will have no effect even if enabled.
     */
    public static final String[] MIXIN_DEPENDENT_FLAGS = {
        ITEM_PICKUP_MANUAL,  // Requires pickup mixin
        INVINCIBLE_ITEMS,    // Requires durability mixin
        KEEP_INVENTORY,      // Requires death mixin
        NPC_SPAWNING         // Requires spawn mixin
    };

    /**
     * Set of mixin-dependent flags for fast lookup.
     */
    private static final Set<String> MIXIN_FLAG_SET = Set.of(MIXIN_DEPENDENT_FLAGS);

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
     * SafeZone philosophy:
     * - No combat, no building, no damage
     * - Allow basic traversal (doors, seats)
     * - Auto item pickup allowed, manual F-key pickup blocked
     * - Keep inventory on death (if mixins available)
     * - Invincible items (if mixins available)
     * - No mob spawning
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
            // Items: Auto pickup allowed, manual F-key blocked, items are invincible
            case ITEM_DROP -> false;
            case ITEM_PICKUP -> true;             // Auto pickup allowed
            case ITEM_PICKUP_MANUAL -> false;     // F-key pickup blocked (mixin)
            case INVINCIBLE_ITEMS -> true;        // No durability loss (mixin)
            // Damage: No environmental damage in safe zones
            case FALL_DAMAGE -> false;
            case ENVIRONMENTAL_DAMAGE -> false;
            // Death: Keep inventory (mixin)
            case KEEP_INVENTORY -> true;
            // Mob Spawning: Entirely disabled in safe zones
            case MOB_SPAWNING -> false;
            case HOSTILE_MOB_SPAWNING -> false;
            case PASSIVE_MOB_SPAWNING -> false;
            case NEUTRAL_MOB_SPAWNING -> false;
            case NPC_SPAWNING -> false;           // Mixin spawn hook blocked
            // Integration: Protected in safe zones
            case GRAVESTONE_ACCESS -> false;
            default -> false;
        };
    }

    /**
     * Gets the default value for a flag in WarZones.
     * WarZones are PvP-enabled areas where building is blocked to prevent griefing.
     *
     * WarZone philosophy:
     * - Full PvP combat enabled
     * - No building to prevent griefing
     * - All item interactions allowed
     * - NO keep inventory - deaths have consequences
     * - NO invincible items - equipment can break
     * - Full mob spawning enabled
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
            // Items: All allowed - looting and item interactions permitted
            case ITEM_DROP -> true;
            case ITEM_PICKUP -> true;             // Auto pickup allowed
            case ITEM_PICKUP_MANUAL -> true;      // F-key pickup allowed
            case INVINCIBLE_ITEMS -> false;       // Items can break (no protection)
            // Damage: All environmental damage enabled
            case FALL_DAMAGE -> true;
            case ENVIRONMENTAL_DAMAGE -> true;
            // Death: No keep inventory - deaths have consequences
            case KEEP_INVENTORY -> false;
            // Mob Spawning: All mob spawning enabled in war zones
            case MOB_SPAWNING -> true;
            case HOSTILE_MOB_SPAWNING -> true;
            case PASSIVE_MOB_SPAWNING -> true;
            case NEUTRAL_MOB_SPAWNING -> true;
            case NPC_SPAWNING -> true;            // Mixin spawn hook allowed
            // Integration: Free for all in war zones
            case GRAVESTONE_ACCESS -> true;
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
     * Gets a map of all default flags for a zone type.
     * Useful for importing zones from mods that don't have a flag system.
     *
     * @param type the zone type
     * @return map of flag name to default value
     */
    @NotNull
    public static Map<String, Boolean> getDefaultFlags(@NotNull ZoneType type) {
        Map<String, Boolean> defaults = new HashMap<>();
        for (String flag : ALL_FLAGS) {
            defaults.put(flag, getDefault(flag, type));
        }
        return defaults;
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
            case ITEM_PICKUP -> "Auto Pickup";
            case ITEM_PICKUP_MANUAL -> "F-Key Pickup";
            case INVINCIBLE_ITEMS -> "Invincible Items";
            case FALL_DAMAGE -> "Fall Damage";
            case ENVIRONMENTAL_DAMAGE -> "Env. Damage";
            case KEEP_INVENTORY -> "Keep Inventory";
            case MOB_SPAWNING -> "Mob Spawning";
            case HOSTILE_MOB_SPAWNING -> "Hostile Mobs";
            case PASSIVE_MOB_SPAWNING -> "Passive Mobs";
            case NEUTRAL_MOB_SPAWNING -> "Neutral Mobs";
            case NPC_SPAWNING -> "NPC Spawning";
            case GRAVESTONE_ACCESS -> "Others Loot Graves";
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
            case ITEM_PICKUP -> "Auto-collect items when walking over them";
            case ITEM_PICKUP_MANUAL -> "Pick up items with F-key (requires mixin)";
            case INVINCIBLE_ITEMS -> "Items don't lose durability (requires mixin)";
            case FALL_DAMAGE -> "Fall damage applies";
            case ENVIRONMENTAL_DAMAGE -> "Drowning, suffocation, etc.";
            case KEEP_INVENTORY -> "Keep items on death (requires mixin)";
            case MOB_SPAWNING -> "Master toggle for mob spawning (parent)";
            case HOSTILE_MOB_SPAWNING -> "Aggressive mobs can spawn";
            case PASSIVE_MOB_SPAWNING -> "Non-aggressive mobs can spawn";
            case NEUTRAL_MOB_SPAWNING -> "Conditionally aggressive mobs can spawn";
            case NPC_SPAWNING -> "NPC spawning via mixin (requires mixin)";
            case GRAVESTONE_ACCESS -> "Non-owners can loot/break other players' gravestones (owners always can)";
            default -> "Unknown flag";
        };
    }

    /**
     * Gets the parent flag for a flag, if it has one.
     * Child flags only take effect when their parent is enabled.
     *
     * @param flagName the flag name
     * @return the parent flag name, or null if no parent
     */
    @Nullable
    public static String getParentFlag(String flagName) {
        return switch (flagName) {
            // Interaction flags have BLOCK_INTERACT as parent
            case DOOR_USE, CONTAINER_USE, BENCH_USE, PROCESSING_USE, SEAT_USE -> BLOCK_INTERACT;
            // Mob group flags have MOB_SPAWNING as parent
            case HOSTILE_MOB_SPAWNING, PASSIVE_MOB_SPAWNING, NEUTRAL_MOB_SPAWNING, NPC_SPAWNING -> MOB_SPAWNING;
            default -> null;
        };
    }

    /**
     * Checks if a flag is a parent flag (has children).
     *
     * @param flagName the flag name
     * @return true if this is a parent flag
     */
    public static boolean isParentFlag(String flagName) {
        return BLOCK_INTERACT.equals(flagName) || MOB_SPAWNING.equals(flagName);
    }

    /**
     * Gets the child flags for a parent flag.
     *
     * @param parentFlagName the parent flag name
     * @return array of child flag names, or empty array if not a parent
     */
    @NotNull
    public static String[] getChildFlags(String parentFlagName) {
        return switch (parentFlagName) {
            case BLOCK_INTERACT -> new String[] { DOOR_USE, CONTAINER_USE, BENCH_USE, PROCESSING_USE, SEAT_USE };
            case MOB_SPAWNING -> new String[] { HOSTILE_MOB_SPAWNING, PASSIVE_MOB_SPAWNING, NEUTRAL_MOB_SPAWNING, NPC_SPAWNING };
            default -> new String[0];
        };
    }

    // ==========================================================================
    // MIXIN DEPENDENCY METHODS
    // ==========================================================================

    /**
     * Checks if a flag requires OrbisGuard-Mixins to function.
     *
     * @param flagName the flag name
     * @return true if this flag requires mixin support
     */
    public static boolean requiresMixin(String flagName) {
        return MIXIN_FLAG_SET.contains(flagName);
    }

    /**
     * Gets the specific mixin type required for a flag.
     *
     * @param flagName the flag name
     * @return the mixin type constant, or null if no mixin required
     */
    @Nullable
    public static String getMixinType(String flagName) {
        return switch (flagName) {
            case ITEM_PICKUP_MANUAL -> MIXIN_PICKUP;
            case INVINCIBLE_ITEMS -> MIXIN_DURABILITY;
            case KEEP_INVENTORY -> MIXIN_DEATH;
            case NPC_SPAWNING -> MIXIN_SPAWN;
            default -> null;
        };
    }

    /**
     * Gets a human-readable name for a mixin type.
     *
     * @param mixinType the mixin type constant
     * @return the display name
     */
    @NotNull
    public static String getMixinDisplayName(String mixinType) {
        return switch (mixinType) {
            case MIXIN_PICKUP -> "Item Pickup Mixin";
            case MIXIN_DEATH -> "Death Event Mixin";
            case MIXIN_DURABILITY -> "Durability Mixin";
            case MIXIN_SEATING -> "Seating Mixin";
            case MIXIN_SPAWN -> "NPC Spawn Mixin";
            default -> "Unknown Mixin";
        };
    }

    /**
     * Gets the category name for UI organization.
     *
     * @param flagName the flag name
     * @return the category name
     */
    @NotNull
    public static String getCategory(String flagName) {
        for (String f : COMBAT_FLAGS) if (f.equals(flagName)) return "Combat";
        for (String f : BUILDING_FLAGS) if (f.equals(flagName)) return "Building";
        for (String f : DAMAGE_FLAGS) if (f.equals(flagName)) return "Damage";
        for (String f : DEATH_FLAGS) if (f.equals(flagName)) return "Death";
        for (String f : SPAWNING_FLAGS) if (f.equals(flagName)) return "Spawning";
        for (String f : INTERACTION_FLAGS) if (f.equals(flagName)) return "Interaction";
        for (String f : ITEM_FLAGS) if (f.equals(flagName)) return "Items";
        for (String f : INTEGRATION_FLAGS) if (f.equals(flagName)) return "Integration";
        return "Other";
    }

    /**
     * Gets all flags organized by category for UI display.
     *
     * @return map of category name to flag arrays
     */
    @NotNull
    public static Map<String, String[]> getFlagsByCategory() {
        Map<String, String[]> categories = new HashMap<>();
        categories.put("Combat", COMBAT_FLAGS);
        categories.put("Building", BUILDING_FLAGS);
        categories.put("Interaction", INTERACTION_FLAGS);
        categories.put("Items", ITEM_FLAGS);
        categories.put("Damage", DAMAGE_FLAGS);
        categories.put("Death", DEATH_FLAGS);
        categories.put("Spawning", SPAWNING_FLAGS);
        categories.put("Integration", INTEGRATION_FLAGS);
        return categories;
    }

    /**
     * Gets the ordered list of category names for UI display.
     *
     * @return array of category names in display order
     */
    @NotNull
    public static String[] getCategoryOrder() {
        return new String[] {
            "Combat",
            "Building",
            "Interaction",
            "Items",
            "Damage",
            "Death",
            "Spawning",
            "Integration"
        };
    }
}
