package com.hyperfactions.config.modules;

import com.google.gson.JsonObject;
import com.hyperfactions.config.ModuleConfig;
import com.hyperfactions.config.ValidationResult;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Configuration for the chat formatting system.
 * <p>
 * Controls how faction tags appear in chat and the colors used
 * for different faction relationships.
 */
public class ChatConfig extends ModuleConfig {

    // Format settings
    private String format = "{faction_tag}{prefix}{player}{suffix}: {message}";
    private String tagDisplay = "tag";    // "tag", "name", or "none"
    private String tagFormat = "[{tag}] ";
    private String noFactionTag = "";           // Tag for non-faction players
    private String noFactionTagColor = "#555555"; // Color for no-faction tag (dark gray)
    private String priority = "LATE";             // Event priority

    // Relation colors
    private String relationColorOwn = "#00FF00";     // Green - same faction
    private String relationColorAlly = "#FF69B4";    // Pink - allies
    private String relationColorNeutral = "#AAAAAA"; // Gray - neutral
    private String relationColorEnemy = "#FF0000";   // Red - enemies

    /**
     * Creates a new chat config.
     *
     * @param filePath path to config/chat.json
     */
    public ChatConfig(@NotNull Path filePath) {
        super(filePath);
    }

    @Override
    @NotNull
    public String getModuleName() {
        return "chat";
    }

    @Override
    protected void createDefaults() {
        enabled = true;
        format = "{faction_tag}{prefix}{player}{suffix}: {message}";
        tagDisplay = "tag";
        tagFormat = "[{tag}] ";
        noFactionTag = "";
        noFactionTagColor = "#555555";
        priority = "LATE";
        relationColorOwn = "#00FF00";
        relationColorAlly = "#FF69B4";
        relationColorNeutral = "#AAAAAA";
        relationColorEnemy = "#FF0000";
    }

    @Override
    protected void loadModuleSettings(@NotNull JsonObject root) {
        format = getString(root, "format", format);
        tagDisplay = getString(root, "tagDisplay", tagDisplay);
        tagFormat = getString(root, "tagFormat", tagFormat);
        noFactionTag = getString(root, "noFactionTag", noFactionTag);
        noFactionTagColor = getString(root, "noFactionTagColor", noFactionTagColor);
        priority = getString(root, "priority", priority);

        // Load relation colors
        if (hasSection(root, "relationColors")) {
            JsonObject colors = root.getAsJsonObject("relationColors");
            relationColorOwn = getString(colors, "own", relationColorOwn);
            relationColorAlly = getString(colors, "ally", relationColorAlly);
            relationColorNeutral = getString(colors, "neutral", relationColorNeutral);
            relationColorEnemy = getString(colors, "enemy", relationColorEnemy);
        }
    }

    @Override
    protected void writeModuleSettings(@NotNull JsonObject root) {
        root.addProperty("_formatNote", "format is only used if no permissions plugin is detected");
        root.addProperty("format", format);
        root.addProperty("tagDisplay", tagDisplay);
        root.addProperty("tagFormat", tagFormat);
        root.addProperty("noFactionTag", noFactionTag);
        root.addProperty("noFactionTagColor", noFactionTagColor);
        root.addProperty("priority", priority);

        JsonObject colors = new JsonObject();
        colors.addProperty("own", relationColorOwn);
        colors.addProperty("ally", relationColorAlly);
        colors.addProperty("neutral", relationColorNeutral);
        colors.addProperty("enemy", relationColorEnemy);
        root.add("relationColors", colors);
    }

    // === Getters ===

    /**
     * Gets the chat format template.
     *
     * @return format string with placeholders
     */
    @NotNull
    public String getFormat() {
        return format;
    }

    /**
     * Gets the tag display mode.
     *
     * @return "tag", "name", or "none"
     */
    @NotNull
    public String getTagDisplay() {
        return tagDisplay;
    }

    /**
     * Gets the tag format template.
     *
     * @return tag format string
     */
    @NotNull
    public String getTagFormat() {
        return tagFormat;
    }

    /**
     * Gets the tag shown for players without a faction.
     *
     * @return no-faction tag (may be empty)
     */
    @NotNull
    public String getNoFactionTag() {
        return noFactionTag;
    }

    @NotNull
    public String getNoFactionTagColor() {
        return noFactionTagColor;
    }

    /**
     * Gets the event priority for chat formatting.
     *
     * @return priority string (e.g., "LATE")
     */
    @NotNull
    public String getPriority() {
        return priority;
    }

    /**
     * Gets the color for same-faction members.
     *
     * @return hex color (e.g., "#00FF00")
     */
    @NotNull
    public String getRelationColorOwn() {
        return relationColorOwn;
    }

    /**
     * Gets the color for allied factions.
     *
     * @return hex color (e.g., "#FF69B4")
     */
    @NotNull
    public String getRelationColorAlly() {
        return relationColorAlly;
    }

    /**
     * Gets the color for neutral factions.
     *
     * @return hex color (e.g., "#AAAAAA")
     */
    @NotNull
    public String getRelationColorNeutral() {
        return relationColorNeutral;
    }

    /**
     * Gets the color for enemy factions.
     *
     * @return hex color (e.g., "#FF0000")
     */
    @NotNull
    public String getRelationColorEnemy() {
        return relationColorEnemy;
    }

    // === Validation ===

    @Override
    @NotNull
    public ValidationResult validate() {
        ValidationResult result = new ValidationResult();

        // Validate tagDisplay
        tagDisplay = validateEnum(result, "tagDisplay", tagDisplay,
                new String[]{"tag", "name", "none"}, "tag");

        // Validate priority
        priority = validateEnum(result, "priority", priority,
                new String[]{"EARLIEST", "EARLY", "NORMAL", "LATE", "LATEST"}, "LATE");

        // Validate hex colors (warn but don't correct)
        validateHexColor(result, "noFactionTagColor", noFactionTagColor);
        validateHexColor(result, "relationColors.own", relationColorOwn);
        validateHexColor(result, "relationColors.ally", relationColorAlly);
        validateHexColor(result, "relationColors.neutral", relationColorNeutral);
        validateHexColor(result, "relationColors.enemy", relationColorEnemy);

        return result;
    }
}
