package com.hyperfactions.gui.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Event data for the Color Picker page.
 */
public class ColorPickerData {

    /** The button/action that triggered the event */
    public String button;

    /** Selected color code (Minecraft color code letter) */
    public String colorCode;

    /** Selected color hex value */
    public String colorHex;

    /** Codec for serialization/deserialization */
    public static final BuilderCodec<ColorPickerData> CODEC = BuilderCodec
            .builder(ColorPickerData.class, ColorPickerData::new)
            .addField(
                    new KeyedCodec<>("Button", Codec.STRING),
                    (data, value) -> data.button = value,
                    data -> data.button
            )
            .addField(
                    new KeyedCodec<>("ColorCode", Codec.STRING),
                    (data, value) -> data.colorCode = value,
                    data -> data.colorCode
            )
            .addField(
                    new KeyedCodec<>("ColorHex", Codec.STRING),
                    (data, value) -> data.colorHex = value,
                    data -> data.colorHex
            )
            .build();

    public ColorPickerData() {
    }
}
