package com.hyperfactions.gui.page.admin;

import com.hyperfactions.data.*;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.data.AdminZoneData;
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

    private String currentTab = "safe"; // safe, war
    private int currentPage = 0;

    public AdminZonePage(PlayerRef playerRef,
                         ZoneManager zoneManager,
                         GuiManager guiManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, AdminZoneData.CODEC);
        this.playerRef = playerRef;
        this.zoneManager = zoneManager;
        this.guiManager = guiManager;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        // Load the main template
        cmd.append("HyperFactions/admin_zones.ui");

        // Set title
        cmd.set("#Title #TitleText.Text", "Zone Management");

        // Tab buttons
        cmd.set("#TabSafe.Style.TextColor", currentTab.equals("safe") ? "#00FFFF" : "#888888");
        cmd.set("#TabWar.Style.TextColor", currentTab.equals("war") ? "#FF00FF" : "#888888");

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TabSafe",
                EventData.of("Button", "TabSafe").append("Tab", "safe"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TabWar",
                EventData.of("Button", "TabWar").append("Tab", "war"),
                false
        );

        // Get zones of current type
        ZoneType targetType = currentTab.equals("safe") ? ZoneType.SAFE : ZoneType.WAR;
        List<Zone> zones = new ArrayList<>(zoneManager.getAllZones().stream()
                .filter(z -> z.type() == targetType)
                .sorted(Comparator.comparing(Zone::name))
                .toList());

        // Zone count
        cmd.set("#ZoneCount.Text", zones.size() + " " + currentTab + " zones");

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

                cmd.append(entryId, "HyperFactions/admin_zone_entry.ui");

                String prefix = entryId + " ";

                // Zone info
                String typeColor = zone.type() == ZoneType.SAFE ? "#00FFFF" : "#FF00FF";
                cmd.set(prefix + "#ZoneName.Text", zone.name());
                cmd.set(prefix + "#ZoneName.Style.TextColor", typeColor);
                cmd.set(prefix + "#ZoneType.Text", zone.type().name());
                cmd.set(prefix + "#ZoneType.Style.TextColor", typeColor);
                cmd.set(prefix + "#ZoneLocation.Text", String.format(
                        "%s (%d, %d)",
                        zone.world(), zone.chunkX(), zone.chunkZ()
                ));

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

            case "TabSafe", "TabWar" -> {
                currentTab = data.zoneType != null ? data.zoneType : "safe";
                currentPage = 0;
                guiManager.openAdminZone(player, ref, store, playerRef);
            }

            case "PrevPage" -> {
                currentPage = Math.max(0, data.page);
                guiManager.openAdminZone(player, ref, store, playerRef);
            }

            case "NextPage" -> {
                currentPage = data.page;
                guiManager.openAdminZone(player, ref, store, playerRef);
            }

            case "CreateZone" -> {
                guiManager.closePage(player, ref, store);
                ZoneType type = data.zoneType != null && data.zoneType.equals("war") ? ZoneType.WAR : ZoneType.SAFE;
                player.sendMessage(Message.raw("Use /f admin zone create <name> " + type.name().toLowerCase() +
                        " to create a zone at your current location.").color("#00FFFF"));
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
                        guiManager.openAdminZone(player, ref, store, playerRef);
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(Message.raw("Invalid zone ID.").color("#FF5555"));
                    }
                }
            }
        }
    }
}
