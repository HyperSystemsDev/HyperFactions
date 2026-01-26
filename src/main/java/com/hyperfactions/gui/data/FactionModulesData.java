package com.hyperfactions.gui.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Event data for the Faction Modules page.
 */
public class FactionModulesData {

    /** The button/action that triggered the event */
    public String button;

    /** NavBar target (page ID when nav button clicked) */
    public String navBar;

    /** Module ID (if any) */
    public String moduleId;

    /** Codec for serialization/deserialization */
    public static final BuilderCodec<FactionModulesData> CODEC = BuilderCodec
            .builder(FactionModulesData.class, FactionModulesData::new)
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
                    new KeyedCodec<>("ModuleId", Codec.STRING),
                    (data, value) -> data.moduleId = value,
                    data -> data.moduleId
            )
            .build();

    public FactionModulesData() {
    }
}
