package com.hyperfactions.config.modules;

import com.google.gson.JsonObject;
import com.hyperfactions.config.ModuleConfig;
import com.hyperfactions.data.FactionPermissions;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Configuration for faction territory permissions.
 * <p>
 * Controls default permissions, locked permissions (server-enforced),
 * and forced values for locked permissions.
 */
public class FactionPermissionsConfig extends ModuleConfig {

    // Default permissions for new factions
    private boolean defaultOutsiderBreak = false;
    private boolean defaultOutsiderPlace = false;
    private boolean defaultOutsiderInteract = false;
    private boolean defaultAllyBreak = false;
    private boolean defaultAllyPlace = false;
    private boolean defaultAllyInteract = true;
    private boolean defaultMemberBreak = true;
    private boolean defaultMemberPlace = true;
    private boolean defaultMemberInteract = true;
    private boolean defaultPvpEnabled = true;
    private boolean defaultOfficersCanEdit = false;

    // Lock flags - when true, factions CANNOT change this setting
    private boolean lockOutsiderBreak = false;
    private boolean lockOutsiderPlace = false;
    private boolean lockOutsiderInteract = false;
    private boolean lockAllyBreak = false;
    private boolean lockAllyPlace = false;
    private boolean lockAllyInteract = false;
    private boolean lockMemberBreak = false;
    private boolean lockMemberPlace = false;
    private boolean lockMemberInteract = false;
    private boolean lockPvpEnabled = false;
    private boolean lockOfficersCanEdit = false;

    // Force values - when locked, use these values instead
    private boolean forceOutsiderBreak = false;
    private boolean forceOutsiderPlace = false;
    private boolean forceOutsiderInteract = false;
    private boolean forceAllyBreak = false;
    private boolean forceAllyPlace = false;
    private boolean forceAllyInteract = true;
    private boolean forceMemberBreak = true;
    private boolean forceMemberPlace = true;
    private boolean forceMemberInteract = true;
    private boolean forcePvpEnabled = true;
    private boolean forceOfficersCanEdit = false;

    /**
     * Creates a new faction permissions config.
     *
     * @param filePath path to config/faction-permissions.json
     */
    public FactionPermissionsConfig(@NotNull Path filePath) {
        super(filePath);
    }

    @Override
    @NotNull
    public String getModuleName() {
        return "faction-permissions";
    }

    @Override
    protected void createDefaults() {
        enabled = true;
        // Defaults are already set in field declarations
    }

