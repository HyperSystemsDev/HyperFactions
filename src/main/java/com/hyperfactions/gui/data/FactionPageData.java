package com.hyperfactions.gui.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Unified event data class for HyperFactions GUI pages.
 * Consolidates common fields used across multiple pages.
 */
public class FactionPageData {

    /** The button/action that triggered the event (e.g., "Nav", "Members", "Teleport") */
    public String button;

    /** Target identifier (e.g., page ID for navigation, faction ID, player UUID) */
    public String target;

    /** NavBar target (AdminUI pattern - page ID when nav button clicked) */
    public String navBar;

    /** Current page number for paginated lists (0-indexed) */
    public int page;

    /** Faction ID for faction-specific actions */
    public String factionId;

    /** Player UUID for player-specific actions */
    public String playerUuid;

    /** Sort mode for lists (e.g., "power", "members", "name") */
    public String sortMode;

    /** Tab selection for multi-tab pages (e.g., "allies", "enemies") */
    public String tab;

    /** Codec for serialization/deserialization */
    public static final BuilderCodec<FactionPageData> CODEC = BuilderCodec
            .builder(FactionPageData.class, FactionPageData::new)
            .addField(
                    new KeyedCodec<>("Button", Codec.STRING),
                    (data, value) -> data.button = value,
                    data -> data.button
            )
            .addField(
                    new KeyedCodec<>("Target", Codec.STRING),
                    (data, value) -> data.target = value,
                    data -> data.target
            )
            .addField(
                    new KeyedCodec<>("NavBar", Codec.STRING),
                    (data, value) -> data.navBar = value,
                    data -> data.navBar
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
            .addField(
                    new KeyedCodec<>("FactionId", Codec.STRING),
                    (data, value) -> data.factionId = value,
                    data -> data.factionId
            )
            .addField(
                    new KeyedCodec<>("PlayerUuid", Codec.STRING),
                    (data, value) -> data.playerUuid = value,
                    data -> data.playerUuid
            )
            .addField(
                    new KeyedCodec<>("SortMode", Codec.STRING),
                    (data, value) -> data.sortMode = value,
                    data -> data.sortMode
            )
            .addField(
                    new KeyedCodec<>("Tab", Codec.STRING),
                    (data, value) -> data.tab = value,
                    data -> data.tab
            )
            .build();

    public FactionPageData() {
    }

    @Override
    public String toString() {
        return "FactionPageData{" +
                "button='" + button + '\'' +
                ", target='" + target + '\'' +
                ", navBar='" + navBar + '\'' +
                ", page=" + page +
                ", factionId='" + factionId + '\'' +
                ", playerUuid='" + playerUuid + '\'' +
                ", sortMode='" + sortMode + '\'' +
                ", tab='" + tab + '\'' +
                '}';
    }
}
