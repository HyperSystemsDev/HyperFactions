package com.hyperfactions.gui.help;

import org.jetbrains.annotations.NotNull;

/**
 * Categories for organizing help content.
 * Each category represents a module/feature area of HyperFactions.
 */
public enum HelpCategory {
    GETTING_STARTED("getting_started", "Getting Started", 0),
    FACTION_BASICS("faction_basics", "Faction Basics", 1),
    TERRITORY("territory", "Territory & Claims", 2),
    RELATIONS("relations", "Relations & Diplomacy", 3),
    COMBAT("combat", "Combat & Protection", 4),
    COMMANDS("commands", "Commands Reference", 5);

    private final String id;
    private final String displayName;
    private final int order;

    HelpCategory(@NotNull String id, @NotNull String displayName, int order) {
        this.id = id;
        this.displayName = displayName;
        this.order = order;
    }

    /**
     * Gets the unique identifier for this category.
     */
    @NotNull
    public String id() {
        return id;
    }

    /**
     * Gets the display name shown in the UI.
     */
    @NotNull
    public String displayName() {
        return displayName;
    }

    /**
     * Gets the display order (lower = higher in list).
     */
    public int order() {
        return order;
    }

    /**
     * Finds a category by its ID.
     *
     * @param id The category ID
     * @return The matching category, or GETTING_STARTED if not found
     */
    @NotNull
    public static HelpCategory fromId(@NotNull String id) {
        for (HelpCategory category : values()) {
            if (category.id.equals(id)) {
                return category;
            }
        }
        return GETTING_STARTED;
    }
}
