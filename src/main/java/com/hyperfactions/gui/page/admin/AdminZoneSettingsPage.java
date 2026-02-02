package com.hyperfactions.gui.page.admin;

import com.hyperfactions.data.Zone;
import com.hyperfactions.data.ZoneFlags;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.admin.AdminNavBarHelper;
import com.hyperfactions.gui.admin.data.AdminZoneSettingsData;
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

import java.util.UUID;

/**
 * Admin Zone Settings page - configure zone flags visually.
 * Shows all 15 zone flags with toggle buttons and default indicators in 2-column layout.
 *
 * <p>Layout:
 * <ul>
 *   <li>Left column: Combat (0-3), Building (4-5), Damage (13-14)</li>
 *   <li>Right column: Interaction (6-10), Items (11-12)</li>
 * </ul>
 */
public class AdminZoneSettingsPage extends InteractiveCustomUIPage<AdminZoneSettingsData> {

    private final PlayerRef playerRef;
    private final UUID zoneId;
    private final ZoneManager zoneManager;
    private final GuiManager guiManager;

    public AdminZoneSettingsPage(PlayerRef playerRef,
                                 UUID zoneId,
                                 ZoneManager zoneManager,
                                 GuiManager guiManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, AdminZoneSettingsData.CODEC);
        this.playerRef = playerRef;
        this.zoneId = zoneId;
        this.zoneManager = zoneManager;
        this.guiManager = guiManager;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        // Load the admin zone settings template
        cmd.append("HyperFactions/admin/admin_zone_settings.ui");

        // Setup admin nav bar
        AdminNavBarHelper.setupBar(playerRef, "zones", cmd, events);

        // Get the zone
        Zone zone = zoneManager.getZoneById(zoneId);
        if (zone == null) {
            cmd.set("#ZoneName.Text", "Zone Not Found");
            cmd.set("#FlagsContainer.Visible", false);
            return;
        }

        // Zone info header
        cmd.set("#ZoneName.Text", zone.name());
        cmd.set("#ZoneType.Text", zone.type().name());
        cmd.set("#ZoneChunks.Text", zone.getChunkCount() + " chunks");

        // Type indicator color
        String typeColor = zone.isSafeZone() ? "#2dd4bf" : "#c084fc";
        cmd.set("#ZoneType.Style.TextColor", typeColor);

        // Build flag toggles by category (matching 2-column UI layout)
        // Left column: Combat (0-3), Building (4-5), Damage (13-14)
        buildFlagCategory(cmd, events, zone, "Combat", ZoneFlags.COMBAT_FLAGS, 0);
        buildFlagCategory(cmd, events, zone, "Building", ZoneFlags.BUILDING_FLAGS, 4);
        buildFlagCategory(cmd, events, zone, "Damage", ZoneFlags.DAMAGE_FLAGS, 13);
        // Right column: Interaction (6-10), Items (11-12)
        buildFlagCategory(cmd, events, zone, "Interaction", ZoneFlags.INTERACTION_FLAGS, 6);
        buildFlagCategory(cmd, events, zone, "Items", ZoneFlags.ITEM_FLAGS, 11);

