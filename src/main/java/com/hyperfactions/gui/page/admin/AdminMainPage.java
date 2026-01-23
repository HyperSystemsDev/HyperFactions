package com.hyperfactions.gui.page.admin;

import com.hyperfactions.data.*;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.data.AdminMainData;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.PowerManager;
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

import java.util.*;

/**
 * Admin Main page - provides admin controls for faction management.
 */
public class AdminMainPage extends InteractiveCustomUIPage<AdminMainData> {

    private static final int FACTIONS_PER_PAGE = 6;

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final PowerManager powerManager;
    private final GuiManager guiManager;

    private int currentPage = 0;

    public AdminMainPage(PlayerRef playerRef,
                         FactionManager factionManager,
                         PowerManager powerManager,
                         GuiManager guiManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, AdminMainData.CODEC);
        this.playerRef = playerRef;
        this.factionManager = factionManager;
        this.powerManager = powerManager;
        this.guiManager = guiManager;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        // Load the main template
        cmd.append("HyperFactions/admin_main.ui");


        // Stats overview
        Collection<Faction> allFactions = factionManager.getAllFactions();
        int totalFactions = allFactions.size();
        int totalMembers = allFactions.stream()
                .mapToInt(f -> f.members().size())
                .sum();
        int totalClaims = allFactions.stream()
                .mapToInt(f -> f.claims().size())
                .sum();

        cmd.set("#TotalFactions.Text", "Factions: " + totalFactions);
        cmd.set("#TotalMembers.Text", "Total Members: " + totalMembers);
        cmd.set("#TotalClaims.Text", "Total Claims: " + totalClaims);

        // Navigation buttons
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ZonesBtn",
                EventData.of("Button", "Zones"),
                false
        );

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ReloadBtn",
                EventData.of("Button", "Reload"),
                false
        );

        // Get all factions sorted by power
        List<Faction> factions = new ArrayList<>(allFactions);
        factions.sort((a, b) -> {
            double powerA = powerManager.getFactionPowerStats(a.id()).currentPower();
            double powerB = powerManager.getFactionPowerStats(b.id()).currentPower();
            return Double.compare(powerB, powerA);
        });

        // Calculate pagination
        int totalPages = Math.max(1, (int) Math.ceil((double) factions.size() / FACTIONS_PER_PAGE));
        currentPage = Math.min(currentPage, totalPages - 1);
        int startIdx = currentPage * FACTIONS_PER_PAGE;

        // Build faction entries
        for (int i = 0; i < FACTIONS_PER_PAGE; i++) {
            String entryId = "#FactionEntry" + i;
            int factionIdx = startIdx + i;

            if (factionIdx < factions.size()) {
                Faction faction = factions.get(factionIdx);
                PowerManager.FactionPowerStats stats = powerManager.getFactionPowerStats(faction.id());

                cmd.append(entryId, "HyperFactions/admin_faction_entry.ui");

                String prefix = entryId + " ";

                // Faction info
                String colorHex = faction.color() != null ? faction.color() : "#00FFFF";
                cmd.set(prefix + "#FactionName.Text", faction.name());
                cmd.set(prefix + "#MemberCount.Text", faction.members().size() + " members");
                cmd.set(prefix + "#PowerCount.Text", stats.currentPower() + "/" + stats.maxPower() + " power");
                cmd.set(prefix + "#ClaimCount.Text", faction.claims().size() + " claims");

                // Leader info
                FactionMember leader = faction.getLeader();
                String leaderName = leader != null ? leader.username() : "None";
                cmd.set(prefix + "#LeaderName.Text", "Leader: " + leaderName);

                // Action buttons
                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        prefix + "#DisbandBtn",
                        EventData.of("Button", "Disband")
                                .append("FactionId", faction.id().toString())
                                .append("FactionName", faction.name()),
                        false
                );

                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        prefix + "#UnclaimAllBtn",
                        EventData.of("Button", "UnclaimAll")
                                .append("FactionId", faction.id().toString())
                                .append("FactionName", faction.name()),
                        false
                );
            }
        }

        // Pagination
        cmd.set("#PageInfo.Text", (currentPage + 1) + "/" + totalPages);

        if (currentPage > 0) {
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#PrevBtn",
                    EventData.of("Button", "PrevPage")
                            .append("Page", String.valueOf(currentPage - 1)),
                    false
            );
        }

        if (currentPage < totalPages - 1) {
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#NextBtn",
                    EventData.of("Button", "NextPage")
                            .append("Page", String.valueOf(currentPage + 1)),
                    false
            );
        }
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                AdminMainData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null || data.button == null) {
            return;
        }

        switch (data.button) {
            case "Zones" -> guiManager.openAdminZone(player, ref, store, playerRef);

            case "Reload" -> {
                guiManager.closePage(player, ref, store);
                player.sendMessage(Message.raw("Use /f admin reload to reload configuration.").color("#00FFFF"));
            }

            case "PrevPage" -> {
                currentPage = Math.max(0, data.page);
                guiManager.openAdminMain(player, ref, store, playerRef);
            }

            case "NextPage" -> {
                currentPage = data.page;
                guiManager.openAdminMain(player, ref, store, playerRef);
            }

            case "Disband" -> {
                if (data.factionId != null) {
                    try {
                        UUID factionId = UUID.fromString(data.factionId);
                        UUID adminUuid = playerRef.getUuid();
                        // Admin bypass - get the leader's UUID to disband
                        Faction faction = factionManager.getFaction(factionId);
                        if (faction != null) {
                            UUID leaderId = faction.getLeaderId();
                            if (leaderId != null) {
                                FactionManager.FactionResult result = factionManager.disbandFaction(factionId, leaderId);
                                if (result == FactionManager.FactionResult.SUCCESS) {
                                    player.sendMessage(Message.raw("Faction " + data.factionName + " disbanded.").color("#FF5555"));
                                } else {
                                    player.sendMessage(Message.raw("Failed to disband: " + result).color("#FF5555"));
                                }
                            } else {
                                player.sendMessage(Message.raw("Faction has no leader, cannot disband.").color("#FF5555"));
                            }
                        }
                        guiManager.openAdminMain(player, ref, store, playerRef);
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(Message.raw("Invalid faction.").color("#FF5555"));
                    }
                }
            }

            case "UnclaimAll" -> {
                if (data.factionId != null) {
                    try {
                        UUID factionId = UUID.fromString(data.factionId);
                        Faction faction = factionManager.getFaction(factionId);
                        if (faction != null) {
                            int claimCount = faction.claims().size();
                            // Admin unclaim - prompt for command
                            guiManager.closePage(player, ref, store);
                            player.sendMessage(Message.raw("Use /f admin unclaim " + data.factionName + " to unclaim all " + claimCount + " chunks.").color("#FFAA00"));
                        }
                        guiManager.openAdminMain(player, ref, store, playerRef);
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(Message.raw("Invalid faction.").color("#FF5555"));
                    }
                }
            }
        }
    }
}
