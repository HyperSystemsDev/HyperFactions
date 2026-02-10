package com.hyperfactions.gui.page.admin.config;

/**
 * Tabs available in the admin config editor.
 */
public enum ConfigTabType {
    GENERAL("General"),
    POWER("Power"),
    CLAIMS("Claims"),
    COMBAT("Combat"),
    CHAT("Chat"),
    MODULES("Modules"),
    WORLDMAP("World Map"),
    PROTECTION("Protection");

    private final String displayName;

    ConfigTabType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
