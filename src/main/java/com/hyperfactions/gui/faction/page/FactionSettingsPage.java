package com.hyperfactions.gui.faction.page;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.gui.faction.FactionPageRegistry;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.nav.NavBarHelper;
import com.hyperfactions.gui.faction.data.FactionSettingsData;
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
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
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
 * Faction Settings page - comprehensive settings management with sections.
 * Requires officer or leader role for most actions.
 */
public class FactionSettingsPage extends InteractiveCustomUIPage<FactionSettingsData> {

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

    public FactionSettingsPage(PlayerRef playerRef,
                               FactionManager factionManager,
                               ClaimManager claimManager,
                               GuiManager guiManager,
                               HyperFactions hyperFactions,
                               Faction faction) {
        super(playerRef, CustomPageLifetime.CanDismiss, FactionSettingsData.CODEC);
        this.playerRef = playerRef;
        this.factionManager = factionManager;
        this.claimManager = claimManager;
        this.guiManager = guiManager;
        this.hyperFactions = hyperFactions;
        this.faction = faction;
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
            // Bind close button to go back to dashboard
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#CloseBtn",
                    EventData.of("Button", "Close"),
                    false
            );
            return;
        }

        boolean isLeader = member.role() == FactionRole.LEADER;

        // Load the settings template
        cmd.append("HyperFactions/faction/faction_settings.ui");

        // Setup navigation bar
        NavBarHelper.setupBar(playerRef, true, PAGE_ID, cmd, events);

        // === GENERAL SECTION ===
        cmd.set("#NameValue.Text", faction.name());

        String desc = faction.description() != null && !faction.description().isEmpty()
                ? faction.description()
                : "(None)";
        cmd.set("#DescValue.Text", desc);

        String tagDisplay = faction.tag() != null && !faction.tag().isEmpty()
                ? "[" + faction.tag().toUpperCase() + "]"
                : "(None)";
        cmd.set("#TagValue.Text", tagDisplay);

        // Edit buttons for general section
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#NameEditBtn",
                EventData.of("Button", "OpenRenameModal"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#DescEditBtn",
                EventData.of("Button", "OpenDescriptionModal"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TagEditBtn",
                EventData.of("Button", "OpenTagModal"),
                false
        );

        // === APPEARANCE SECTION ===
        String colorHex = getColorHex(faction.color());
        cmd.set("#ColorPreview.Background.Color", colorHex);
        cmd.set("#ColorValue.Text", colorHex);

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ColorBtn",
                EventData.of("Button", "OpenColorPicker"),
                false
        );

        // === RECRUITMENT SECTION ===
        String recruitmentStatus = faction.open() ? "Open" : "Invite Only";
        cmd.set("#RecruitmentStatus.Text", recruitmentStatus);

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#RecruitmentBtn",
                EventData.of("Button", "OpenRecruitmentModal"),
                false
        );

        // === HOME LOCATION SECTION ===
        if (faction.home() != null) {
            Faction.FactionHome home = faction.home();
            String worldName = home.world();
            // Simplify world name if it's a path
            if (worldName.contains("/")) {
                worldName = worldName.substring(worldName.lastIndexOf('/') + 1);
            }
            String homeText = String.format("%s (%.0f, %.0f, %.0f)",
                    worldName, home.x(), home.y(), home.z());
            cmd.set("#HomeLocation.Text", homeText);

            // Enable teleport button
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#TeleportHomeBtn",
                    EventData.of("Button", "TeleportHome"),
                    false
            );
        } else {
            cmd.set("#HomeLocation.Text", "Not set");
            // Note: Can't disable button styling dynamically - TeleportHome handler checks for null home
        }

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#SetHomeBtn",
                EventData.of("Button", "SetHome"),
                false
        );

        // === MODULES SECTION ===
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ModulesBtn",
                EventData.of("Button", "OpenModules"),
                false
        );

        // === DANGER ZONE (Leader Only) ===
        // Use conditional appending since cmd.set() for Visible doesn't work
        if (isLeader) {
            cmd.append("#DangerZoneContainer", "HyperFactions/faction/settings_danger_zone.ui");

            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#DangerZoneContainer #DisbandBtn",
                    EventData.of("Button", "Disband"),
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
                                FactionSettingsData data) {
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
            case "OpenColorPicker" -> {
                guiManager.openColorPicker(player, ref, store, playerRef, faction);
            }

            case "OpenRecruitmentModal" -> {
                guiManager.openRecruitmentModal(player, ref, store, playerRef, faction);
            }

            case "OpenRenameModal" -> {
                guiManager.openRenameModal(player, ref, store, playerRef, faction);
            }

            case "OpenDescriptionModal" -> {
                guiManager.openDescriptionModal(player, ref, store, playerRef, faction);
            }

            case "OpenTagModal" -> {
                guiManager.openTagModal(player, ref, store, playerRef, faction);
            }

            case "SetHome" -> {
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
                            worldName,
                            pos.x,
                            pos.y,
                            pos.z,
                            0f, // Yaw - not easily available from transform
                            0f, // Pitch - not easily available from transform
                            uuid
                    );

                    Faction updatedFaction = faction.withHome(newHome);
                    factionManager.updateFaction(updatedFaction);

                    player.sendMessage(
                            Message.raw("Faction home set to your current location!").color("#55FF55")
                    );

                    guiManager.openFactionSettings(player, ref, store, playerRef,
                            factionManager.getFaction(faction.id()));
                } else {
                    player.sendMessage(Message.raw("Could not determine your location.").color("#FF5555"));
                    sendUpdate();
                }
            }

            case "TeleportHome" -> {
                if (faction.home() == null) {
                    player.sendMessage(Message.raw("No faction home set.").color("#FF5555"));
                    sendUpdate();
                    return;
                }

                // Close GUI first
                guiManager.closePage(player, ref, store);

                // Get player location for movement checking
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

                // Initiate teleport with warmup/combat checking
                TeleportManager.TeleportResult result = hyperFactions.getTeleportManager().teleportToHome(
                        uuid,
                        startLoc,
                        (delayTicks, task) -> hyperFactions.scheduleDelayedTask(delayTicks, task),
                        hyperFactions::cancelTask,
                        f -> executeTeleport(store, ref, world, f),
                        message -> player.sendMessage(Message.raw(message)),
                        () -> hyperFactions.getCombatTagManager().isTagged(uuid)
                );

                // Handle immediate result messages
                handleTeleportResult(player, result);
            }

            case "OpenModules" -> {
                guiManager.openFactionModules(player, ref, store, playerRef, faction);
            }

            case "Disband" -> {
                if (!isLeader) {
                    player.sendMessage(Message.raw("Only the leader can disband the faction.").color("#FF5555"));
                    sendUpdate();
                    return;
                }

                // Open disband confirmation modal
                guiManager.openDisbandConfirm(player, ref, store, playerRef, faction);
            }

            default -> sendUpdate();
        }
    }

    /**
     * Executes the actual teleport to faction home using the proper Teleport component.
     */
    private TeleportManager.TeleportResult executeTeleport(Store<EntityStore> store, Ref<EntityStore> ref,
                                                           World currentWorld, Faction faction) {
        Faction.FactionHome home = faction.home();
        if (home == null) {
            return TeleportManager.TeleportResult.NO_HOME;
        }

        // Get target world (supports cross-world teleportation)
        World targetWorld;
        if (currentWorld.getName().equals(home.world())) {
            targetWorld = currentWorld;
        } else {
            targetWorld = Universe.get().getWorld(home.world());
            if (targetWorld == null) {
                return TeleportManager.TeleportResult.WORLD_NOT_FOUND;
            }
        }

        // Create and apply teleport using the proper Teleport component
        Vector3d position = new Vector3d(home.x(), home.y(), home.z());
        Vector3f rotation = new Vector3f(home.pitch(), home.yaw(), 0);
        Teleport teleport = new Teleport(targetWorld, position, rotation);
        store.addComponent(ref, Teleport.getComponentType(), teleport);

        return TeleportManager.TeleportResult.SUCCESS_INSTANT;
    }

    /**
     * Handles teleport result messages for immediate results.
     */
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

    private record ColorInfo(String code, String hex, String name) {}
}
