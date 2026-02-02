package com.hyperfactions.gui.admin.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.jetbrains.annotations.Nullable;

/**
 * Event data for the Admin Faction Members page.
 */
public class AdminFactionMembersData implements AdminNavAwareData {

    /** The button/action that triggered the event */
    public String button;

    /** Target faction ID (for navigation) */
    public String factionId;

    /** Target member UUID (for viewing info) */
    public String memberUuid;

    /** Target member name (for display in messages) */
    public String memberName;

    /** Admin nav bar target (for navigation) */
    public String adminNavBar;

    /** Codec for serialization/deserialization */
    public static final BuilderCodec<AdminFactionMembersData> CODEC = BuilderCodec
            .builder(AdminFactionMembersData.class, AdminFactionMembersData::new)
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
                    new KeyedCodec<>("AdminNavBar", Codec.STRING),
                    (data, value) -> data.adminNavBar = value,
                    data -> data.adminNavBar
            )
            .build();

    public AdminFactionMembersData() {
    }

    @Override
    @Nullable
    public String getAdminNavBar() {
        return adminNavBar;
    }
}
