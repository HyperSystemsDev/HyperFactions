package com.hyperfactions.config.modules;

import com.google.gson.JsonObject;
import com.hyperfactions.config.ModuleConfig;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Configuration for GravestonePlugin integration.
 * Controls faction-aware gravestone access rules per zone type.
 */
public class GravestoneConfig extends ModuleConfig {

    private boolean protectInOwnTerritory = true;
    private boolean factionMembersCanAccess = true;
    private boolean alliesCanAccess = false;
    private boolean protectInSafeZone = true;
    private boolean protectInWarZone = false;
    private boolean protectInWilderness = false;
    private boolean announceDeathLocation = true;

    /**
     * Creates a new gravestone config.
     *
     * @param filePath path to config/gravestones.json
     */
    public GravestoneConfig(@NotNull Path filePath) {
        super(filePath);
    }

    @Override
    @NotNull
    public String getModuleName() {
        return "gravestones";
    }

    @Override
    protected boolean getDefaultEnabled() {
        return true;
    }

    @Override
    protected void createDefaults() {
        enabled = true;
        protectInOwnTerritory = true;
        factionMembersCanAccess = true;
        alliesCanAccess = false;
        protectInSafeZone = true;
        protectInWarZone = false;
        protectInWilderness = false;
        announceDeathLocation = true;
    }

    @Override
    protected void loadModuleSettings(@NotNull JsonObject root) {
        protectInOwnTerritory = getBool(root, "protectInOwnTerritory", protectInOwnTerritory);
        factionMembersCanAccess = getBool(root, "factionMembersCanAccess", factionMembersCanAccess);
        alliesCanAccess = getBool(root, "alliesCanAccess", alliesCanAccess);
        protectInSafeZone = getBool(root, "protectInSafeZone", protectInSafeZone);
        protectInWarZone = getBool(root, "protectInWarZone", protectInWarZone);
        protectInWilderness = getBool(root, "protectInWilderness", protectInWilderness);
        announceDeathLocation = getBool(root, "announceDeathLocation", announceDeathLocation);
    }

    @Override
    protected void writeModuleSettings(@NotNull JsonObject root) {
        root.addProperty("protectInOwnTerritory", protectInOwnTerritory);
        root.addProperty("factionMembersCanAccess", factionMembersCanAccess);
        root.addProperty("alliesCanAccess", alliesCanAccess);
        root.addProperty("protectInSafeZone", protectInSafeZone);
        root.addProperty("protectInWarZone", protectInWarZone);
        root.addProperty("protectInWilderness", protectInWilderness);
        root.addProperty("announceDeathLocation", announceDeathLocation);
    }

    // === Getters ===

    public boolean isProtectInOwnTerritory() {
        return protectInOwnTerritory;
    }

    public boolean isFactionMembersCanAccess() {
        return factionMembersCanAccess;
    }

    public boolean isAlliesCanAccess() {
        return alliesCanAccess;
    }

    public boolean isProtectInSafeZone() {
        return protectInSafeZone;
    }

    public boolean isProtectInWarZone() {
        return protectInWarZone;
    }

    public boolean isProtectInWilderness() {
        return protectInWilderness;
    }

    public boolean isAnnounceDeathLocation() {
        return announceDeathLocation;
    }
}
