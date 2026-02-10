package com.hyperfactions.gui.admin.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.jetbrains.annotations.Nullable;

/**
 * Event data for the Admin Config page.
 */
public class AdminConfigData implements AdminNavAwareData {

    /** The button/action that triggered the event */
    public String button;

    /** Admin nav bar target (for navigation) */
    public String adminNavBar;

    /** Tab to switch to */
    public String tab;

    /** Config key being modified */
    public String key;

    /** Value from UI controls */
    public String value;

    /** List item identifier (for array add/remove) */
    public String listItem;

    /** Role selector (for permissions matrix) */
    public String role;

    /** Codec for serialization/deserialization */
    public static final BuilderCodec<AdminConfigData> CODEC = BuilderCodec
            .builder(AdminConfigData.class, AdminConfigData::new)
            .addField(
                    new KeyedCodec<>("Button", Codec.STRING),
                    (data, v) -> data.button = v,
                    data -> data.button
            )
            .addField(
                    new KeyedCodec<>("AdminNavBar", Codec.STRING),
                    (data, v) -> data.adminNavBar = v,
                    data -> data.adminNavBar
            )
            .addField(
                    new KeyedCodec<>("Tab", Codec.STRING),
                    (data, v) -> data.tab = v,
                    data -> data.tab
            )
            .addField(
                    new KeyedCodec<>("Key", Codec.STRING),
                    (data, v) -> data.key = v,
                    data -> data.key
            )
            .addField(
                    new KeyedCodec<>("Value", Codec.STRING),
                    (data, v) -> data.value = v,
                    data -> data.value
            )
            .addField(
                    new KeyedCodec<>("ListItem", Codec.STRING),
                    (data, v) -> data.listItem = v,
                    data -> data.listItem
            )
            .addField(
                    new KeyedCodec<>("Role", Codec.STRING),
                    (data, v) -> data.role = v,
                    data -> data.role
            )
            .build();

    public AdminConfigData() {
    }

    @Override
    @Nullable
    public String getAdminNavBar() {
        return adminNavBar;
    }
}
