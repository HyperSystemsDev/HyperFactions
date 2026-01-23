package com.hyperfactions.gui.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Event data for the Admin Zone management page.
 */
public class AdminZoneData {

    /** The button/action that triggered the event */
    public String button;

    /** Zone ID (if any) */
    public String zoneId;

    /** Zone name (if any) */
    public String zoneName;

    /** Zone type (safe, war) */
    public String zoneType;

    /** Target chunk X coordinate */
    public int chunkX;

    /** Target chunk Z coordinate */
    public int chunkZ;

    /** Current page number (for pagination) */
    public int page;

    /** Codec for serialization/deserialization */
    public static final BuilderCodec<AdminZoneData> CODEC = BuilderCodec
            .builder(AdminZoneData.class, AdminZoneData::new)
            .addField(
                    new KeyedCodec<>("Button", Codec.STRING),
                    (data, value) -> data.button = value,
                    data -> data.button
            )
            .addField(
                    new KeyedCodec<>("ZoneId", Codec.STRING),
                    (data, value) -> data.zoneId = value,
                    data -> data.zoneId
            )
            .addField(
                    new KeyedCodec<>("ZoneName", Codec.STRING),
                    (data, value) -> data.zoneName = value,
                    data -> data.zoneName
            )
            .addField(
                    new KeyedCodec<>("ZoneType", Codec.STRING),
                    (data, value) -> data.zoneType = value,
                    data -> data.zoneType
            )
            .addField(
                    new KeyedCodec<>("ChunkX", Codec.STRING),
                    (data, value) -> {
                        try {
                            data.chunkX = value != null ? Integer.parseInt(value) : 0;
                        } catch (NumberFormatException e) {
                            data.chunkX = 0;
                        }
                    },
                    data -> String.valueOf(data.chunkX)
            )
            .addField(
                    new KeyedCodec<>("ChunkZ", Codec.STRING),
                    (data, value) -> {
                        try {
                            data.chunkZ = value != null ? Integer.parseInt(value) : 0;
                        } catch (NumberFormatException e) {
                            data.chunkZ = 0;
                        }
                    },
                    data -> String.valueOf(data.chunkZ)
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

    public AdminZoneData() {
    }
}
