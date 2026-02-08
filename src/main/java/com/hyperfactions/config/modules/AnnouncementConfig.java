package com.hyperfactions.config.modules;

import com.google.gson.JsonObject;
import com.hyperfactions.config.ModuleConfig;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Configuration for the server-wide faction announcement system.
 * Controls which faction events trigger broadcasts to all online players.
 */
public class AnnouncementConfig extends ModuleConfig {

    // Per-event toggle settings
    private boolean factionCreated = true;
    private boolean factionDisbanded = true;
    private boolean leadershipTransfer = true;
    private boolean overclaim = true;
    private boolean warDeclared = true;
    private boolean allianceFormed = true;
    private boolean allianceBroken = true;

    /**
     * Creates a new announcement config.
     *
     * @param filePath path to config/announcements.json
     */
    public AnnouncementConfig(@NotNull Path filePath) {
        super(filePath);
    }

    @Override
    @NotNull
    public String getModuleName() {
        return "announcements";
    }

    @Override
    protected boolean getDefaultEnabled() {
        return true;
    }

    @Override
    protected void createDefaults() {
        enabled = true;
        factionCreated = true;
        factionDisbanded = true;
        leadershipTransfer = true;
        overclaim = true;
        warDeclared = true;
        allianceFormed = true;
        allianceBroken = true;
    }

    @Override
    protected void loadModuleSettings(@NotNull JsonObject root) {
        if (hasSection(root, "events")) {
            JsonObject events = root.getAsJsonObject("events");
            factionCreated = getBool(events, "factionCreated", factionCreated);
            factionDisbanded = getBool(events, "factionDisbanded", factionDisbanded);
            leadershipTransfer = getBool(events, "leadershipTransfer", leadershipTransfer);
            overclaim = getBool(events, "overclaim", overclaim);
            warDeclared = getBool(events, "warDeclared", warDeclared);
            allianceFormed = getBool(events, "allianceFormed", allianceFormed);
            allianceBroken = getBool(events, "allianceBroken", allianceBroken);
        }
    }

    @Override
    protected void writeModuleSettings(@NotNull JsonObject root) {
        JsonObject events = new JsonObject();
        events.addProperty("factionCreated", factionCreated);
        events.addProperty("factionDisbanded", factionDisbanded);
        events.addProperty("leadershipTransfer", leadershipTransfer);
        events.addProperty("overclaim", overclaim);
        events.addProperty("warDeclared", warDeclared);
        events.addProperty("allianceFormed", allianceFormed);
        events.addProperty("allianceBroken", allianceBroken);
        root.add("events", events);
    }

    // === Getters ===

    public boolean isFactionCreated() {
        return factionCreated;
    }

    public boolean isFactionDisbanded() {
        return factionDisbanded;
    }

    public boolean isLeadershipTransfer() {
        return leadershipTransfer;
    }

    public boolean isOverclaim() {
        return overclaim;
    }

    public boolean isWarDeclared() {
        return warDeclared;
    }

    public boolean isAllianceFormed() {
        return allianceFormed;
    }

    public boolean isAllianceBroken() {
        return allianceBroken;
    }
}
