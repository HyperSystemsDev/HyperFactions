package com.hyperfactions.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

    // Auto-save settings
    private boolean autoSaveEnabled = true;
    private int autoSaveIntervalMinutes = 5;

    // Economy settings
    private boolean economyEnabled = true;
    private String economyCurrencyName = "dollar";
    private String economyCurrencyNamePlural = "dollars";
    private String economyCurrencySymbol = "$";
    private double economyStartingBalance = 0.0;

    // Message settings
    private String prefix = "\u00A7b[HyperFactions]\u00A7r ";
    private String primaryColor = "#00FFFF";

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

    /**
     * Loads the configuration from file.
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

        try {
            String json = Files.readString(configFile);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            // Faction settings
            if (root.has("faction") && root.get("faction").isJsonObject()) {
                JsonObject faction = root.getAsJsonObject("faction");
                maxMembers = getInt(faction, "maxMembers", maxMembers);
                maxNameLength = getInt(faction, "maxNameLength", maxNameLength);
                minNameLength = getInt(faction, "minNameLength", minNameLength);
                allowColors = getBool(faction, "allowColors", allowColors);
            }

            // Power settings
            if (root.has("power") && root.get("power").isJsonObject()) {
                JsonObject power = root.getAsJsonObject("power");
                maxPlayerPower = getDouble(power, "maxPlayerPower", maxPlayerPower);
                startingPower = getDouble(power, "startingPower", startingPower);
                powerPerClaim = getDouble(power, "powerPerClaim", powerPerClaim);
                deathPenalty = getDouble(power, "deathPenalty", deathPenalty);
                regenPerMinute = getDouble(power, "regenPerMinute", regenPerMinute);
                regenWhenOffline = getBool(power, "regenWhenOffline", regenWhenOffline);
            }

            // Claim settings
            if (root.has("claims") && root.get("claims").isJsonObject()) {
                JsonObject claims = root.getAsJsonObject("claims");
                maxClaims = getInt(claims, "maxClaims", maxClaims);
                onlyAdjacent = getBool(claims, "onlyAdjacent", onlyAdjacent);
                decayEnabled = getBool(claims, "decayEnabled", decayEnabled);
                decayDaysInactive = getInt(claims, "decayDaysInactive", decayDaysInactive);
                worldWhitelist = getStringList(claims, "worldWhitelist");
                worldBlacklist = getStringList(claims, "worldBlacklist");
            }

            // Combat settings
            if (root.has("combat") && root.get("combat").isJsonObject()) {
                JsonObject combat = root.getAsJsonObject("combat");
                tagDurationSeconds = getInt(combat, "tagDurationSeconds", tagDurationSeconds);
                allyDamage = getBool(combat, "allyDamage", allyDamage);
                factionDamage = getBool(combat, "factionDamage", factionDamage);
                taggedLogoutPenalty = getBool(combat, "taggedLogoutPenalty", taggedLogoutPenalty);

                // Spawn protection sub-section
                if (combat.has("spawnProtection") && combat.get("spawnProtection").isJsonObject()) {
                    JsonObject spawnProt = combat.getAsJsonObject("spawnProtection");
                    spawnProtectionEnabled = getBool(spawnProt, "enabled", spawnProtectionEnabled);
                    spawnProtectionDurationSeconds = getInt(spawnProt, "durationSeconds", spawnProtectionDurationSeconds);
                    spawnProtectionBreakOnAttack = getBool(spawnProt, "breakOnAttack", spawnProtectionBreakOnAttack);
                    spawnProtectionBreakOnMove = getBool(spawnProt, "breakOnMove", spawnProtectionBreakOnMove);
                }
            }

            // Relation settings
            if (root.has("relations") && root.get("relations").isJsonObject()) {
                JsonObject relations = root.getAsJsonObject("relations");
                maxAllies = getInt(relations, "maxAllies", maxAllies);
                maxEnemies = getInt(relations, "maxEnemies", maxEnemies);
            }

            // Invite/Request settings
            if (root.has("invites") && root.get("invites").isJsonObject()) {
                JsonObject invites = root.getAsJsonObject("invites");
                inviteExpirationMinutes = getInt(invites, "inviteExpirationMinutes", inviteExpirationMinutes);
                joinRequestExpirationHours = getInt(invites, "joinRequestExpirationHours", joinRequestExpirationHours);
            }

            // Stuck settings
            if (root.has("stuck") && root.get("stuck").isJsonObject()) {
                JsonObject stuck = root.getAsJsonObject("stuck");
                stuckWarmupSeconds = getInt(stuck, "warmupSeconds", stuckWarmupSeconds);
                stuckCooldownSeconds = getInt(stuck, "cooldownSeconds", stuckCooldownSeconds);
            }

            // Teleport settings
            if (root.has("teleport") && root.get("teleport").isJsonObject()) {
                JsonObject teleport = root.getAsJsonObject("teleport");
                warmupSeconds = getInt(teleport, "warmupSeconds", warmupSeconds);
                cooldownSeconds = getInt(teleport, "cooldownSeconds", cooldownSeconds);
                cancelOnMove = getBool(teleport, "cancelOnMove", cancelOnMove);
                cancelOnDamage = getBool(teleport, "cancelOnDamage", cancelOnDamage);
            }

            // Update settings
            if (root.has("updates") && root.get("updates").isJsonObject()) {
                JsonObject updates = root.getAsJsonObject("updates");
                updateCheckEnabled = getBool(updates, "enabled", updateCheckEnabled);
                updateCheckUrl = getString(updates, "url", updateCheckUrl);
            }

            // Auto-save settings
            if (root.has("autoSave") && root.get("autoSave").isJsonObject()) {
                JsonObject autoSave = root.getAsJsonObject("autoSave");
                autoSaveEnabled = getBool(autoSave, "enabled", autoSaveEnabled);
                autoSaveIntervalMinutes = getInt(autoSave, "intervalMinutes", autoSaveIntervalMinutes);
            }

            // Economy settings
            if (root.has("economy") && root.get("economy").isJsonObject()) {
                JsonObject economy = root.getAsJsonObject("economy");
                economyEnabled = getBool(economy, "enabled", economyEnabled);
                economyCurrencyName = getString(economy, "currencyName", economyCurrencyName);
                economyCurrencyNamePlural = getString(economy, "currencyNamePlural", economyCurrencyNamePlural);
                economyCurrencySymbol = getString(economy, "currencySymbol", economyCurrencySymbol);
                economyStartingBalance = getDouble(economy, "startingBalance", economyStartingBalance);
            }

            // Message settings
            if (root.has("messages") && root.get("messages").isJsonObject()) {
                JsonObject messages = root.getAsJsonObject("messages");
                prefix = getString(messages, "prefix", prefix);
                primaryColor = getString(messages, "primaryColor", primaryColor);
            }

            // Territory notification settings
            if (root.has("territoryNotifications") && root.get("territoryNotifications").isJsonObject()) {
                JsonObject territoryNotifications = root.getAsJsonObject("territoryNotifications");
                territoryNotificationsEnabled = getBool(territoryNotifications, "enabled", territoryNotificationsEnabled);
            }

            // World map marker settings
            if (root.has("worldMap") && root.get("worldMap").isJsonObject()) {
                JsonObject worldMap = root.getAsJsonObject("worldMap");
                worldMapMarkersEnabled = getBool(worldMap, "enabled", worldMapMarkersEnabled);
            }

            // Debug settings
            if (root.has("debug") && root.get("debug").isJsonObject()) {
                JsonObject debug = root.getAsJsonObject("debug");
                debugEnabledByDefault = getBool(debug, "enabledByDefault", debugEnabledByDefault);
                debugLogToConsole = getBool(debug, "logToConsole", debugLogToConsole);

                if (debug.has("categories") && debug.get("categories").isJsonObject()) {
                    JsonObject categories = debug.getAsJsonObject("categories");
                    debugPower = getBool(categories, "power", debugPower);
                    debugClaim = getBool(categories, "claim", debugClaim);
                    debugCombat = getBool(categories, "combat", debugCombat);
                    debugProtection = getBool(categories, "protection", debugProtection);
                    debugRelation = getBool(categories, "relation", debugRelation);
                    debugTerritory = getBool(categories, "territory", debugTerritory);
                }
            }

            // Apply debug settings to Logger
            applyDebugSettings();

            Logger.info("Configuration loaded");
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
            root.add("updates", updates);

            // Auto-save settings
            JsonObject autoSave = new JsonObject();
            autoSave.addProperty("enabled", autoSaveEnabled);
            autoSave.addProperty("intervalMinutes", autoSaveIntervalMinutes);
            root.add("autoSave", autoSave);

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

    // === Auto-save Getters ===
    public boolean isAutoSaveEnabled() { return autoSaveEnabled; }
    public int getAutoSaveIntervalMinutes() { return autoSaveIntervalMinutes; }

    // === Economy Getters ===
    public boolean isEconomyEnabled() { return economyEnabled; }
    public String getEconomyCurrencyName() { return economyCurrencyName; }
    public String getEconomyCurrencyNamePlural() { return economyCurrencyNamePlural; }
    public String getEconomyCurrencySymbol() { return economyCurrencySymbol; }
    public double getEconomyStartingBalance() { return economyStartingBalance; }

    // === Message Getters ===
    public String getPrefix() { return prefix; }
    public String getPrimaryColor() { return primaryColor; }

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
    private int getInt(JsonObject obj, String key, int def) {
        return obj.has(key) ? obj.get(key).getAsInt() : def;
    }

    private double getDouble(JsonObject obj, String key, double def) {
        return obj.has(key) ? obj.get(key).getAsDouble() : def;
    }

    private boolean getBool(JsonObject obj, String key, boolean def) {
        return obj.has(key) ? obj.get(key).getAsBoolean() : def;
    }

    private String getString(JsonObject obj, String key, String def) {
        return obj.has(key) ? obj.get(key).getAsString() : def;
    }

    private List<String> getStringList(JsonObject obj, String key) {
        List<String> list = new ArrayList<>();
        if (obj.has(key) && obj.get(key).isJsonArray()) {
            obj.getAsJsonArray(key).forEach(el -> list.add(el.getAsString()));
        }
        return list;
    }
}
