package com.hyperfactions.gui.page.admin;

import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionPermissions;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.admin.AdminNavBarHelper;
import com.hyperfactions.gui.admin.data.AdminFactionSettingsData;
import com.hyperfactions.manager.FactionManager;
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

import java.util.List;
import java.util.UUID;

/**
 * Admin Faction Settings page - allows admins to edit any faction's settings and permissions.
 * Two-column layout: General Settings | Territory Permissions
 * Bypasses all role checks - admins have full control.
 */
public class AdminFactionSettingsPage extends InteractiveCustomUIPage<AdminFactionSettingsData> {

    // Minecraft color code to hex mapping
    private static final List<ColorInfo> COLORS = List.of(
            new ColorInfo("0", "#000000", "Black"),
            new ColorInfo("1", "#0000AA", "Dark Blue"),
            new ColorInfo("2", "#00AA00", "Dark Green"),
            new ColorInfo("3", "#00AAAA", "Dark Aqua"),
            new ColorInfo("4", "#AA0000", "Dark Red"),
            new ColorInfo("5", "#AA00AA", "Dark Purple"),
            new ColorInfo("6", "#FFAA00", "Gold"),
            new ColorInfo("7", "#AAAAAA", "Gray"),
            new ColorInfo("8", "#555555", "Dark Gray"),
            new ColorInfo("9", "#5555FF", "Blue"),
            new ColorInfo("a", "#55FF55", "Green"),
            new ColorInfo("b", "#55FFFF", "Aqua"),
            new ColorInfo("c", "#FF5555", "Red"),
            new ColorInfo("d", "#FF55FF", "Light Purple"),
            new ColorInfo("e", "#FFFF55", "Yellow"),
            new ColorInfo("f", "#FFFFFF", "White")
    );

    private final PlayerRef playerRef;
    private final UUID factionId;
    private final FactionManager factionManager;
    private final GuiManager guiManager;

    public AdminFactionSettingsPage(PlayerRef playerRef,
                                    UUID factionId,
                                    FactionManager factionManager,
                                    GuiManager guiManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, AdminFactionSettingsData.CODEC);
        this.playerRef = playerRef;
        this.factionId = factionId;
        this.factionManager = factionManager;
        this.guiManager = guiManager;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        // Load the admin faction settings template
        cmd.append("HyperFactions/admin/admin_faction_settings.ui");

        // Setup admin nav bar
        AdminNavBarHelper.setupBar(playerRef, "factions", cmd, events);

        // Get the faction
        Faction faction = factionManager.getFaction(factionId);
        if (faction == null) {
            cmd.set("#FactionName.Text", "Faction Not Found");
            return;
        }

        // Set faction name in header
        cmd.set("#FactionName.Text", faction.name());

        // === LEFT COLUMN: General Settings ===
        buildGeneralSettings(cmd, events, faction);

        // === RIGHT COLUMN: Permissions ===
        buildPermissions(cmd, events, faction);

