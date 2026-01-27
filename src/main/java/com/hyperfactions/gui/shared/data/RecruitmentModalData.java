package com.hyperfactions.gui.shared.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Event data for the Recruitment Status modal.
 */
public class RecruitmentModalData {

    /** The button/action that triggered the event */
    public String button;

    /** Whether faction should be open (true/false) */
    public String isOpen;

    /** Codec for serialization/deserialization */
    public static final BuilderCodec<RecruitmentModalData> CODEC = BuilderCodec
            .builder(RecruitmentModalData.class, RecruitmentModalData::new)
            .addField(
                    new KeyedCodec<>("Button", Codec.STRING),
                    (data, value) -> data.button = value,
                    data -> data.button
            )
            .addField(
                    new KeyedCodec<>("IsOpen", Codec.STRING),
                    (data, value) -> data.isOpen = value,
                    data -> data.isOpen
            )
            .build();

    public RecruitmentModalData() {
    }
}
