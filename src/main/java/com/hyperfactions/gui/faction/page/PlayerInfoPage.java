package com.hyperfactions.gui.faction.page;

import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.PlayerPower;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.faction.data.PlayerInfoData;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.PowerManager;
import com.hyperfactions.util.TimeUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;

/**
 * Player Info page - shows detailed information about a player.
 */
public class PlayerInfoPage extends InteractiveCustomUIPage<PlayerInfoData> {

    private final PlayerRef viewerRef;
    private final UUID targetPlayerUuid;
    private final String targetPlayerName;
    private final FactionManager factionManager;
    private final PowerManager powerManager;
    private final GuiManager guiManager;

    public PlayerInfoPage(PlayerRef viewerRef,
                          UUID targetPlayerUuid,
                          String targetPlayerName,
                          FactionManager factionManager,
                          PowerManager powerManager,
                          GuiManager guiManager) {
        super(viewerRef, CustomPageLifetime.CanDismiss, PlayerInfoData.CODEC);
        this.viewerRef = viewerRef;
        this.targetPlayerUuid = targetPlayerUuid;
        this.targetPlayerName = targetPlayerName;
        this.factionManager = factionManager;
        this.powerManager = powerManager;
        this.guiManager = guiManager;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        // Load the player info template
        cmd.append("HyperFactions/faction/player_info.ui");

        // Set player name
        cmd.set("#PlayerName.Text", targetPlayerName);

        // Get faction membership
        Faction faction = factionManager.getPlayerFaction(targetPlayerUuid);
        FactionMember member = faction != null ? faction.getMember(targetPlayerUuid) : null;

        // === Basic Info Section ===
        cmd.append("#BasicInfoSection", "HyperFactions/faction/info_section.ui");
        cmd.set("#BasicInfoSection #SectionTitle.Text", "Basic Information");

        // Status (online/offline)
        boolean isOnline = store.getComponent(ref, Player.getComponentType()) != null;
        cmd.append("#BasicInfoSection #SectionContent", "HyperFactions/faction/info_row.ui");
        cmd.set("#BasicInfoSection #SectionContent #InfoLabel.Text", "Status");
        cmd.set("#BasicInfoSection #SectionContent #InfoValue.Text", isOnline ? "Online" : "Offline");
        cmd.set("#BasicInfoSection #SectionContent #InfoValue.Color", isOnline ? "#55FF55" : "#FF5555");

        // UUID
        cmd.append("#BasicInfoSection #SectionContent", "HyperFactions/faction/info_row.ui");
        cmd.set("#BasicInfoSection #SectionContent #InfoLabel.Text", "UUID");
        cmd.set("#BasicInfoSection #SectionContent #InfoValue.Text", targetPlayerUuid.toString());

        // === Faction Info Section ===
        cmd.append("#FactionInfoSection", "HyperFactions/faction/info_section.ui");
        cmd.set("#FactionInfoSection #SectionTitle.Text", "Faction");

        if (faction != null && member != null) {
            // Faction name
            cmd.append("#FactionInfoSection #SectionContent", "HyperFactions/faction/info_row.ui");
            cmd.set("#FactionInfoSection #SectionContent #InfoLabel.Text", "Faction");
            cmd.set("#FactionInfoSection #SectionContent #InfoValue.Text", faction.name());
            cmd.set("#FactionInfoSection #SectionContent #InfoValue.Color", faction.color());

            // Role
            cmd.append("#FactionInfoSection #SectionContent", "HyperFactions/faction/info_row.ui");
            cmd.set("#FactionInfoSection #SectionContent #InfoLabel.Text", "Role");
            cmd.set("#FactionInfoSection #SectionContent #InfoValue.Text", member.role().name());

            // Joined date
            cmd.append("#FactionInfoSection #SectionContent", "HyperFactions/faction/info_row.ui");
            cmd.set("#FactionInfoSection #SectionContent #InfoLabel.Text", "Joined");
            cmd.set("#FactionInfoSection #SectionContent #InfoValue.Text",
                    TimeUtil.formatRelative(member.joinedAt()));

            // Last online
            cmd.append("#FactionInfoSection #SectionContent", "HyperFactions/faction/info_row.ui");
            cmd.set("#FactionInfoSection #SectionContent #InfoLabel.Text", "Last Online");
            cmd.set("#FactionInfoSection #SectionContent #InfoValue.Text",
                    TimeUtil.formatRelative(member.lastOnline()));

            // View faction button
            cmd.append("#FactionInfoSection #SectionContent", "HyperFactions/faction/action_button.ui");
            cmd.set("#FactionInfoSection #SectionContent #ActionBtn.Text", "View Faction");
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#FactionInfoSection #SectionContent #ActionBtn",
                    EventData.of("Button", "ViewFaction")
                            .append("FactionId", faction.id().toString()),
                    false
            );
        } else {
            // No faction
            cmd.append("#FactionInfoSection #SectionContent", "HyperFactions/faction/info_row.ui");
            cmd.set("#FactionInfoSection #SectionContent #InfoLabel.Text", "Faction");
            cmd.set("#FactionInfoSection #SectionContent #InfoValue.Text", "(None)");
            cmd.set("#FactionInfoSection #SectionContent #InfoValue.Color", "#AAAAAA");
        }