        // Reset to Defaults button
        if (!zone.getFlags().isEmpty()) {
            cmd.set("#ResetBtn.Disabled", false);
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#ResetBtn",
                    EventData.of("Button", "ResetDefaults")
                            .append("ZoneId", zoneId.toString()),
                    false
            );
        } else {
            cmd.set("#ResetBtn.Disabled", true);
        }

        // Back button
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#BackBtn",
                EventData.of("Button", "Back"),
                false
        );
    }

    private void buildFlagCategory(UICommandBuilder cmd, UIEventBuilder events,
                                    Zone zone, String categoryName, String[] flags, int startIndex) {
        for (int i = 0; i < flags.length; i++) {
            String flagName = flags[i];
            int index = startIndex + i;
            buildFlagToggle(cmd, events, index, flagName, zone);
        }
    }

    private void buildFlagToggle(UICommandBuilder cmd, UIEventBuilder events,
                                  int index, String flagName, Zone zone) {
        String idx = "#Flag" + index;

        boolean currentValue = zone.getEffectiveFlag(flagName);
        boolean isDefault = !zone.hasFlagSet(flagName);

        // Flag name (display name from ZoneFlags)
        cmd.set(idx + "Name.Text", ZoneFlags.getDisplayName(flagName));

        // Current value toggle
        cmd.set(idx + "Toggle.Text", currentValue ? "ON" : "OFF");

        // Color based on value
        String valueColor = currentValue ? "#55FF55" : "#FF5555";
        cmd.set(idx + "Toggle.Style.Default.LabelStyle.TextColor", valueColor);
        cmd.set(idx + "Toggle.Style.Hovered.LabelStyle.TextColor", valueColor);

        // Default indicator (shows "(default)" or "(custom)")
        if (isDefault) {
            cmd.set(idx + "Default.Text", "(default)");
            cmd.set(idx + "Default.Style.TextColor", "#555555");
        } else {
            cmd.set(idx + "Default.Text", "(custom)");
            cmd.set(idx + "Default.Style.TextColor", "#FFAA00");
        }

        // Toggle event
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                idx + "Toggle",
                EventData.of("Button", "ToggleFlag")
                        .append("Flag", flagName)
                        .append("ZoneId", zoneId.toString()),
                false
        );
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                AdminZoneSettingsData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null) {
            return;
        }

        // Handle admin nav bar navigation
        if (AdminNavBarHelper.handleNavEvent(data, player, ref, store, playerRef, guiManager)) {
            return;
        }

        if (data.button == null) {
            return;
        }

        switch (data.button) {
            case "ToggleFlag" -> handleToggleFlag(player, data);
            case "ResetDefaults" -> handleResetDefaults(player, data);
            case "Back" -> guiManager.openAdminZone(player, ref, store, playerRef);
        }
    }

    private void handleToggleFlag(Player player, AdminZoneSettingsData data) {
        String flagName = data.flag;
        if (flagName == null || !ZoneFlags.isValidFlag(flagName)) {
            player.sendMessage(Message.raw("[Admin] Invalid flag.").color("#FF5555"));
            sendUpdate();
            return;
        }

        Zone zone = zoneManager.getZoneById(zoneId);
        if (zone == null) {
            player.sendMessage(Message.raw("[Admin] Zone not found.").color("#FF5555"));
            sendUpdate();
            return;
        }

        // Toggle the flag
        boolean currentValue = zone.getEffectiveFlag(flagName);
        boolean newValue = !currentValue;
        ZoneManager.ZoneResult result = zoneManager.setZoneFlag(zoneId, flagName, newValue);

        if (result == ZoneManager.ZoneResult.SUCCESS) {
            player.sendMessage(Message.raw("[Admin] Set " + ZoneFlags.getDisplayName(flagName) +
                    " to " + (newValue ? "ON" : "OFF")).color("#55FF55"));
        } else {
            player.sendMessage(Message.raw("[Admin] Failed to set flag: " + result).color("#FF5555"));
        }

        rebuildPage();
    }

    private void handleResetDefaults(Player player, AdminZoneSettingsData data) {
        ZoneManager.ZoneResult result = zoneManager.clearAllZoneFlags(zoneId);

        if (result == ZoneManager.ZoneResult.SUCCESS) {
            player.sendMessage(Message.raw("[Admin] Reset all flags to defaults.").color("#55FF55"));
        } else {
            player.sendMessage(Message.raw("[Admin] Failed to reset flags: " + result).color("#FF5555"));
        }

        rebuildPage();
    }

    private void rebuildPage() {
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        Zone zone = zoneManager.getZoneById(zoneId);
        if (zone == null) {
            sendUpdate();
            return;
        }

        // Rebuild flag toggles (matching 2-column UI layout)
        // Left column: Combat (0-3), Building (4-5), Damage (13-14)
        buildFlagCategory(cmd, events, zone, "Combat", ZoneFlags.COMBAT_FLAGS, 0);
        buildFlagCategory(cmd, events, zone, "Building", ZoneFlags.BUILDING_FLAGS, 4);
        buildFlagCategory(cmd, events, zone, "Damage", ZoneFlags.DAMAGE_FLAGS, 13);
        // Right column: Interaction (6-10), Items (11-12)
        buildFlagCategory(cmd, events, zone, "Interaction", ZoneFlags.INTERACTION_FLAGS, 6);
        buildFlagCategory(cmd, events, zone, "Items", ZoneFlags.ITEM_FLAGS, 11);

        // Update reset button state
        if (!zone.getFlags().isEmpty()) {
            cmd.set("#ResetBtn.Disabled", false);
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#ResetBtn",
                    EventData.of("Button", "ResetDefaults")
                            .append("ZoneId", zoneId.toString()),
                    false
            );
        } else {
            cmd.set("#ResetBtn.Disabled", true);
        }

        sendUpdate(cmd, events, false);
    }
}
