package com.hyperfactions.gui.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Data for the Faction Settings page.
 */
public class FactionSettingsData {

    /** The button/action that triggered the event */
    public String button;

    /** New faction name */
    public String name;

    /** New faction description */
    public String description;

    /** Selected color hex code */
    public String color;

    /** Whether faction is open */
    public String isOpen;

    /** Codec for serialization/deserialization */
    public static final BuilderCodec<FactionSettingsData> CODEC = BuilderCodec
            .builder(FactionSettingsData.class, FactionSettingsData::new)
            .addField(
                    new KeyedCodec<>("Button", Codec.STRING),
                    (data, value) -> data.button = value,
                    data -> data.button
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

    public FactionSettingsData() {
    }
}
