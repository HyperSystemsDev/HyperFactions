package com.hyperfactions.gui.faction.data;

import com.hyperfactions.gui.shared.data.NavAwareData;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Data for the tabbed Faction Settings page.
 * Handles tab navigation, permission toggles, and standard page events.
 */
public class FactionSettingsTabsData implements NavAwareData {

    /** The button/action that triggered the event */
    public String button;

    /** Navigation target from NavBar button */
    public String navBar;

    /** Target tab for tab switching (general, permissions, members) */
    public String tab;

    /** Permission name for toggle actions (e.g., "outsiderBreak", "pvpEnabled") */
    public String perm;

    /** New faction name (for rename action) */
    public String name;

    /** New faction description */
    public String description;

    /** Selected color hex code */
    public String color;

    /** Whether faction is open */
    public String isOpen;

    /** Codec for serialization/deserialization */
    public static final BuilderCodec<FactionSettingsTabsData> CODEC = BuilderCodec
            .builder(FactionSettingsTabsData.class, FactionSettingsTabsData::new)
            .addField(
                    new KeyedCodec<>("Button", Codec.STRING),
                    (data, value) -> data.button = value,
                    data -> data.button
            )
            .addField(
                    new KeyedCodec<>("NavBar", Codec.STRING),
                    (data, value) -> data.navBar = value,
                    data -> data.navBar
            )
            .addField(
                    new KeyedCodec<>("Tab", Codec.STRING),
                    (data, value) -> data.tab = value,
                    data -> data.tab
            )
            .addField(
                    new KeyedCodec<>("Perm", Codec.STRING),
                    (data, value) -> data.perm = value,
                    data -> data.perm
            )
            .addField(
                    new KeyedCodec<>("Name", Codec.STRING),
                    (data, value) -> data.name = value,
                    data -> data.name
            )
            .addField(
                    new KeyedCodec<>("Description", Codec.STRING),
                    (data, value) -> data.description = value,
                    data -> data.description
            )
            .addField(
                    new KeyedCodec<>("Color", Codec.STRING),
                    (data, value) -> data.color = value,
                    data -> data.color
            )
            .addField(
                    new KeyedCodec<>("IsOpen", Codec.STRING),
                    (data, value) -> data.isOpen = value,
                    data -> data.isOpen
            )
            .build();

    public FactionSettingsTabsData() {
    }

    @Override
    public String getNavBar() {
        return navBar;
    }
}