        // Back button
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#BackBtn",
                EventData.of("Button", "Back")
                        .append("FactionId", factionId.toString()),
                false
        );
    }

    private void buildGeneralSettings(UICommandBuilder cmd, UIEventBuilder events, Faction faction) {
        // Name
        cmd.set("#NameValue.Text", faction.name());
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#NameEditBtn",
                EventData.of("Button", "OpenRenameModal"),
                false
        );

        // Tag
        String tagDisplay = faction.tag() != null && !faction.tag().isEmpty()
                ? "[" + faction.tag().toUpperCase() + "]"
                : "(None)";
        cmd.set("#TagValue.Text", tagDisplay);
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TagEditBtn",
                EventData.of("Button", "OpenTagModal"),
                false
        );

        // Description
        String desc = faction.description() != null && !faction.description().isEmpty()
                ? faction.description()
                : "(None)";
        cmd.set("#DescValue.Text", desc);
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#DescEditBtn",
                EventData.of("Button", "OpenDescriptionModal"),
                false
        );

        // Color
        String colorHex = getColorHex(faction.color());
        cmd.set("#ColorPreview.Background.Color", colorHex);
        cmd.set("#ColorValue.Text", colorHex);
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ColorBtn",
                EventData.of("Button", "OpenColorPicker"),
                false
        );

        // Recruitment
        String recruitmentStatus = faction.open() ? "Open" : "Invite Only";
        cmd.set("#RecruitmentStatus.Text", recruitmentStatus);
        cmd.set("#RecruitmentStatus.Style.TextColor", faction.open() ? "#55FF55" : "#FFAA00");
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#RecruitmentBtn",
                EventData.of("Button", "OpenRecruitmentModal"),
                false
        );

        // Home location
        if (faction.home() != null) {
            Faction.FactionHome home = faction.home();
            String worldName = home.world();
            if (worldName.contains("/")) {
                worldName = worldName.substring(worldName.lastIndexOf('/') + 1);
            }
            String homeText = String.format("%s (%.0f, %.0f, %.0f)",
                    worldName, home.x(), home.y(), home.z());
            cmd.set("#HomeLocation.Text", homeText);
        } else {
            cmd.set("#HomeLocation.Text", "Not set");
        }
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ClearHomeBtn",
                EventData.of("Button", "ClearHome"),
                false
        );

        // Disband
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#DisbandBtn",
                EventData.of("Button", "Disband"),
                false
        );
    }

    private void buildPermissions(UICommandBuilder cmd, UIEventBuilder events, Faction faction) {
        // Get effective permissions
        FactionPermissions perms = ConfigManager.get().getEffectiveFactionPermissions(
                faction.getEffectivePermissions()
        );
        ConfigManager config = ConfigManager.get();

        // === Outsider Permissions ===
        buildToggle(cmd, events, "OutsiderBreakToggle", "outsiderBreak", perms.outsiderBreak(), config);
        buildToggle(cmd, events, "OutsiderPlaceToggle", "outsiderPlace", perms.outsiderPlace(), config);
        buildToggle(cmd, events, "OutsiderInteractToggle", "outsiderInteract", perms.outsiderInteract(), config);

        // === Ally Permissions ===
        buildToggle(cmd, events, "AllyBreakToggle", "allyBreak", perms.allyBreak(), config);
        buildToggle(cmd, events, "AllyPlaceToggle", "allyPlace", perms.allyPlace(), config);
        buildToggle(cmd, events, "AllyInteractToggle", "allyInteract", perms.allyInteract(), config);

        // === Member Permissions ===
        buildToggle(cmd, events, "MemberBreakToggle", "memberBreak", perms.memberBreak(), config);
        buildToggle(cmd, events, "MemberPlaceToggle", "memberPlace", perms.memberPlace(), config);
        buildToggle(cmd, events, "MemberInteractToggle", "memberInteract", perms.memberInteract(), config);

        // === Combat ===
        buildToggle(cmd, events, "PvPToggle", "pvpEnabled", perms.pvpEnabled(), config);
        cmd.set("#PvPStatus.Text", perms.pvpEnabled() ? "Enabled" : "Disabled");
        cmd.set("#PvPStatus.Style.TextColor", perms.pvpEnabled() ? "#55FF55" : "#FF5555");

        // === Access Control ===
        buildToggle(cmd, events, "OfficersCanEditToggle", "officersCanEdit", perms.officersCanEdit(), config);
    }

    private void buildToggle(UICommandBuilder cmd, UIEventBuilder events,
                             String elementId, String permName, boolean currentValue,
                             ConfigManager config) {
        boolean locked = config.isPermissionLocked(permName);
        String selector = "#" + elementId;

        if (locked) {
            // Locked by server - show lock indicator but admin can still see value
            cmd.set(selector + ".Text", currentValue ? "ON (LOCKED)" : "OFF (LOCKED)");
            cmd.set(selector + ".Disabled", true);
            // Gray out for locked
            cmd.set(selector + ".Style.Default.LabelStyle.TextColor", "#666666");
            cmd.set(selector + ".Style.Disabled.LabelStyle.TextColor", "#666666");
        } else {
            // Admin can toggle - show current value
            cmd.set(selector + ".Text", currentValue ? "ON" : "OFF");
            cmd.set(selector + ".Disabled", false);

            // Set text color based on value
            String color = currentValue ? "#55FF55" : "#FF5555";
            cmd.set(selector + ".Style.Default.LabelStyle.TextColor", color);
            cmd.set(selector + ".Style.Hovered.LabelStyle.TextColor", color);

            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    selector,
                    EventData.of("Button", "TogglePerm")
                            .append("Perm", permName)
                            .append("FactionId", factionId.toString()),
                    false
            );
        }
    }

    private String getColorHex(String colorCode) {
        return COLORS.stream()
                .filter(c -> c.code.equals(colorCode))
                .findFirst()
                .map(c -> c.hex)
                .orElse("#FFFFFF");
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                AdminFactionSettingsData data) {
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

        Faction faction = factionManager.getFaction(factionId);
        if (faction == null && !data.button.equals("Back")) {
            player.sendMessage(Message.raw("[Admin] Faction not found.").color("#FF5555"));
            sendUpdate();
            return;
        }

        switch (data.button) {
            case "TogglePerm" -> handleTogglePerm(player, ref, store, data);

            // Admin modal pages - bypass permission checks
            case "OpenRenameModal" -> guiManager.openAdminRenameModal(player, ref, store, playerRef, faction);

            case "OpenTagModal" -> guiManager.openAdminTagModal(player, ref, store, playerRef, faction);

            case "OpenDescriptionModal" -> guiManager.openAdminDescriptionModal(player, ref, store, playerRef, faction);

            case "OpenColorPicker" -> guiManager.openAdminColorPicker(player, ref, store, playerRef, faction);

            case "OpenRecruitmentModal" -> guiManager.openAdminRecruitmentModal(player, ref, store, playerRef, faction);

            case "ClearHome" -> handleClearHome(player, ref, store, faction);

            case "Disband" -> guiManager.openAdminDisbandConfirm(player, ref, store, playerRef, faction.id(), faction.name());

            case "Back" -> guiManager.openAdminFactionInfo(player, ref, store, playerRef, factionId);
        }
    }

    private void handleTogglePerm(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                                  AdminFactionSettingsData data) {
        String permName = data.perm;
        if (permName == null) {
            sendUpdate();
            return;
        }

        ConfigManager config = ConfigManager.get();

        // Check if server has locked this setting
        if (config.isPermissionLocked(permName)) {
            player.sendMessage(Message.raw("[Admin] This setting is locked by server configuration.").color("#FF5555"));
            sendUpdate();
            return;
        }

        // Get current faction
        Faction faction = factionManager.getFaction(factionId);
        if (faction == null) {
            player.sendMessage(Message.raw("[Admin] Faction not found.").color("#FF5555"));
            sendUpdate();
            return;
        }

        // Toggle the permission
        FactionPermissions current = faction.getEffectivePermissions();
        FactionPermissions updated = current.toggle(permName);

        // Save to faction
        Faction updatedFaction = faction.withPermissions(updated);
        factionManager.updateFaction(updatedFaction);

        // Format permission name nicely
        String displayName = formatPermissionName(permName);
        boolean newValue = updated.get(permName);

        player.sendMessage(Message.raw("[Admin] Set " + displayName + " to " + (newValue ? "ON" : "OFF"))
                .color("#55FF55"));

        // Rebuild page with fresh data
        rebuildPage();
    }

    private void handleClearHome(Player player, Ref<EntityStore> ref, Store<EntityStore> store, Faction faction) {
        if (faction.home() == null) {
            player.sendMessage(Message.raw("[Admin] This faction has no home set.").color("#FFAA00"));
            sendUpdate();
            return;
        }

        Faction updatedFaction = faction.withHome(null);
        factionManager.updateFaction(updatedFaction);

        player.sendMessage(Message.raw("[Admin] Cleared faction home for " + faction.name()).color("#55FF55"));

        // Rebuild page with fresh data
        rebuildPage();
    }

    private String formatPermissionName(String permName) {
        // Convert camelCase to readable format
        return switch (permName) {
            case "outsiderBreak" -> "Outsider Break";
            case "outsiderPlace" -> "Outsider Place";
            case "outsiderInteract" -> "Outsider Interact";
            case "allyBreak" -> "Ally Break";
            case "allyPlace" -> "Ally Place";
            case "allyInteract" -> "Ally Interact";
            case "memberBreak" -> "Member Break";
            case "memberPlace" -> "Member Place";
            case "memberInteract" -> "Member Interact";
            case "pvpEnabled" -> "PvP Enabled";
            case "officersCanEdit" -> "Officers Can Edit";
            default -> permName;
        };
    }

    private void rebuildPage() {
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        // Get fresh faction data
        Faction faction = factionManager.getFaction(factionId);
        if (faction == null) {
            cmd.set("#FactionName.Text", "Faction Not Found");
            sendUpdate(cmd, events, false);
            return;
        }

        // Update header
        cmd.set("#FactionName.Text", faction.name());

        // Rebuild general settings
        rebuildGeneralSettings(cmd, events, faction);

        // Rebuild permissions
        rebuildPermissions(cmd, events, faction);

        sendUpdate(cmd, events, false);
    }

    private void rebuildGeneralSettings(UICommandBuilder cmd, UIEventBuilder events, Faction faction) {
        // Name
        cmd.set("#NameValue.Text", faction.name());

        // Tag
        String tagDisplay = faction.tag() != null && !faction.tag().isEmpty()
                ? "[" + faction.tag().toUpperCase() + "]"
                : "(None)";
        cmd.set("#TagValue.Text", tagDisplay);

        // Description
        String desc = faction.description() != null && !faction.description().isEmpty()
                ? faction.description()
                : "(None)";
        cmd.set("#DescValue.Text", desc);

        // Color
        String colorHex = getColorHex(faction.color());
        cmd.set("#ColorPreview.Background.Color", colorHex);
        cmd.set("#ColorValue.Text", colorHex);

        // Recruitment
        String recruitmentStatus = faction.open() ? "Open" : "Invite Only";
        cmd.set("#RecruitmentStatus.Text", recruitmentStatus);
        cmd.set("#RecruitmentStatus.Style.TextColor", faction.open() ? "#55FF55" : "#FFAA00");

        // Home location
        if (faction.home() != null) {
            Faction.FactionHome home = faction.home();
            String worldName = home.world();
            if (worldName.contains("/")) {
                worldName = worldName.substring(worldName.lastIndexOf('/') + 1);
            }
            String homeText = String.format("%s (%.0f, %.0f, %.0f)",
                    worldName, home.x(), home.y(), home.z());
            cmd.set("#HomeLocation.Text", homeText);
        } else {
            cmd.set("#HomeLocation.Text", "Not set");
        }
    }

    private void rebuildPermissions(UICommandBuilder cmd, UIEventBuilder events, Faction faction) {
        FactionPermissions perms = ConfigManager.get().getEffectiveFactionPermissions(
                faction.getEffectivePermissions()
        );
        ConfigManager config = ConfigManager.get();

        buildToggle(cmd, events, "OutsiderBreakToggle", "outsiderBreak", perms.outsiderBreak(), config);
        buildToggle(cmd, events, "OutsiderPlaceToggle", "outsiderPlace", perms.outsiderPlace(), config);
        buildToggle(cmd, events, "OutsiderInteractToggle", "outsiderInteract", perms.outsiderInteract(), config);

        buildToggle(cmd, events, "AllyBreakToggle", "allyBreak", perms.allyBreak(), config);
        buildToggle(cmd, events, "AllyPlaceToggle", "allyPlace", perms.allyPlace(), config);
        buildToggle(cmd, events, "AllyInteractToggle", "allyInteract", perms.allyInteract(), config);

        buildToggle(cmd, events, "MemberBreakToggle", "memberBreak", perms.memberBreak(), config);
        buildToggle(cmd, events, "MemberPlaceToggle", "memberPlace", perms.memberPlace(), config);
        buildToggle(cmd, events, "MemberInteractToggle", "memberInteract", perms.memberInteract(), config);

        buildToggle(cmd, events, "PvPToggle", "pvpEnabled", perms.pvpEnabled(), config);
        cmd.set("#PvPStatus.Text", perms.pvpEnabled() ? "Enabled" : "Disabled");
        cmd.set("#PvPStatus.Style.TextColor", perms.pvpEnabled() ? "#55FF55" : "#FF5555");

        buildToggle(cmd, events, "OfficersCanEditToggle", "officersCanEdit", perms.officersCanEdit(), config);
    }

    private record ColorInfo(String code, String hex, String name) {}
}
