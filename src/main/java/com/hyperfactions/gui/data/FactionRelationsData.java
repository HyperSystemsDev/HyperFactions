package com.hyperfactions.gui.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Event data for the Faction Relations page.
 */
public class FactionRelationsData {

    /** The button/action that triggered the event */
    public String button;

    /** Target faction ID (if any) */
    public String factionId;

    /** Target faction name (if any) */
    public String factionName;

    /** Current tab (allies, enemies, requests) */
    public String tab;

    /** Current page number (for pagination) */
    public int page;

    /** Codec for serialization/deserialization */
    public static final BuilderCodec<FactionRelationsData> CODEC = BuilderCodec
            .builder(FactionRelationsData.class, FactionRelationsData::new)
            .addField(
                    new KeyedCodec<>("Button", Codec.STRING),
                    (data, value) -> data.button = value,
                    data -> data.button
            )
            .addField(
                    new KeyedCodec<>("FactionId", Codec.STRING),
                    (data, value) -> data.factionId = value,
                    data -> data.factionId
            )
            .addField(
                    new KeyedCodec<>("FactionName", Codec.STRING),
                    (data, value) -> data.factionName = value,
                    data -> data.factionName
            )
            .addField(
                    new KeyedCodec<>("Tab", Codec.STRING),
                    (data, value) -> data.tab = value,
                    data -> data.tab
            )
            .addField(
                    new KeyedCodec<>("Page", Codec.STRING),
                    (data, value) -> {
                        try {
                            data.page = value != null ? Integer.parseInt(value) : 0;
                        } catch (NumberFormatException e) {
                            data.page = 0;
                        }
                    },
                    data -> String.valueOf(data.page)
            )
            .build();

    public FactionRelationsData() {
    }
}
