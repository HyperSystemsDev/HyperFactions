package com.hyperfactions.gui.faction.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Event data for the Faction Members page.
 */
public class FactionMembersData {

    /** The button/action that triggered the event */
    public String button;

    /** Target member UUID (if any) */
    public String memberUuid;

    /** Target member name (if any) */
    public String memberName;

    /** Current page number (for pagination) */
    public int page;

    /** Codec for serialization/deserialization */
    public static final BuilderCodec<FactionMembersData> CODEC = BuilderCodec
            .builder(FactionMembersData.class, FactionMembersData::new)
            .addField(
                    new KeyedCodec<>("Button", Codec.STRING),
                    (data, value) -> data.button = value,
                    data -> data.button
            )
            .addField(
                    new KeyedCodec<>("MemberUuid", Codec.STRING),
                    (data, value) -> data.memberUuid = value,
                    data -> data.memberUuid
            )
            .addField(
                    new KeyedCodec<>("MemberName", Codec.STRING),
                    (data, value) -> data.memberName = value,
                    data -> data.memberName
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

    public FactionMembersData() {
    }
}
