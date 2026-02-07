package com.hyperfactions.gui.faction.page;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionPermissions;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.faction.FactionPageRegistry;
import com.hyperfactions.gui.faction.data.FactionSettingsTabsData;
import com.hyperfactions.gui.nav.NavBarHelper;
import com.hyperfactions.integration.PermissionManager;
import com.hyperfactions.manager.ClaimManager;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.TeleportManager;
import com.hyperfactions.util.ChunkUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;
import java.util.UUID;

/**
 * Faction Settings page with tabbed layout.
 * Tabs: GENERAL | PERMISSIONS | MEMBERS
 * Uses fresh page pattern for state changes.
 */
public class FactionSettingsTabsPage extends InteractiveCustomUIPage<FactionSettingsTabsData> {

    private static final String PAGE_ID = "settings";

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
    private final FactionManager factionManager;
    private final ClaimManager claimManager;
    private final GuiManager guiManager;
    private final HyperFactions hyperFactions;
    private final Faction faction;
    private final String activeTab;

    public FactionSettingsTabsPage(PlayerRef playerRef,
                                   FactionManager factionManager,
                                   ClaimManager claimManager,
                                   GuiManager guiManager,
                                   HyperFactions hyperFactions,
                                   Faction faction,
                                   String activeTab) {
        super(playerRef, CustomPageLifetime.CanDismiss, FactionSettingsTabsData.CODEC);
        this.playerRef = playerRef;
        this.factionManager = factionManager;
        this.claimManager = claimManager;
        this.guiManager = guiManager;
        this.hyperFactions = hyperFactions;
        this.faction = faction;
        this.activeTab = activeTab != null ? activeTab : "general";
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        UUID uuid = playerRef.getUuid();
        FactionMember member = faction.getMember(uuid);

        // Permission check - officer or leader only
        if (member == null || member.role().getLevel() < FactionRole.OFFICER.getLevel()) {
            cmd.append("HyperFactions/shared/error_page.ui");
            cmd.set("#ErrorMessage.Text", "Only officers and leaders can change faction settings.");
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#CloseBtn",
                    EventData.of("Button", "Close"),
                    false
            );
            return;
        }

        boolean isLeader = member.role() == FactionRole.LEADER;

        // Load the tabbed settings template
        cmd.append("HyperFactions/faction/settings_tabs.ui");

        // Setup navigation bar
        NavBarHelper.setupBar(playerRef, faction, PAGE_ID, cmd, events);

        // Tab button states - Disabled = active styling
        cmd.set("#TabGeneral.Disabled", activeTab.equals("general"));
        cmd.set("#TabPermissions.Disabled", activeTab.equals("permissions"));
        cmd.set("#TabMembers.Disabled", activeTab.equals("members"));

        // Tab content visibility
        cmd.set("#GeneralContent.Visible", activeTab.equals("general"));
        cmd.set("#PermissionsContent.Visible", activeTab.equals("permissions"));
        cmd.set("#MembersContent.Visible", activeTab.equals("members"));

        // Bind tab switch events
        bindTabEvents(events);

