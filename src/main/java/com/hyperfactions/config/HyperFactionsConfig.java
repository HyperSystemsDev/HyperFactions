package com.hyperfactions.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hyperfactions.data.FactionPermissions;
import com.hyperfactions.util.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration singleton for HyperFactions.
 */
public class HyperFactionsConfig {

    private static HyperFactionsConfig instance;

    // Faction settings
    private int maxMembers = 50;
    private int maxNameLength = 24;
    private int minNameLength = 3;
    private boolean allowColors = true;

    // Power settings
    private double maxPlayerPower = 20.0;
    private double startingPower = 10.0;
    private double powerPerClaim = 2.0;
    private double deathPenalty = 1.0;
    private double regenPerMinute = 0.1;
    private boolean regenWhenOffline = false;

    // Claim settings
    private int maxClaims = 100;
    private boolean onlyAdjacent = false;
    private boolean decayEnabled = true;
    private int decayDaysInactive = 30;
    private List<String> worldWhitelist = new ArrayList<>();
    private List<String> worldBlacklist = new ArrayList<>();

    // Combat settings
    private int tagDurationSeconds = 15;
    private boolean allyDamage = false;
    private boolean factionDamage = false;
    private boolean taggedLogoutPenalty = true;

    // Spawn protection settings
    private boolean spawnProtectionEnabled = true;
    private int spawnProtectionDurationSeconds = 5;
    private boolean spawnProtectionBreakOnAttack = true;
    private boolean spawnProtectionBreakOnMove = true;

    // Relation settings
    private int maxAllies = 10;      // -1 for unlimited
    private int maxEnemies = -1;     // -1 for unlimited

    // Invite/Request settings
    private int inviteExpirationMinutes = 5;         // How long faction invites last
    private int joinRequestExpirationHours = 24;     // How long join requests last

    // Stuck command settings
    private int stuckWarmupSeconds = 30;
    private int stuckCooldownSeconds = 300;  // 5 minutes

    // Teleport settings
    private int warmupSeconds = 5;
    private int cooldownSeconds = 300;
    private boolean cancelOnMove = true;
    private boolean cancelOnDamage = true;

    // Update settings
    private boolean updateCheckEnabled = true;
    private String updateCheckUrl = "https://api.github.com/repos/ZenithDevHQ/HyperFactions/releases/latest";
    private String releaseChannel = "stable";  // "stable" or "prerelease"

    // Auto-save settings
    private boolean autoSaveEnabled = true;
    private int autoSaveIntervalMinutes = 5;

    // Backup settings (GFS rotation scheme)
    private boolean backupEnabled = true;
    private int backupHourlyRetention = 24;   // Keep last 24 hourly backups
    private int backupDailyRetention = 7;     // Keep last 7 daily backups
    private int backupWeeklyRetention = 4;    // Keep last 4 weekly backups
    private boolean backupOnShutdown = true;  // Create backup on server shutdown

    // Economy settings
    private boolean economyEnabled = true;
    private String economyCurrencyName = "dollar";
    private String economyCurrencyNamePlural = "dollars";
    private String economyCurrencySymbol = "$";
    private double economyStartingBalance = 0.0;

    // Message settings
    private String prefix = "\u00A7b[HyperFactions]\u00A7r ";
    private String primaryColor = "#00FFFF";

    // GUI settings
    private String guiTitle = "HyperFactions";  // Title shown in nav bar

    // Territory notification settings
    private boolean territoryNotificationsEnabled = true;

    // World map marker settings
    private boolean worldMapMarkersEnabled = true;

    // Debug settings
    private boolean debugEnabledByDefault = false;
    private boolean debugLogToConsole = true;
    private boolean debugPower = false;
    private boolean debugClaim = false;
    private boolean debugCombat = false;
    private boolean debugProtection = false;
    private boolean debugRelation = false;
    private boolean debugTerritory = false;

    // Chat formatting settings
    private boolean chatFormattingEnabled = true;
    private String chatFormat = "{faction_tag}{prefix}{player}{suffix}: {message}";
    private String chatTagDisplay = "tag";  // "tag", "name", or "none"
    private String chatTagFormat = "[{tag}] ";
    private String chatRelationColorOwn = "#00FF00";     // Green - same faction
    private String chatRelationColorAlly = "#FF69B4";    // Pink - allies
    private String chatRelationColorNeutral = "#AAAAAA"; // Gray - neutral
    private String chatRelationColorEnemy = "#FF0000";   // Red - enemies
    private String chatNoFactionTag = "";                // Empty = no tag for non-faction players
    private String chatEventPriority = "LATE";           // After LuckPerms (NORMAL)

