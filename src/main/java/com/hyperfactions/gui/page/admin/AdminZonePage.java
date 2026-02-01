package com.hyperfactions.gui.page.admin;

import com.hyperfactions.data.*;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.admin.data.AdminZoneData;
import com.hyperfactions.manager.ZoneManager;
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
 * Admin Zone page - manage safe zones and war zones.
 */
public class AdminZonePage extends InteractiveCustomUIPage<AdminZoneData> {

    private static final int ZONES_PER_PAGE = 8;

    private final PlayerRef playerRef;
    private final ZoneManager zoneManager;
    private final GuiManager guiManager;

    private String currentTab = "all"; // all, safe, war
    private int currentPage = 0;

    public AdminZonePage(PlayerRef playerRef,
                         ZoneManager zoneManager,
                         GuiManager guiManager) {
        this(playerRef, zoneManager, guiManager, "all", 0);
    }

    public AdminZonePage(PlayerRef playerRef,
                         ZoneManager zoneManager,
                         GuiManager guiManager,
                         String tab,
                         int page) {
        super(playerRef, CustomPageLifetime.CanDismiss, AdminZoneData.CODEC);
        this.playerRef = playerRef;
        this.zoneManager = zoneManager;
        this.guiManager = guiManager;
        this.currentTab = tab != null ? tab : "all";
        this.currentPage = page;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {
        // Load the main template
        cmd.append("HyperFactions/admin/admin_zones.ui");


        // Tab buttons
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TabAll",
                EventData.of("Button", "TabAll").append("ZoneType", "all"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TabSafe",
                EventData.of("Button", "TabSafe").append("ZoneType", "safe"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TabWar",
                EventData.of("Button", "TabWar").append("ZoneType", "war"),
                false
        );

        // Update tab text to show which is active
        cmd.set("#TabAll.Text", currentTab.equals("all") ? "[ALL]" : "ALL");
        cmd.set("#TabSafe.Text", currentTab.equals("safe") ? "[SAFE]" : "SAFE");
        cmd.set("#TabWar.Text", currentTab.equals("war") ? "[WAR]" : "WAR");

        // Get zones based on current tab filter
        List<Zone> zones;
        if (currentTab.equals("all")) {
            zones = new ArrayList<>(zoneManager.getAllZones().stream()
                    .sorted(Comparator.comparing(Zone::name))
                    .toList());
        } else {
            ZoneType targetType = currentTab.equals("safe") ? ZoneType.SAFE : ZoneType.WAR;
            zones = new ArrayList<>(zoneManager.getAllZones().stream()
                    .filter(z -> z.type() == targetType)
                    .sorted(Comparator.comparing(Zone::name))
                    .toList());
        }

        // Zone count (with total chunks)
        int totalChunks = zones.stream().mapToInt(Zone::getChunkCount).sum();
        String tabLabel = currentTab.equals("all") ? "" : currentTab + " ";
        cmd.set("#ZoneCount.Text", zones.size() + " " + tabLabel + "zones (" + totalChunks + " chunks)");

        // Create zone button
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#CreateZoneBtn",
                EventData.of("Button", "CreateZone")
                        .append("ZoneType", currentTab),
                false
        );

        // Calculate pagination
        int totalPages = Math.max(1, (int) Math.ceil((double) zones.size() / ZONES_PER_PAGE));
        currentPage = Math.min(currentPage, totalPages - 1);
        int startIdx = currentPage * ZONES_PER_PAGE;

        // Build zone entries
        for (int i = 0; i < ZONES_PER_PAGE; i++) {
            String entryId = "#ZoneEntry" + i;
            int zoneIdx = startIdx + i;

            if (zoneIdx < zones.size()) {
                Zone zone = zones.get(zoneIdx);

                cmd.append(entryId, "HyperFactions/admin/admin_zone_entry.ui");

                String prefix = entryId + " ";

                // Zone info
                cmd.set(prefix + "#ZoneName.Text", zone.name());
                cmd.set(prefix + "#ZoneType.Text", zone.type().name() + " (" + zone.getChunkCount() + " chunks)");
                cmd.set(prefix + "#ZoneLocation.Text", zone.world());

                // Edit Map button
                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        prefix + "#EditMapBtn",
                        EventData.of("Button", "EditMap")
                                .append("ZoneId", zone.id().toString())
                                .append("ZoneName", zone.name()),
                        false
                );

                // Delete button
                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        prefix + "#DeleteBtn",
                        EventData.of("Button", "DeleteZone")
                                .append("ZoneId", zone.id().toString())
                                .append("ZoneName", zone.name()),
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
                                AdminZoneData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null || data.button == null) {
            return;
        }

        switch (data.button) {
            case "Back" -> guiManager.openAdminMain(player, ref, store, playerRef);

            case "TabAll", "TabSafe", "TabWar" -> {
                String newTab = data.zoneType != null ? data.zoneType : "all";
                guiManager.openAdminZone(player, ref, store, playerRef, newTab, 0);
            }

            case "PrevPage" -> {
                int newPage = Math.max(0, data.page);
                guiManager.openAdminZone(player, ref, store, playerRef, currentTab, newPage);
            }

            case "NextPage" -> {
                int newPage = data.page;
                guiManager.openAdminZone(player, ref, store, playerRef, currentTab, newPage);
            }

            case "CreateZone" -> {
                ZoneType type = data.zoneType != null && data.zoneType.equals("war") ? ZoneType.WAR : ZoneType.SAFE;
                guiManager.openCreateZoneWizard(player, ref, store, playerRef, type);
            }

            case "EditMap" -> {
                if (data.zoneId != null) {
                    try {
                        UUID zoneId = UUID.fromString(data.zoneId);
                        Zone zone = zoneManager.getZoneById(zoneId);
                        if (zone != null) {
                            guiManager.openAdminZoneMap(player, ref, store, playerRef, zone);
                        } else {
                            player.sendMessage(Message.raw("Zone not found.").color("#FF5555"));
                            guiManager.openAdminZone(player, ref, store, playerRef, currentTab, currentPage);
                        }
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(Message.raw("Invalid zone ID.").color("#FF5555"));
                    }
                }
            }

            case "DeleteZone" -> {
                if (data.zoneId != null) {
                    try {
                        UUID zoneId = UUID.fromString(data.zoneId);
                        ZoneManager.ZoneResult result = zoneManager.removeZone(zoneId);
                        if (result == ZoneManager.ZoneResult.SUCCESS) {
                            player.sendMessage(Message.raw("Zone " + data.zoneName + " deleted.").color("#FF5555"));
                        } else {
                            player.sendMessage(Message.raw("Failed to delete zone: " + result).color("#FF5555"));
                        }
                        // Refresh to show updated zone list
                        guiManager.openAdminZone(player, ref, store, playerRef, currentTab, currentPage);
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(Message.raw("Invalid zone ID.").color("#FF5555"));
                    }
                }
            }
        }
    }
}