    @Override
    protected void loadModuleSettings(@NotNull JsonObject root) {
        // Load defaults section
        if (hasSection(root, "defaults")) {
            JsonObject defaults = root.getAsJsonObject("defaults");
            defaultOutsiderBreak = getBool(defaults, "outsiderBreak", defaultOutsiderBreak);
            defaultOutsiderPlace = getBool(defaults, "outsiderPlace", defaultOutsiderPlace);
            defaultOutsiderInteract = getBool(defaults, "outsiderInteract", defaultOutsiderInteract);
            defaultAllyBreak = getBool(defaults, "allyBreak", defaultAllyBreak);
            defaultAllyPlace = getBool(defaults, "allyPlace", defaultAllyPlace);
            defaultAllyInteract = getBool(defaults, "allyInteract", defaultAllyInteract);
            defaultMemberBreak = getBool(defaults, "memberBreak", defaultMemberBreak);
            defaultMemberPlace = getBool(defaults, "memberPlace", defaultMemberPlace);
            defaultMemberInteract = getBool(defaults, "memberInteract", defaultMemberInteract);
            defaultPvpEnabled = getBool(defaults, "pvpEnabled", defaultPvpEnabled);
            defaultOfficersCanEdit = getBool(defaults, "officersCanEdit", defaultOfficersCanEdit);
        }

        // Load locks section
        if (hasSection(root, "locks")) {
            JsonObject locks = root.getAsJsonObject("locks");
            lockOutsiderBreak = getBool(locks, "outsiderBreak", lockOutsiderBreak);
            lockOutsiderPlace = getBool(locks, "outsiderPlace", lockOutsiderPlace);
            lockOutsiderInteract = getBool(locks, "outsiderInteract", lockOutsiderInteract);
            lockAllyBreak = getBool(locks, "allyBreak", lockAllyBreak);
            lockAllyPlace = getBool(locks, "allyPlace", lockAllyPlace);
            lockAllyInteract = getBool(locks, "allyInteract", lockAllyInteract);
            lockMemberBreak = getBool(locks, "memberBreak", lockMemberBreak);
            lockMemberPlace = getBool(locks, "memberPlace", lockMemberPlace);
            lockMemberInteract = getBool(locks, "memberInteract", lockMemberInteract);
            lockPvpEnabled = getBool(locks, "pvpEnabled", lockPvpEnabled);
            lockOfficersCanEdit = getBool(locks, "officersCanEdit", lockOfficersCanEdit);
        }

        // Load forced section
        if (hasSection(root, "forced")) {
            JsonObject forced = root.getAsJsonObject("forced");
            forceOutsiderBreak = getBool(forced, "outsiderBreak", forceOutsiderBreak);
            forceOutsiderPlace = getBool(forced, "outsiderPlace", forceOutsiderPlace);
            forceOutsiderInteract = getBool(forced, "outsiderInteract", forceOutsiderInteract);
            forceAllyBreak = getBool(forced, "allyBreak", forceAllyBreak);
            forceAllyPlace = getBool(forced, "allyPlace", forceAllyPlace);
            forceAllyInteract = getBool(forced, "allyInteract", forceAllyInteract);
            forceMemberBreak = getBool(forced, "memberBreak", forceMemberBreak);
            forceMemberPlace = getBool(forced, "memberPlace", forceMemberPlace);
            forceMemberInteract = getBool(forced, "memberInteract", forceMemberInteract);
            forcePvpEnabled = getBool(forced, "pvpEnabled", forcePvpEnabled);
            forceOfficersCanEdit = getBool(forced, "officersCanEdit", forceOfficersCanEdit);
        }
    }

    @Override
    protected void writeModuleSettings(@NotNull JsonObject root) {
        // Write defaults section
        JsonObject defaults = new JsonObject();
        defaults.addProperty("outsiderBreak", defaultOutsiderBreak);
        defaults.addProperty("outsiderPlace", defaultOutsiderPlace);
        defaults.addProperty("outsiderInteract", defaultOutsiderInteract);
        defaults.addProperty("allyBreak", defaultAllyBreak);
        defaults.addProperty("allyPlace", defaultAllyPlace);
        defaults.addProperty("allyInteract", defaultAllyInteract);
        defaults.addProperty("memberBreak", defaultMemberBreak);
        defaults.addProperty("memberPlace", defaultMemberPlace);
        defaults.addProperty("memberInteract", defaultMemberInteract);
        defaults.addProperty("pvpEnabled", defaultPvpEnabled);
        defaults.addProperty("officersCanEdit", defaultOfficersCanEdit);
        root.add("defaults", defaults);

        // Write locks section
        JsonObject locks = new JsonObject();
        locks.addProperty("outsiderBreak", lockOutsiderBreak);
        locks.addProperty("outsiderPlace", lockOutsiderPlace);
        locks.addProperty("outsiderInteract", lockOutsiderInteract);
        locks.addProperty("allyBreak", lockAllyBreak);
        locks.addProperty("allyPlace", lockAllyPlace);
        locks.addProperty("allyInteract", lockAllyInteract);
        locks.addProperty("memberBreak", lockMemberBreak);
        locks.addProperty("memberPlace", lockMemberPlace);
        locks.addProperty("memberInteract", lockMemberInteract);
        locks.addProperty("pvpEnabled", lockPvpEnabled);
        locks.addProperty("officersCanEdit", lockOfficersCanEdit);
        root.add("locks", locks);

        // Write forced section
        JsonObject forced = new JsonObject();
        forced.addProperty("outsiderBreak", forceOutsiderBreak);
        forced.addProperty("outsiderPlace", forceOutsiderPlace);
        forced.addProperty("outsiderInteract", forceOutsiderInteract);
        forced.addProperty("allyBreak", forceAllyBreak);
        forced.addProperty("allyPlace", forceAllyPlace);
        forced.addProperty("allyInteract", forceAllyInteract);
        forced.addProperty("memberBreak", forceMemberBreak);
        forced.addProperty("memberPlace", forceMemberPlace);
        forced.addProperty("memberInteract", forceMemberInteract);
        forced.addProperty("pvpEnabled", forcePvpEnabled);
        forced.addProperty("officersCanEdit", forceOfficersCanEdit);
        root.add("forced", forced);
    }