    // Permission settings
    private boolean adminRequiresOp = true;
    private String permissionFallbackBehavior = "allow";

    // Faction territory permission settings
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

    private HyperFactionsConfig() {}

    /**
     * Gets the singleton config instance.
     *
     * @return the config instance
     */
    public static HyperFactionsConfig get() {
        if (instance == null) {
            instance = new HyperFactionsConfig();
        }
        return instance;
    }

    // Track if config needs saving (missing keys were found)
    private boolean configNeedsSave = false;

    /**
     * Loads the configuration from file.
     * Any missing keys will be added with default values and the config will be re-saved.
     * User-defined values are always preserved.
     *
     * @param dataDir the plugin data directory
     */
    public void load(@NotNull Path dataDir) {
        Path configFile = dataDir.resolve("config.json");
        Logger.info("[Config] Loading from: %s", configFile.toAbsolutePath());

        if (!Files.exists(configFile)) {
            Logger.info("[Config] Config file not found, creating default");
            save(dataDir);
            return;
        }

        configNeedsSave = false;  // Reset flag

        try {
            String json = Files.readString(configFile);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            // Faction settings
            if (hasSection(root, "faction")) {
                JsonObject faction = root.getAsJsonObject("faction");
                maxMembers = getInt(faction, "maxMembers", maxMembers);
                maxNameLength = getInt(faction, "maxNameLength", maxNameLength);
                minNameLength = getInt(faction, "minNameLength", minNameLength);
                allowColors = getBool(faction, "allowColors", allowColors);
            }

            // Power settings
            if (hasSection(root, "power")) {
                JsonObject power = root.getAsJsonObject("power");
                maxPlayerPower = getDouble(power, "maxPlayerPower", maxPlayerPower);
                startingPower = getDouble(power, "startingPower", startingPower);
                powerPerClaim = getDouble(power, "powerPerClaim", powerPerClaim);
                deathPenalty = getDouble(power, "deathPenalty", deathPenalty);
                regenPerMinute = getDouble(power, "regenPerMinute", regenPerMinute);
                regenWhenOffline = getBool(power, "regenWhenOffline", regenWhenOffline);
            }

            // Claim settings
            if (hasSection(root, "claims")) {
                JsonObject claims = root.getAsJsonObject("claims");
                maxClaims = getInt(claims, "maxClaims", maxClaims);
                onlyAdjacent = getBool(claims, "onlyAdjacent", onlyAdjacent);
                decayEnabled = getBool(claims, "decayEnabled", decayEnabled);
                decayDaysInactive = getInt(claims, "decayDaysInactive", decayDaysInactive);
                worldWhitelist = getStringList(claims, "worldWhitelist");
                worldBlacklist = getStringList(claims, "worldBlacklist");
            }

            // Combat settings
            if (hasSection(root, "combat")) {
                JsonObject combat = root.getAsJsonObject("combat");
                tagDurationSeconds = getInt(combat, "tagDurationSeconds", tagDurationSeconds);
                allyDamage = getBool(combat, "allyDamage", allyDamage);
                factionDamage = getBool(combat, "factionDamage", factionDamage);
                taggedLogoutPenalty = getBool(combat, "taggedLogoutPenalty", taggedLogoutPenalty);

                // Spawn protection sub-section
                if (hasSection(combat, "spawnProtection")) {
                    JsonObject spawnProt = combat.getAsJsonObject("spawnProtection");
                    spawnProtectionEnabled = getBool(spawnProt, "enabled", spawnProtectionEnabled);
                    spawnProtectionDurationSeconds = getInt(spawnProt, "durationSeconds", spawnProtectionDurationSeconds);
                    spawnProtectionBreakOnAttack = getBool(spawnProt, "breakOnAttack", spawnProtectionBreakOnAttack);
                    spawnProtectionBreakOnMove = getBool(spawnProt, "breakOnMove", spawnProtectionBreakOnMove);
                }
            }

            // Relation settings
            if (hasSection(root, "relations")) {
                JsonObject relations = root.getAsJsonObject("relations");
                maxAllies = getInt(relations, "maxAllies", maxAllies);
                maxEnemies = getInt(relations, "maxEnemies", maxEnemies);
            }

            // Invite/Request settings
            if (hasSection(root, "invites")) {
                JsonObject invites = root.getAsJsonObject("invites");
                inviteExpirationMinutes = getInt(invites, "inviteExpirationMinutes", inviteExpirationMinutes);
                joinRequestExpirationHours = getInt(invites, "joinRequestExpirationHours", joinRequestExpirationHours);
            }

            // Stuck settings
            if (hasSection(root, "stuck")) {
                JsonObject stuck = root.getAsJsonObject("stuck");
                stuckWarmupSeconds = getInt(stuck, "warmupSeconds", stuckWarmupSeconds);
                stuckCooldownSeconds = getInt(stuck, "cooldownSeconds", stuckCooldownSeconds);
            }

            // Teleport settings
            if (hasSection(root, "teleport")) {
                JsonObject teleport = root.getAsJsonObject("teleport");
                warmupSeconds = getInt(teleport, "warmupSeconds", warmupSeconds);
                cooldownSeconds = getInt(teleport, "cooldownSeconds", cooldownSeconds);
                cancelOnMove = getBool(teleport, "cancelOnMove", cancelOnMove);
                cancelOnDamage = getBool(teleport, "cancelOnDamage", cancelOnDamage);
            }

            // Update settings
            if (hasSection(root, "updates")) {
                JsonObject updates = root.getAsJsonObject("updates");
                updateCheckEnabled = getBool(updates, "enabled", updateCheckEnabled);
                updateCheckUrl = getString(updates, "url", updateCheckUrl);
                releaseChannel = getString(updates, "releaseChannel", releaseChannel);
                // Validate release channel
                if (!releaseChannel.equals("stable") && !releaseChannel.equals("prerelease")) {
                    Logger.warn("Invalid releaseChannel '%s', using 'stable'", releaseChannel);
                    releaseChannel = "stable";
                }
            }

            // Auto-save settings
            if (hasSection(root, "autoSave")) {
                JsonObject autoSave = root.getAsJsonObject("autoSave");
                autoSaveEnabled = getBool(autoSave, "enabled", autoSaveEnabled);
                autoSaveIntervalMinutes = getInt(autoSave, "intervalMinutes", autoSaveIntervalMinutes);
            }

            // Backup settings
            if (hasSection(root, "backup")) {
                JsonObject backup = root.getAsJsonObject("backup");
                backupEnabled = getBool(backup, "enabled", backupEnabled);
                backupHourlyRetention = getInt(backup, "hourlyRetention", backupHourlyRetention);
                backupDailyRetention = getInt(backup, "dailyRetention", backupDailyRetention);
                backupWeeklyRetention = getInt(backup, "weeklyRetention", backupWeeklyRetention);
                backupOnShutdown = getBool(backup, "onShutdown", backupOnShutdown);
            }

            // Economy settings
            if (hasSection(root, "economy")) {
                JsonObject economy = root.getAsJsonObject("economy");
                economyEnabled = getBool(economy, "enabled", economyEnabled);
                economyCurrencyName = getString(economy, "currencyName", economyCurrencyName);
                economyCurrencyNamePlural = getString(economy, "currencyNamePlural", economyCurrencyNamePlural);
                economyCurrencySymbol = getString(economy, "currencySymbol", economyCurrencySymbol);
                economyStartingBalance = getDouble(economy, "startingBalance", economyStartingBalance);
            }

            // Message settings
            if (hasSection(root, "messages")) {
                JsonObject messages = root.getAsJsonObject("messages");
                prefix = getString(messages, "prefix", prefix);
                primaryColor = getString(messages, "primaryColor", primaryColor);
            }

            // GUI settings
            if (hasSection(root, "gui")) {
                JsonObject gui = root.getAsJsonObject("gui");
                guiTitle = getString(gui, "title", guiTitle);
            }

            // Territory notification settings
            if (hasSection(root, "territoryNotifications")) {
                JsonObject territoryNotifications = root.getAsJsonObject("territoryNotifications");
                territoryNotificationsEnabled = getBool(territoryNotifications, "enabled", territoryNotificationsEnabled);
            }

            // World map marker settings
            if (hasSection(root, "worldMap")) {
                JsonObject worldMap = root.getAsJsonObject("worldMap");
                worldMapMarkersEnabled = getBool(worldMap, "enabled", worldMapMarkersEnabled);
            }

            // Debug settings
            if (hasSection(root, "debug")) {
                JsonObject debug = root.getAsJsonObject("debug");
                debugEnabledByDefault = getBool(debug, "enabledByDefault", debugEnabledByDefault);
                debugLogToConsole = getBool(debug, "logToConsole", debugLogToConsole);

                if (hasSection(debug, "categories")) {
                    JsonObject categories = debug.getAsJsonObject("categories");
                    debugPower = getBool(categories, "power", debugPower);
                    debugClaim = getBool(categories, "claim", debugClaim);
                    debugCombat = getBool(categories, "combat", debugCombat);
                    debugProtection = getBool(categories, "protection", debugProtection);
                    debugRelation = getBool(categories, "relation", debugRelation);
                    debugTerritory = getBool(categories, "territory", debugTerritory);
                }
            }

            // Chat settings
            if (hasSection(root, "chat")) {
                JsonObject chat = root.getAsJsonObject("chat");
                chatFormattingEnabled = getBool(chat, "enabled", chatFormattingEnabled);
                chatFormat = getString(chat, "format", chatFormat);
                chatTagDisplay = getString(chat, "tagDisplay", chatTagDisplay);
                chatTagFormat = getString(chat, "tagFormat", chatTagFormat);
                chatNoFactionTag = getString(chat, "noFactionTag", chatNoFactionTag);
                chatEventPriority = getString(chat, "priority", chatEventPriority);

                if (hasSection(chat, "relationColors")) {
                    JsonObject colors = chat.getAsJsonObject("relationColors");
                    chatRelationColorOwn = getString(colors, "own", chatRelationColorOwn);
                    chatRelationColorAlly = getString(colors, "ally", chatRelationColorAlly);
                    chatRelationColorNeutral = getString(colors, "neutral", chatRelationColorNeutral);
                    chatRelationColorEnemy = getString(colors, "enemy", chatRelationColorEnemy);
                }
            }

            // Permission settings
            if (hasSection(root, "permissions")) {
                JsonObject permissions = root.getAsJsonObject("permissions");
                adminRequiresOp = getBool(permissions, "adminRequiresOp", adminRequiresOp);
                permissionFallbackBehavior = getString(permissions, "fallbackBehavior", permissionFallbackBehavior);
            }

            // Faction territory permission settings
            if (hasSection(root, "factionPermissions")) {
                JsonObject factionPerms = root.getAsJsonObject("factionPermissions");

                // Defaults section
                if (hasSection(factionPerms, "defaults")) {
                    JsonObject defaults = factionPerms.getAsJsonObject("defaults");
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

                // Locks section
                if (hasSection(factionPerms, "locks")) {
                    JsonObject locks = factionPerms.getAsJsonObject("locks");
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

                // Forced section
                if (hasSection(factionPerms, "forced")) {
                    JsonObject forced = factionPerms.getAsJsonObject("forced");
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

            // Apply debug settings to Logger
            applyDebugSettings();

            Logger.info("Configuration loaded");

            // Save config only if there were missing keys to add
            if (configNeedsSave) {
                Logger.info("[Config] Adding missing config keys with default values");
                save(dataDir);
            }
        } catch (Exception e) {
            Logger.severe("Failed to load configuration", e);
        }
    }

    /**
     * Saves the configuration to file.
     *
     * @param dataDir the plugin data directory
     */
    public void save(@NotNull Path dataDir) {
        Path configFile = dataDir.resolve("config.json");

        try {
            Files.createDirectories(dataDir);

            JsonObject root = new JsonObject();

            // Faction settings
            JsonObject faction = new JsonObject();
            faction.addProperty("maxMembers", maxMembers);
            faction.addProperty("maxNameLength", maxNameLength);
            faction.addProperty("minNameLength", minNameLength);
            faction.addProperty("allowColors", allowColors);
            root.add("faction", faction);

            // Power settings
            JsonObject power = new JsonObject();
            power.addProperty("maxPlayerPower", maxPlayerPower);
            power.addProperty("startingPower", startingPower);
            power.addProperty("powerPerClaim", powerPerClaim);
            power.addProperty("deathPenalty", deathPenalty);
            power.addProperty("regenPerMinute", regenPerMinute);
            power.addProperty("regenWhenOffline", regenWhenOffline);
            root.add("power", power);

            // Claim settings
            JsonObject claims = new JsonObject();
            claims.addProperty("maxClaims", maxClaims);
            claims.addProperty("onlyAdjacent", onlyAdjacent);
            claims.addProperty("decayEnabled", decayEnabled);
            claims.addProperty("decayDaysInactive", decayDaysInactive);
            JsonArray wl = new JsonArray();
            worldWhitelist.forEach(wl::add);
            claims.add("worldWhitelist", wl);
            JsonArray bl = new JsonArray();
            worldBlacklist.forEach(bl::add);
            claims.add("worldBlacklist", bl);
            root.add("claims", claims);

            // Combat settings
            JsonObject combat = new JsonObject();
            combat.addProperty("tagDurationSeconds", tagDurationSeconds);
            combat.addProperty("allyDamage", allyDamage);
            combat.addProperty("factionDamage", factionDamage);
            combat.addProperty("taggedLogoutPenalty", taggedLogoutPenalty);

            // Spawn protection sub-section
            JsonObject spawnProt = new JsonObject();
            spawnProt.addProperty("enabled", spawnProtectionEnabled);
            spawnProt.addProperty("durationSeconds", spawnProtectionDurationSeconds);
            spawnProt.addProperty("breakOnAttack", spawnProtectionBreakOnAttack);
            spawnProt.addProperty("breakOnMove", spawnProtectionBreakOnMove);
            combat.add("spawnProtection", spawnProt);

            root.add("combat", combat);

            // Relation settings
            JsonObject relations = new JsonObject();
            relations.addProperty("maxAllies", maxAllies);
            relations.addProperty("maxEnemies", maxEnemies);
            root.add("relations", relations);

            // Invite/Request settings
            JsonObject invites = new JsonObject();
            invites.addProperty("inviteExpirationMinutes", inviteExpirationMinutes);
            invites.addProperty("joinRequestExpirationHours", joinRequestExpirationHours);
            root.add("invites", invites);

            // Stuck settings
            JsonObject stuck = new JsonObject();
            stuck.addProperty("warmupSeconds", stuckWarmupSeconds);
            stuck.addProperty("cooldownSeconds", stuckCooldownSeconds);
            root.add("stuck", stuck);

            // Teleport settings
            JsonObject teleport = new JsonObject();
            teleport.addProperty("warmupSeconds", warmupSeconds);
            teleport.addProperty("cooldownSeconds", cooldownSeconds);
            teleport.addProperty("cancelOnMove", cancelOnMove);
            teleport.addProperty("cancelOnDamage", cancelOnDamage);
            root.add("teleport", teleport);

            // Update settings
            JsonObject updates = new JsonObject();
            updates.addProperty("enabled", updateCheckEnabled);
            updates.addProperty("url", updateCheckUrl);
            updates.addProperty("releaseChannel", releaseChannel);
            root.add("updates", updates);

            // Auto-save settings
            JsonObject autoSave = new JsonObject();
            autoSave.addProperty("enabled", autoSaveEnabled);
            autoSave.addProperty("intervalMinutes", autoSaveIntervalMinutes);
            root.add("autoSave", autoSave);

            // Backup settings
            JsonObject backup = new JsonObject();
            backup.addProperty("enabled", backupEnabled);
            backup.addProperty("hourlyRetention", backupHourlyRetention);
            backup.addProperty("dailyRetention", backupDailyRetention);
            backup.addProperty("weeklyRetention", backupWeeklyRetention);
            backup.addProperty("onShutdown", backupOnShutdown);
            root.add("backup", backup);

            // Economy settings
            JsonObject economy = new JsonObject();
            economy.addProperty("enabled", economyEnabled);
            economy.addProperty("currencyName", economyCurrencyName);
            economy.addProperty("currencyNamePlural", economyCurrencyNamePlural);
            economy.addProperty("currencySymbol", economyCurrencySymbol);
            economy.addProperty("startingBalance", economyStartingBalance);
            root.add("economy", economy);

            // Message settings
            JsonObject messages = new JsonObject();
            messages.addProperty("prefix", prefix);
            messages.addProperty("primaryColor", primaryColor);
            root.add("messages", messages);

            // GUI settings
            JsonObject gui = new JsonObject();
            gui.addProperty("title", guiTitle);
            root.add("gui", gui);

            // Territory notification settings
            JsonObject territoryNotifications = new JsonObject();
            territoryNotifications.addProperty("enabled", territoryNotificationsEnabled);
            root.add("territoryNotifications", territoryNotifications);

            // World map marker settings
            JsonObject worldMap = new JsonObject();
            worldMap.addProperty("enabled", worldMapMarkersEnabled);
            root.add("worldMap", worldMap);

            // Debug settings
            JsonObject debug = new JsonObject();
            debug.addProperty("enabledByDefault", debugEnabledByDefault);
            debug.addProperty("logToConsole", debugLogToConsole);

            JsonObject categories = new JsonObject();
            categories.addProperty("power", debugPower);
            categories.addProperty("claim", debugClaim);
            categories.addProperty("combat", debugCombat);
            categories.addProperty("protection", debugProtection);
            categories.addProperty("relation", debugRelation);
            categories.addProperty("territory", debugTerritory);
            debug.add("categories", categories);
            root.add("debug", debug);

            // Chat settings
            JsonObject chat = new JsonObject();
            chat.addProperty("enabled", chatFormattingEnabled);
            chat.addProperty("format", chatFormat);
            chat.addProperty("tagDisplay", chatTagDisplay);
            chat.addProperty("tagFormat", chatTagFormat);
            chat.addProperty("noFactionTag", chatNoFactionTag);
            chat.addProperty("priority", chatEventPriority);

            JsonObject relationColors = new JsonObject();
            relationColors.addProperty("own", chatRelationColorOwn);
            relationColors.addProperty("ally", chatRelationColorAlly);
            relationColors.addProperty("neutral", chatRelationColorNeutral);
            relationColors.addProperty("enemy", chatRelationColorEnemy);
            chat.add("relationColors", relationColors);
            root.add("chat", chat);

            // Permission settings
            JsonObject permissions = new JsonObject();
            permissions.addProperty("adminRequiresOp", adminRequiresOp);
            permissions.addProperty("fallbackBehavior", permissionFallbackBehavior);
            root.add("permissions", permissions);

            // Faction territory permission settings
            JsonObject factionPerms = new JsonObject();

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
            factionPerms.add("defaults", defaults);

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
            factionPerms.add("locks", locks);

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
            factionPerms.add("forced", forced);

            root.add("factionPermissions", factionPerms);

            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
            Files.writeString(configFile, gson.toJson(root));

            Logger.info("Configuration saved");
        } catch (IOException e) {
            Logger.severe("Failed to save configuration", e);
        }
    }

    /**
     * Reloads the configuration.
     *
     * @param dataDir the plugin data directory
     */
    public void reload(@NotNull Path dataDir) {
        load(dataDir);
    }

    // === Faction Getters ===
    public int getMaxMembers() { return maxMembers; }
    public int getMaxNameLength() { return maxNameLength; }
    public int getMinNameLength() { return minNameLength; }
    public boolean isAllowColors() { return allowColors; }

    // === Power Getters ===
    public double getMaxPlayerPower() { return maxPlayerPower; }
    public double getStartingPower() { return startingPower; }
    public double getPowerPerClaim() { return powerPerClaim; }
    public double getDeathPenalty() { return deathPenalty; }
    public double getRegenPerMinute() { return regenPerMinute; }
    public boolean isRegenWhenOffline() { return regenWhenOffline; }

    // === Claim Getters ===
    public int getMaxClaims() { return maxClaims; }
    public boolean isOnlyAdjacent() { return onlyAdjacent; }
    public boolean isDecayEnabled() { return decayEnabled; }
    public int getDecayDaysInactive() { return decayDaysInactive; }
    public List<String> getWorldWhitelist() { return worldWhitelist; }
    public List<String> getWorldBlacklist() { return worldBlacklist; }

    // === Combat Getters ===
    public int getTagDurationSeconds() { return tagDurationSeconds; }
    public boolean isAllyDamage() { return allyDamage; }
    public boolean isFactionDamage() { return factionDamage; }
    public boolean isTaggedLogoutPenalty() { return taggedLogoutPenalty; }

    // === Spawn Protection Getters ===
    public boolean isSpawnProtectionEnabled() { return spawnProtectionEnabled; }
    public int getSpawnProtectionDurationSeconds() { return spawnProtectionDurationSeconds; }
    public boolean isSpawnProtectionBreakOnAttack() { return spawnProtectionBreakOnAttack; }
    public boolean isSpawnProtectionBreakOnMove() { return spawnProtectionBreakOnMove; }

    // === Relation Getters ===
    public int getMaxAllies() { return maxAllies; }
    public int getMaxEnemies() { return maxEnemies; }

    // === Invite/Request Getters ===
    public int getInviteExpirationMinutes() { return inviteExpirationMinutes; }
    public int getJoinRequestExpirationHours() { return joinRequestExpirationHours; }
    public long getInviteExpirationMs() { return inviteExpirationMinutes * 60 * 1000L; }
    public long getJoinRequestExpirationMs() { return joinRequestExpirationHours * 60 * 60 * 1000L; }

    // === Stuck Getters ===
    public int getStuckWarmupSeconds() { return stuckWarmupSeconds; }
    public int getStuckCooldownSeconds() { return stuckCooldownSeconds; }

    // === Teleport Getters ===
    public int getWarmupSeconds() { return warmupSeconds; }
    public int getCooldownSeconds() { return cooldownSeconds; }
    public boolean isCancelOnMove() { return cancelOnMove; }
    public boolean isCancelOnDamage() { return cancelOnDamage; }

    // === Update Getters ===
    public boolean isUpdateCheckEnabled() { return updateCheckEnabled; }
    public String getUpdateCheckUrl() { return updateCheckUrl; }
    public String getReleaseChannel() { return releaseChannel; }
    public boolean isPreReleaseChannel() { return "prerelease".equals(releaseChannel); }

    // === Auto-save Getters ===
    public boolean isAutoSaveEnabled() { return autoSaveEnabled; }
    public int getAutoSaveIntervalMinutes() { return autoSaveIntervalMinutes; }

    // === Backup Getters ===
    public boolean isBackupEnabled() { return backupEnabled; }
    public int getBackupHourlyRetention() { return backupHourlyRetention; }
    public int getBackupDailyRetention() { return backupDailyRetention; }
    public int getBackupWeeklyRetention() { return backupWeeklyRetention; }
    public boolean isBackupOnShutdown() { return backupOnShutdown; }

    // === Economy Getters ===
    public boolean isEconomyEnabled() { return economyEnabled; }
    public String getEconomyCurrencyName() { return economyCurrencyName; }
    public String getEconomyCurrencyNamePlural() { return economyCurrencyNamePlural; }
    public String getEconomyCurrencySymbol() { return economyCurrencySymbol; }
    public double getEconomyStartingBalance() { return economyStartingBalance; }

    // === Message Getters ===
    public String getPrefix() { return prefix; }
    public String getPrimaryColor() { return primaryColor; }

    // === GUI Getters ===
    public String getGuiTitle() { return guiTitle; }

    // === Territory Notification Getters ===
    public boolean isTerritoryNotificationsEnabled() { return territoryNotificationsEnabled; }

    // === World Map Getters ===
    public boolean isWorldMapMarkersEnabled() { return worldMapMarkersEnabled; }

    // === Debug Getters ===
    public boolean isDebugEnabledByDefault() { return debugEnabledByDefault; }
    public boolean isDebugLogToConsole() { return debugLogToConsole; }
    public boolean isDebugPower() { return debugPower; }
    public boolean isDebugClaim() { return debugClaim; }
    public boolean isDebugCombat() { return debugCombat; }
    public boolean isDebugProtection() { return debugProtection; }
    public boolean isDebugRelation() { return debugRelation; }
    public boolean isDebugTerritory() { return debugTerritory; }

    // === Chat Formatting Getters ===
    public boolean isChatFormattingEnabled() { return chatFormattingEnabled; }
    public String getChatFormat() { return chatFormat; }
    public String getChatTagDisplay() { return chatTagDisplay; }
    public String getChatTagFormat() { return chatTagFormat; }
    public String getChatRelationColorOwn() { return chatRelationColorOwn; }
    public String getChatRelationColorAlly() { return chatRelationColorAlly; }
    public String getChatRelationColorNeutral() { return chatRelationColorNeutral; }
    public String getChatRelationColorEnemy() { return chatRelationColorEnemy; }
    public String getChatNoFactionTag() { return chatNoFactionTag; }
    public String getChatEventPriority() { return chatEventPriority; }

    // === Permission Getters ===
    public boolean isAdminRequiresOp() { return adminRequiresOp; }
    public String getPermissionFallbackBehavior() { return permissionFallbackBehavior; }

    // === Faction Permission Methods ===

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

    // === Debug Setters (for runtime toggle) ===
    public void setDebugPower(boolean enabled) { this.debugPower = enabled; applyDebugSettings(); }
    public void setDebugClaim(boolean enabled) { this.debugClaim = enabled; applyDebugSettings(); }
    public void setDebugCombat(boolean enabled) { this.debugCombat = enabled; applyDebugSettings(); }
    public void setDebugProtection(boolean enabled) { this.debugProtection = enabled; applyDebugSettings(); }
    public void setDebugRelation(boolean enabled) { this.debugRelation = enabled; applyDebugSettings(); }
    public void setDebugTerritory(boolean enabled) { this.debugTerritory = enabled; applyDebugSettings(); }

    /**
     * Applies debug settings to the Logger.
     */
    public void applyDebugSettings() {
        Logger.setLogToConsole(debugLogToConsole);
        Logger.setDebugEnabled(Logger.DebugCategory.POWER, debugEnabledByDefault || debugPower);
        Logger.setDebugEnabled(Logger.DebugCategory.CLAIM, debugEnabledByDefault || debugClaim);
        Logger.setDebugEnabled(Logger.DebugCategory.COMBAT, debugEnabledByDefault || debugCombat);
        Logger.setDebugEnabled(Logger.DebugCategory.PROTECTION, debugEnabledByDefault || debugProtection);
        Logger.setDebugEnabled(Logger.DebugCategory.RELATION, debugEnabledByDefault || debugRelation);
        Logger.setDebugEnabled(Logger.DebugCategory.TERRITORY, debugEnabledByDefault || debugTerritory);
    }

    /**
     * Enables all debug categories at runtime.
     */
    public void enableAllDebug() {
        debugPower = true;
        debugClaim = true;
        debugCombat = true;
        debugProtection = true;
        debugRelation = true;
        debugTerritory = true;
        Logger.enableAll();
    }

    /**
     * Disables all debug categories at runtime.
     */
    public void disableAllDebug() {
        debugPower = false;
        debugClaim = false;
        debugCombat = false;
        debugProtection = false;
        debugRelation = false;
        debugTerritory = false;
        Logger.disableAll();
    }

    /**
     * Checks if a world is allowed for claiming.
     *
     * @param worldName the world name
     * @return true if allowed
     */
    public boolean isWorldAllowed(@NotNull String worldName) {
        // If whitelist is set, world must be in it
        if (!worldWhitelist.isEmpty()) {
            return worldWhitelist.contains(worldName);
        }
        // If blacklist is set, world must not be in it
        if (!worldBlacklist.isEmpty()) {
            return !worldBlacklist.contains(worldName);
        }
        // Default: all worlds allowed
        return true;
    }

    /**
     * Calculates the max claims for a faction based on their power.
     *
     * @param totalPower the faction's total power
     * @return the max claims allowed
     */
    public int calculateMaxClaims(double totalPower) {
        if (powerPerClaim <= 0) return maxClaims;
        int powerClaims = (int) Math.floor(totalPower / powerPerClaim);
        return Math.min(powerClaims, maxClaims);
    }

    // === Helper methods ===
    // These methods track missing keys to trigger config save with defaults

    private int getInt(JsonObject obj, String key, int def) {
        if (obj.has(key)) {
            return obj.get(key).getAsInt();
        }
        configNeedsSave = true;
        return def;
    }

    private double getDouble(JsonObject obj, String key, double def) {
        if (obj.has(key)) {
            return obj.get(key).getAsDouble();
        }
        configNeedsSave = true;
        return def;
    }

    private boolean getBool(JsonObject obj, String key, boolean def) {
        if (obj.has(key)) {
            return obj.get(key).getAsBoolean();
        }
        configNeedsSave = true;
        return def;
    }

    private String getString(JsonObject obj, String key, String def) {
        if (obj.has(key)) {
            return obj.get(key).getAsString();
        }
        configNeedsSave = true;
        return def;
    }

    private List<String> getStringList(JsonObject obj, String key) {
        List<String> list = new ArrayList<>();
        if (obj.has(key) && obj.get(key).isJsonArray()) {
            obj.getAsJsonArray(key).forEach(el -> list.add(el.getAsString()));
        } else if (!obj.has(key)) {
            configNeedsSave = true;
        }
        return list;
    }

    /**
     * Checks if a section exists in the config.
     * If not, marks the config as needing save.
     */
    private boolean hasSection(JsonObject root, String sectionName) {
        if (root.has(sectionName) && root.get(sectionName).isJsonObject()) {
            return true;
        }
        configNeedsSave = true;
        return false;
    }
}
