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
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
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

        // === RIGHT COLUMN ===
        buildColorSection(cmd, events, faction);
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

        // Recruitment dropdown
        cmd.set("#RecruitmentDropdown.Entries", List.of(
                new DropdownEntryInfo(LocalizableString.fromString("Open"), "OPEN"),
                new DropdownEntryInfo(LocalizableString.fromString("Invite Only"), "INVITE_ONLY")
        ));
        cmd.set("#RecruitmentDropdown.Value", faction.open() ? "OPEN" : "INVITE_ONLY");
        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#RecruitmentDropdown",
                EventData.of("Button", "RecruitmentChanged")
                        .append("@Recruitment", "#RecruitmentDropdown.Value"),
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

    private void buildColorSection(UICommandBuilder cmd, UIEventBuilder events, Faction faction) {
        String colorHex = faction.color();
        cmd.set("#ColorPreview.Background.Color", colorHex);
        cmd.set("#ColorValue.Text", colorHex);
        cmd.set("#FactionColorPicker.Value", colorHex);
        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#FactionColorPicker",
                EventData.of("Button", "ColorChanged")
                        .append("@Color", "#FactionColorPicker.Value"),
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

        // Set checkbox value via child selector
        cmd.set(selector + " #CheckBox.Value", currentValue);

        if (locked) {
            // Locked by server - disable checkbox
            cmd.set(selector + " #CheckBox.Disabled", true);
        } else {
            // Admin can toggle - bind ValueChanged event
            events.addEventBinding(
                    CustomUIEventBindingType.ValueChanged,
                    selector + " #CheckBox",
                    EventData.of("Button", "TogglePerm")
                            .append("Perm", permName)
                            .append("FactionId", factionId.toString()),
                    false
            );
        }
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

            case "ColorChanged" -> handleColorChanged(player, ref, store, data, faction);

            case "RecruitmentChanged" -> handleRecruitmentChanged(player, ref, store, data, faction);

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

    private void handleColorChanged(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                                     AdminFactionSettingsData data, Faction faction) {
        String rawColor = data.color;
        if (rawColor == null || rawColor.isEmpty()) {
            sendUpdate();
            return;
        }

        // ColorPicker returns #RRGGBBAA — strip alpha to get #RRGGBB
        String hexColor = rawColor.length() >= 7 ? rawColor.substring(0, 7).toUpperCase() : rawColor.toUpperCase();

        // Validate hex format
        if (!hexColor.matches("#[0-9A-F]{6}")) {
            sendUpdate();
            return;
        }

        Faction updatedFaction = faction.withColor(hexColor);
        factionManager.updateFaction(updatedFaction);

        player.sendMessage(Message.raw("[Admin] Set faction color to " + hexColor).color("#55FF55"));

        rebuildPage();
    }

    private void handleRecruitmentChanged(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                                            AdminFactionSettingsData data, Faction faction) {
        String value = data.recruitment;
        if (value == null) {
            sendUpdate();
            return;
        }

        boolean isOpen = "OPEN".equals(value);
        Faction updatedFaction = faction.withOpen(isOpen);
        factionManager.updateFaction(updatedFaction);

        player.sendMessage(Message.raw("[Admin] Set recruitment to " + (isOpen ? "Open" : "Invite Only")).color("#55FF55"));

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

        // Rebuild color section
        rebuildColorSection(cmd, events, faction);

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

        // Recruitment dropdown
        cmd.set("#RecruitmentDropdown.Entries", List.of(
                new DropdownEntryInfo(LocalizableString.fromString("Open"), "OPEN"),
                new DropdownEntryInfo(LocalizableString.fromString("Invite Only"), "INVITE_ONLY")
        ));
        cmd.set("#RecruitmentDropdown.Value", faction.open() ? "OPEN" : "INVITE_ONLY");
        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#RecruitmentDropdown",
                EventData.of("Button", "RecruitmentChanged")
                        .append("@Recruitment", "#RecruitmentDropdown.Value"),
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
    }

    private void rebuildColorSection(UICommandBuilder cmd, UIEventBuilder events, Faction faction) {
        String colorHex = faction.color();
        cmd.set("#ColorPreview.Background.Color", colorHex);
        cmd.set("#ColorValue.Text", colorHex);
        cmd.set("#FactionColorPicker.Value", colorHex);
        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#FactionColorPicker",
                EventData.of("Button", "ColorChanged")
                        .append("@Color", "#FactionColorPicker.Value"),
                false
        );
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

}