    // === FactionPermissions Integration ===

    /**
     * Gets the default permissions for new factions based on config.
     *
     * @return the default faction permissions from config
     */
    @NotNull
    public FactionPermissions getDefaultFactionPermissions() {
        return new FactionPermissions(
            defaultOutsiderBreak, defaultOutsiderPlace, defaultOutsiderInteract,
            defaultAllyBreak, defaultAllyPlace, defaultAllyInteract,
            defaultMemberBreak, defaultMemberPlace, defaultMemberInteract,
            defaultPvpEnabled, defaultOfficersCanEdit
        );
    }

    /**
     * Gets the effective permissions for a faction, applying server locks/forced values.
     * For each field: if locked, use forced value; else use faction value.
     *
     * @param factionPerms the faction's permission settings
     * @return the effective permissions after applying server locks
     */
    @NotNull
    public FactionPermissions getEffectiveFactionPermissions(@NotNull FactionPermissions factionPerms) {
        return new FactionPermissions(
            lockOutsiderBreak ? forceOutsiderBreak : factionPerms.outsiderBreak(),
            lockOutsiderPlace ? forceOutsiderPlace : factionPerms.outsiderPlace(),
            lockOutsiderInteract ? forceOutsiderInteract : factionPerms.outsiderInteract(),
            lockAllyBreak ? forceAllyBreak : factionPerms.allyBreak(),
            lockAllyPlace ? forceAllyPlace : factionPerms.allyPlace(),
            lockAllyInteract ? forceAllyInteract : factionPerms.allyInteract(),
            lockMemberBreak ? forceMemberBreak : factionPerms.memberBreak(),
            lockMemberPlace ? forceMemberPlace : factionPerms.memberPlace(),
            lockMemberInteract ? forceMemberInteract : factionPerms.memberInteract(),
            lockPvpEnabled ? forcePvpEnabled : factionPerms.pvpEnabled(),
            lockOfficersCanEdit ? forceOfficersCanEdit : factionPerms.officersCanEdit()
        );
    }

    /**
     * Checks if a specific permission is locked by the server.
     *
     * @param permissionName the permission name (e.g., "outsiderBreak", "pvpEnabled")
     * @return true if locked, false if factions can change it
     */
    public boolean isPermissionLocked(@NotNull String permissionName) {
        return switch (permissionName) {
            case "outsiderBreak" -> lockOutsiderBreak;
            case "outsiderPlace" -> lockOutsiderPlace;
            case "outsiderInteract" -> lockOutsiderInteract;
            case "allyBreak" -> lockAllyBreak;
            case "allyPlace" -> lockAllyPlace;
            case "allyInteract" -> lockAllyInteract;
            case "memberBreak" -> lockMemberBreak;
            case "memberPlace" -> lockMemberPlace;
            case "memberInteract" -> lockMemberInteract;
            case "pvpEnabled" -> lockPvpEnabled;
            case "officersCanEdit" -> lockOfficersCanEdit;
            default -> false;
        };
    }
}
