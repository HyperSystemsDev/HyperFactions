package com.hyperfactions.config.modules;

import com.google.gson.JsonObject;
import com.hyperfactions.config.ModuleConfig;
import com.hyperfactions.util.Logger;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Configuration for the debug logging system.
 * <p>
 * Controls debug output by category, with integration into the Logger utility.
 */
public class DebugConfig extends ModuleConfig {

    // Global debug settings
    private boolean enabledByDefault = false;
    private boolean logToConsole = true;

    // Per-category settings
    private boolean power = false;
    private boolean claim = false;
    private boolean combat = false;
    private boolean protection = false;
    private boolean relation = false;
    private boolean territory = false;

    /**
     * Creates a new debug config.
     *
     * @param filePath path to config/debug.json
     */
    public DebugConfig(@NotNull Path filePath) {
        super(filePath);
    }

    @Override
    @NotNull
    public String getModuleName() {
        return "debug";
    }

    @Override
    protected boolean getDefaultEnabled() {
        return false; // Debug disabled by default
    }

    @Override
    protected void createDefaults() {
        enabled = false;
        enabledByDefault = false;
        logToConsole = true;
        power = false;
        claim = false;
        combat = false;
        protection = false;
        relation = false;
        territory = false;
    }

    @Override
    protected void loadModuleSettings(@NotNull JsonObject root) {
        enabledByDefault = getBool(root, "enabledByDefault", enabledByDefault);
        logToConsole = getBool(root, "logToConsole", logToConsole);

        // Load categories
        if (hasSection(root, "categories")) {
            JsonObject categories = root.getAsJsonObject("categories");
            power = getBool(categories, "power", power);
            claim = getBool(categories, "claim", claim);
            combat = getBool(categories, "combat", combat);
            protection = getBool(categories, "protection", protection);
            relation = getBool(categories, "relation", relation);
            territory = getBool(categories, "territory", territory);
        }

        // Apply settings to Logger
        applyToLogger();
    }

    @Override
    protected void writeModuleSettings(@NotNull JsonObject root) {
        root.addProperty("enabledByDefault", enabledByDefault);
        root.addProperty("logToConsole", logToConsole);

        JsonObject categories = new JsonObject();
        categories.addProperty("power", power);
        categories.addProperty("claim", claim);
        categories.addProperty("combat", combat);
        categories.addProperty("protection", protection);
        categories.addProperty("relation", relation);
        categories.addProperty("territory", territory);
        root.add("categories", categories);
    }

    /**
     * Applies the debug settings to the Logger utility.
     */
    public void applyToLogger() {
        Logger.setLogToConsole(logToConsole);
        Logger.setDebugEnabled(Logger.DebugCategory.POWER, enabledByDefault || power);
        Logger.setDebugEnabled(Logger.DebugCategory.CLAIM, enabledByDefault || claim);
        Logger.setDebugEnabled(Logger.DebugCategory.COMBAT, enabledByDefault || combat);
        Logger.setDebugEnabled(Logger.DebugCategory.PROTECTION, enabledByDefault || protection);
        Logger.setDebugEnabled(Logger.DebugCategory.RELATION, enabledByDefault || relation);
        Logger.setDebugEnabled(Logger.DebugCategory.TERRITORY, enabledByDefault || territory);
    }

    // === Getters ===

    /**
     * Checks if debug is enabled by default for all categories.
     *
     * @return true if enabled by default
     */
    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }

    /**
     * Checks if debug output should go to console.
     *
     * @return true if logging to console
     */
    public boolean isLogToConsole() {
        return logToConsole;
    }

    /**
     * Checks if power debug is enabled.
     *
     * @return true if enabled
     */
    public boolean isPower() {
        return power;
    }

    /**
     * Checks if claim debug is enabled.
     *
     * @return true if enabled
     */
    public boolean isClaim() {
        return claim;
    }

    /**
     * Checks if combat debug is enabled.
     *
     * @return true if enabled
     */
    public boolean isCombat() {
        return combat;
    }

    /**
     * Checks if protection debug is enabled.
     *
     * @return true if enabled
     */
    public boolean isProtection() {
        return protection;
    }

    /**
     * Checks if relation debug is enabled.
     *
     * @return true if enabled
     */
    public boolean isRelation() {
        return relation;
    }

    /**
     * Checks if territory debug is enabled.
     *
     * @return true if enabled
     */
    public boolean isTerritory() {
        return territory;
    }

    // === Setters (for runtime toggle) ===

    /**
     * Sets power debug state and applies to Logger.
     *
     * @param enabled true to enable
     */
    public void setPower(boolean enabled) {
        this.power = enabled;
        applyToLogger();
    }

    /**
     * Sets claim debug state and applies to Logger.
     *
     * @param enabled true to enable
     */
    public void setClaim(boolean enabled) {
        this.claim = enabled;
        applyToLogger();
    }

    /**
     * Sets combat debug state and applies to Logger.
     *
     * @param enabled true to enable
     */
    public void setCombat(boolean enabled) {
        this.combat = enabled;
        applyToLogger();
    }

    /**
     * Sets protection debug state and applies to Logger.
     *
     * @param enabled true to enable
     */
    public void setProtection(boolean enabled) {
        this.protection = enabled;
        applyToLogger();
    }

    /**
     * Sets relation debug state and applies to Logger.
     *
     * @param enabled true to enable
     */
    public void setRelation(boolean enabled) {
        this.relation = enabled;
        applyToLogger();
    }

    /**
     * Sets territory debug state and applies to Logger.
     *
     * @param enabled true to enable
     */
    public void setTerritory(boolean enabled) {
        this.territory = enabled;
        applyToLogger();
    }

    /**
     * Enables all debug categories.
     */
    public void enableAll() {
        power = true;
        claim = true;
        combat = true;
        protection = true;
        relation = true;
        territory = true;
        Logger.enableAll();
    }

    /**
     * Disables all debug categories.
     */
    public void disableAll() {
        power = false;
        claim = false;
        combat = false;
        protection = false;
        relation = false;
        territory = false;
        Logger.disableAll();
    }
}