        // Build content for visible tab
        switch (activeTab) {
            case "general" -> buildGeneralTab(cmd, events, isLeader);
            case "permissions" -> buildPermissionsTab(cmd, events, isLeader);
            case "members" -> buildMembersTab(cmd, events, isLeader);
        }
    }

    private void bindTabEvents(UIEventBuilder events) {
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TabGeneral",
                EventData.of("Button", "SwitchTab").append("Tab", "general"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TabPermissions",
                EventData.of("Button", "SwitchTab").append("Tab", "permissions"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TabMembers",
                EventData.of("Button", "SwitchTab").append("Tab", "members"),
                false
        );
    }

    // =========================================================================
    // GENERAL TAB
    // =========================================================================

    private void buildGeneralTab(UICommandBuilder cmd, UIEventBuilder events, boolean isLeader) {
        // Append general settings content
        cmd.append("#GeneralContent", "HyperFactions/faction/settings_general_content.ui");

        // Set values
        cmd.set("#NameValue.Text", faction.name());

        String desc = faction.description() != null && !faction.description().isEmpty()
                ? faction.description()
                : "(None)";
        cmd.set("#DescValue.Text", desc);

        String tagDisplay = faction.tag() != null && !faction.tag().isEmpty()
                ? "[" + faction.tag().toUpperCase() + "]"
                : "(None)";
        cmd.set("#TagValue.Text", tagDisplay);

        String colorHex = getColorHex(faction.color());
        cmd.set("#ColorPreview.Background.Color", colorHex);
        cmd.set("#ColorValue.Text", colorHex);

        String recruitmentStatus = faction.open() ? "Open" : "Invite Only";
        cmd.set("#RecruitmentStatus.Text", recruitmentStatus);

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
            // Disable teleport and delete buttons when no home is set
            cmd.set("#TeleportHomeBtn.Disabled", true);
            cmd.set("#DeleteHomeBtn.Disabled", true);
        }

        // Bind general tab events
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NameEditBtn",
                EventData.of("Button", "OpenRenameModal"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#DescEditBtn",
                EventData.of("Button", "OpenDescriptionModal"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TagEditBtn",
                EventData.of("Button", "OpenTagModal"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ColorBtn",
                EventData.of("Button", "OpenColorPicker"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#RecruitmentBtn",
                EventData.of("Button", "OpenRecruitmentModal"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SetHomeBtn",
                EventData.of("Button", "SetHome"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TeleportHomeBtn",
                EventData.of("Button", "TeleportHome"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#DeleteHomeBtn",
                EventData.of("Button", "DeleteHome"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ModulesBtn",
                EventData.of("Button", "OpenModules"), false);

        // Danger zone for leaders - toggle visibility (always in template)
        if (isLeader) {
            cmd.set("#DangerZone.Visible", true);
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#DisbandBtn",
                    EventData.of("Button", "Disband"),
                    false
            );
        }
    }

    // =========================================================================
    // PERMISSIONS TAB
    // =========================================================================

    private void buildPermissionsTab(UICommandBuilder cmd, UIEventBuilder events, boolean isLeader) {
        // Check if user can edit permissions
        boolean canEdit = canEditPermissions(playerRef.getUuid(), faction);

        // Append permissions content
        cmd.append("#PermissionsContent", "HyperFactions/faction/settings_permissions_content.ui");

        // Get effective permissions
        FactionPermissions perms = ConfigManager.get().getEffectiveFactionPermissions(
                faction.getEffectivePermissions()
        );
        ConfigManager config = ConfigManager.get();

        // Outsider toggles
        buildToggle(cmd, events, "OutsiderBreakToggle", "outsiderBreak", perms.outsiderBreak(), canEdit, config);
        buildToggle(cmd, events, "OutsiderPlaceToggle", "outsiderPlace", perms.outsiderPlace(), canEdit, config);
        buildToggle(cmd, events, "OutsiderInteractToggle", "outsiderInteract", perms.outsiderInteract(), canEdit, config);

        // Ally toggles
        buildToggle(cmd, events, "AllyBreakToggle", "allyBreak", perms.allyBreak(), canEdit, config);
        buildToggle(cmd, events, "AllyPlaceToggle", "allyPlace", perms.allyPlace(), canEdit, config);
        buildToggle(cmd, events, "AllyInteractToggle", "allyInteract", perms.allyInteract(), canEdit, config);

        // Member toggles
        buildToggle(cmd, events, "MemberBreakToggle", "memberBreak", perms.memberBreak(), canEdit, config);
        buildToggle(cmd, events, "MemberPlaceToggle", "memberPlace", perms.memberPlace(), canEdit, config);
        buildToggle(cmd, events, "MemberInteractToggle", "memberInteract", perms.memberInteract(), canEdit, config);

        // PvP toggle
        buildToggle(cmd, events, "PvPToggle", "pvpEnabled", perms.pvpEnabled(), canEdit, config);
        cmd.set("#PvPStatus.Text", perms.pvpEnabled() ? "Enabled" : "Disabled");
        cmd.set("#PvPStatus.Style.TextColor", perms.pvpEnabled() ? "#55FF55" : "#FF5555");

        // Officers can edit - only leader can change this
        if (isLeader) {
            buildToggle(cmd, events, "OfficersCanEditToggle", "officersCanEdit", perms.officersCanEdit(), true, config);
        } else {
            // Hide access control section for non-leaders
            cmd.set("#AccessControlSection.Visible", false);
        }
    }

    private void buildToggle(UICommandBuilder cmd, UIEventBuilder events,
                             String elementId, String permName, boolean currentValue,
                             boolean canEdit, ConfigManager config) {
        boolean locked = config.isPermissionLocked(permName);
        String selector = "#" + elementId;

        if (locked) {
            // Locked by server - show lock icon
            cmd.set(selector + ".Text", "Locked");
            cmd.set(selector + ".Disabled", true);
        } else if (!canEdit) {
            // User cannot edit - show current value but disabled
            cmd.set(selector + ".Text", currentValue ? "On" : "Off");
            cmd.set(selector + ".Disabled", true);
        } else {
            // Editable - show current value with event binding
            cmd.set(selector + ".Text", currentValue ? "On" : "Off");
            cmd.set(selector + ".Disabled", false);

            // Set text color based on value
            if (currentValue) {
                cmd.set(selector + ".Style.Default.LabelStyle.TextColor", "#55FF55");
                cmd.set(selector + ".Style.Hovered.LabelStyle.TextColor", "#55FF55");
            } else {
                cmd.set(selector + ".Style.Default.LabelStyle.TextColor", "#FF5555");
                cmd.set(selector + ".Style.Hovered.LabelStyle.TextColor", "#FF5555");
            }

            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    selector,
                    EventData.of("Button", "TogglePerm").append("Perm", permName),
                    false
            );
        }
    }

    private boolean canEditPermissions(UUID uuid, Faction faction) {
        FactionMember member = faction.getMember(uuid);
        if (member == null) return false;

        FactionRole role = member.role();

        // Leader can always edit
        if (role == FactionRole.LEADER) return true;

        // Officers can edit if faction setting allows AND they have the permission
        if (role == FactionRole.OFFICER) {
            FactionPermissions perms = faction.getEffectivePermissions();
            return perms.officersCanEdit() &&
                   PermissionManager.get().hasPermission(uuid, Permissions.FACTION_PERMISSIONS);
        }

        return false;
    }

    // =========================================================================
    // MEMBERS TAB
    // =========================================================================

    private void buildMembersTab(UICommandBuilder cmd, UIEventBuilder events, boolean isLeader) {
        // Show a redirect to the Members page with quick stats
        cmd.append("#MembersContent", "HyperFactions/faction/settings_members_content.ui");

        // Set member counts
        int totalMembers = faction.getMemberCount();
        cmd.set("#MemberCount.Text", String.valueOf(totalMembers));

        // Count online members
        long onlineCount = faction.members().values().stream()
                .filter(m -> {
                    var playerRef = Universe.get().getPlayer(m.uuid());
                    return playerRef != null && playerRef.isValid();
                })
                .count();
        cmd.set("#OnlineCount.Text", String.valueOf(onlineCount));

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ViewMembersBtn",
                EventData.of("Button", "ViewMembers"),
                false
        );
    }

    private String getColorHex(String colorCode) {
        return COLORS.stream()
                .filter(c -> c.code.equals(colorCode))
                .findFirst()
                .map(c -> c.hex)
                .orElse("#FFFFFF");
    }

    // =========================================================================
    // EVENT HANDLING
    // =========================================================================

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                FactionSettingsTabsData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null || data.button == null) {
            sendUpdate();
            return;
        }

        // Handle navigation
        if ("Nav".equals(data.button) && data.navBar != null) {
            FactionPageRegistry.Entry entry = FactionPageRegistry.getInstance().getEntry(data.navBar);
            if (entry != null) {
                Faction currentFaction = factionManager.getFaction(faction.id());
                var page = entry.guiSupplier().create(player, ref, store, playerRef, currentFaction, guiManager);
                if (page != null) {
                    player.getPageManager().openCustomPage(ref, store, page);
                    return;
                }
            }
            sendUpdate();
            return;
        }

        // Handle close button (from error page)
        if ("Close".equals(data.button)) {
            guiManager.openFactionMain(player, ref, store, playerRef);
            return;
        }

        // Handle tab switching (fresh page pattern)
        if ("SwitchTab".equals(data.button) && data.tab != null) {
            // Reload faction and open with new tab
            Faction currentFaction = factionManager.getFaction(faction.id());
            guiManager.openSettingsWithTab(player, ref, store, playerRef, currentFaction, data.tab);
            return;
        }

        UUID uuid = playerRef.getUuid();
        FactionMember member = faction.getMember(uuid);

        // Verify permissions
        if (member == null || member.role().getLevel() < FactionRole.OFFICER.getLevel()) {
            player.sendMessage(Message.raw("You don't have permission to change settings.").color("#FF5555"));
            sendUpdate();
            return;
        }

        boolean isLeader = member.role() == FactionRole.LEADER;

        switch (data.button) {
            case "TogglePerm" -> handleTogglePerm(player, ref, store, data, isLeader);
            case "OpenColorPicker" -> guiManager.openColorPicker(player, ref, store, playerRef, faction);
            case "OpenRecruitmentModal" -> guiManager.openRecruitmentModal(player, ref, store, playerRef, faction);
            case "OpenRenameModal" -> guiManager.openRenameModal(player, ref, store, playerRef, faction);
            case "OpenDescriptionModal" -> guiManager.openDescriptionModal(player, ref, store, playerRef, faction);
            case "OpenTagModal" -> guiManager.openTagModal(player, ref, store, playerRef, faction);
            case "SetHome" -> handleSetHome(player, ref, store, uuid);
            case "TeleportHome" -> handleTeleportHome(player, ref, store, uuid);
            case "DeleteHome" -> handleDeleteHome(player, ref, store, uuid);
            case "OpenModules" -> guiManager.openFactionModules(player, ref, store, playerRef, faction);
            case "Disband" -> {
                if (!isLeader) {
                    player.sendMessage(Message.raw("Only the leader can disband the faction.").color("#FF5555"));
                    sendUpdate();
                    return;
                }
                guiManager.openDisbandConfirm(player, ref, store, playerRef, faction);
            }
            case "ViewMembers" -> guiManager.openFactionMembers(player, ref, store, playerRef, faction);
            default -> sendUpdate();
        }
    }

    private void handleTogglePerm(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                                  FactionSettingsTabsData data, boolean isLeader) {
        String permName = data.perm;
        if (permName == null) {
            sendUpdate();
            return;
        }

        ConfigManager config = ConfigManager.get();

        // Check if server has locked this setting
        if (config.isPermissionLocked(permName)) {
            player.sendMessage(Message.raw("This setting is locked by the server.").color("#FF5555"));
            sendUpdate();
            return;
        }

        // Check if user can edit permissions
        if (!canEditPermissions(playerRef.getUuid(), faction)) {
            player.sendMessage(Message.raw("You don't have permission to edit territory permissions.").color("#FF5555"));
            sendUpdate();
            return;
        }

        // officersCanEdit is leader-only
        if ("officersCanEdit".equals(permName) && !isLeader) {
            player.sendMessage(Message.raw("Only the leader can change officer access.").color("#FF5555"));
            sendUpdate();
            return;
        }

        // Get current permissions and toggle
        FactionPermissions current = faction.getEffectivePermissions();
        FactionPermissions updated = current.toggle(permName);

        // Save to faction
        Faction updatedFaction = faction.withPermissions(updated);
        factionManager.updateFaction(updatedFaction);

        // Re-open page with fresh data (fresh page pattern)
        Faction freshFaction = factionManager.getFaction(faction.id());
        guiManager.openSettingsWithTab(player, ref, store, playerRef, freshFaction, "permissions");
    }

    private void handleSetHome(Player player, Ref<EntityStore> ref, Store<EntityStore> store, UUID uuid) {
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        World world = player.getWorld();
        if (transform != null && world != null) {
            var pos = transform.getPosition();
            String worldName = world.getName();

            // Verify player is in faction territory
            int chunkX = ChunkUtil.toChunkCoord(pos.x);
            int chunkZ = ChunkUtil.toChunkCoord(pos.z);
            UUID claimOwner = claimManager.getClaimOwner(worldName, chunkX, chunkZ);
            if (claimOwner == null || !claimOwner.equals(faction.id())) {
                player.sendMessage(Message.raw("You must be in your faction's territory to set home.").color("#FF5555"));
                sendUpdate();
                return;
            }

            Faction.FactionHome newHome = Faction.FactionHome.create(
                    worldName, pos.x, pos.y, pos.z, 0f, 0f, uuid
            );

            Faction updatedFaction = faction.withHome(newHome);
            factionManager.updateFaction(updatedFaction);

            player.sendMessage(Message.raw("Faction home set to your current location!").color("#55FF55"));

            guiManager.openSettingsWithTab(player, ref, store, playerRef,
                    factionManager.getFaction(faction.id()), activeTab);
        } else {
            player.sendMessage(Message.raw("Could not determine your location.").color("#FF5555"));
            sendUpdate();
        }
    }

    private void handleTeleportHome(Player player, Ref<EntityStore> ref, Store<EntityStore> store, UUID uuid) {
        if (faction.home() == null) {
            player.sendMessage(Message.raw("No faction home set.").color("#FF5555"));
            sendUpdate();
            return;
        }

        // Close GUI first
        guiManager.closePage(player, ref, store);

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            player.sendMessage(Message.raw("Could not determine your location.").color("#FF5555"));
            return;
        }

        Vector3d pos = transform.getPosition();
        World world = player.getWorld();
        if (world == null) {
            player.sendMessage(Message.raw("Could not determine your world.").color("#FF5555"));
            return;
        }

        TeleportManager.StartLocation startLoc = new TeleportManager.StartLocation(
                world.getName(), pos.getX(), pos.getY(), pos.getZ()
        );

        // For instant teleport: executeTeleport runs immediately
        // For warmup teleport: TerritoryTickingSystem executes later
        TeleportManager.TeleportResult result = hyperFactions.getTeleportManager().teleportToHome(
                uuid,
                startLoc,
                f -> executeTeleport(store, ref, world, f),
                player::sendMessage,
                () -> hyperFactions.getCombatTagManager().isTagged(uuid)
        );

        handleTeleportResult(player, result);
    }

    private TeleportManager.TeleportResult executeTeleport(Store<EntityStore> store, Ref<EntityStore> ref,
                                                           World currentWorld, Faction faction) {
        Faction.FactionHome home = faction.home();
        if (home == null) {
            return TeleportManager.TeleportResult.NO_HOME;
        }

        World targetWorld;
        if (currentWorld.getName().equals(home.world())) {
            targetWorld = currentWorld;
        } else {
            targetWorld = Universe.get().getWorld(home.world());
            if (targetWorld == null) {
                return TeleportManager.TeleportResult.WORLD_NOT_FOUND;
            }
        }

        // Execute teleport on the target world's thread using createForPlayer for proper player teleportation
        targetWorld.execute(() -> {
            Vector3d position = new Vector3d(home.x(), home.y(), home.z());
            Vector3f rotation = new Vector3f(home.pitch(), home.yaw(), 0);
            Teleport teleport = Teleport.createForPlayer(targetWorld, position, rotation);
            store.addComponent(ref, Teleport.getComponentType(), teleport);
        });

        return TeleportManager.TeleportResult.SUCCESS_INSTANT;
    }

    private void handleTeleportResult(Player player, TeleportManager.TeleportResult result) {
        switch (result) {
            case NOT_IN_FACTION -> player.sendMessage(Message.raw("You are not in a faction.").color("#FF5555"));
            case NO_HOME -> player.sendMessage(Message.raw("Your faction has no home set.").color("#FF5555"));
            case COMBAT_TAGGED -> player.sendMessage(Message.raw("You cannot teleport while in combat!").color("#FF5555"));
            case SUCCESS_INSTANT -> player.sendMessage(Message.raw("Teleported to faction home!").color("#55FF55"));
            case ON_COOLDOWN, SUCCESS_WARMUP -> {} // Message sent by TeleportManager
            default -> {}
        }
    }

    private void handleDeleteHome(Player player, Ref<EntityStore> ref, Store<EntityStore> store, UUID uuid) {
        if (faction.home() == null) {
            player.sendMessage(Message.raw("Your faction does not have a home set.").color("#FFAA00"));
            sendUpdate();
            return;
        }

        Faction updatedFaction = faction.withHome(null);
        factionManager.updateFaction(updatedFaction);

        player.sendMessage(Message.raw("Faction home deleted!").color("#55FF55"));

        guiManager.openSettingsWithTab(player, ref, store, playerRef,
                factionManager.getFaction(faction.id()), activeTab);
    }

    private record ColorInfo(String code, String hex, String name) {}
}