        // === Power Section ===
        cmd.append("#PowerSection", "HyperFactions/faction/info_section.ui");
        cmd.set("#PowerSection #SectionTitle.Text", "Power");

        PlayerPower power = powerManager.getPlayerPower(targetPlayerUuid);
        if (power != null) {
            // Current power
            cmd.append("#PowerSection #SectionContent", "HyperFactions/faction/info_row.ui");
            cmd.set("#PowerSection #SectionContent #InfoLabel.Text", "Current Power");
            cmd.set("#PowerSection #SectionContent #InfoValue.Text",
                    String.format("%.1f", power.power()));

            // Max power
            cmd.append("#PowerSection #SectionContent", "HyperFactions/faction/info_row.ui");
            cmd.set("#PowerSection #SectionContent #InfoLabel.Text", "Max Power");
            cmd.set("#PowerSection #SectionContent #InfoValue.Text",
                    String.format("%.1f", power.maxPower()));

            // Power percentage (visual bar)
            double percentage = (power.power() / power.maxPower()) * 100.0;
            cmd.append("#PowerSection #SectionContent", "HyperFactions/faction/progress_bar.ui");
            cmd.set("#PowerSection #SectionContent #ProgressBar.Value", String.format("%.0f", percentage));
            cmd.set("#PowerSection #SectionContent #ProgressLabel.Text",
                    String.format("%.0f%%", percentage));
        } else {
            cmd.append("#PowerSection #SectionContent", "HyperFactions/faction/info_row.ui");
            cmd.set("#PowerSection #SectionContent #InfoLabel.Text", "Power");
            cmd.set("#PowerSection #SectionContent #InfoValue.Text", "(No data)");
        }

        // Back button
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#BackBtn",
                EventData.of("Button", "Back"),
                false
        );
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                PlayerInfoData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null || data.button == null) {
            return;
        }

        switch (data.button) {
            case "ViewFaction" -> {
                if (data.playerUuid != null) {
                    try {
                        UUID factionId = UUID.fromString(data.playerUuid);
                        Faction faction = factionManager.getFaction(factionId);
                        if (faction != null) {
                            // Navigate to faction info page (would need to create this)
                            player.sendMessage(
                                    Message.raw("Use ").color("#AAAAAA")
                                            .insert(Message.raw("/f info " + faction.name()).color("#55FF55"))
                                            .insert(Message.raw(" to view faction details.").color("#AAAAAA"))
                            );
                            guiManager.closePage(player, ref, store);
                        }
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(Message.raw("Invalid faction ID.").color("#FF5555"));
                    }
                }
            }

            case "Back" -> guiManager.closePage(player, ref, store);
        }
    }
}
