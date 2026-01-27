package com.hyperfactions.gui.faction.page;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionLog;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.nav.NavBarHelper;
import com.hyperfactions.gui.faction.data.FactionDashboardData;
import com.hyperfactions.manager.ChatManager;
import com.hyperfactions.manager.ClaimManager;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.PowerManager;
import com.hyperfactions.manager.TeleportManager;
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
import java.util.concurrent.TimeUnit;

/**
 * Faction Dashboard page - main view for faction members.
 * Shows faction stats, quick actions, and recent activity.
 */
public class FactionDashboardPage extends InteractiveCustomUIPage<FactionDashboardData> {

    private static final String PAGE_ID = "dashboard";
    private static final int ACTIVITY_ENTRIES = 5;

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final ClaimManager claimManager;
    private final PowerManager powerManager;
    private final TeleportManager teleportManager;
    private final GuiManager guiManager;
    private final HyperFactions plugin;
    private final Faction faction;

    public FactionDashboardPage(PlayerRef playerRef,
                                FactionManager factionManager,
                                ClaimManager claimManager,
                                PowerManager powerManager,
                                TeleportManager teleportManager,
                                GuiManager guiManager,
                                HyperFactions plugin,
                                Faction faction) {
        super(playerRef, CustomPageLifetime.CanDismiss, FactionDashboardData.CODEC);
        this.playerRef = playerRef;
        this.factionManager = factionManager;
        this.claimManager = claimManager;
        this.powerManager = powerManager;
        this.teleportManager = teleportManager;
        this.guiManager = guiManager;
        this.plugin = plugin;
        this.faction = faction;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        UUID viewerUuid = playerRef.getUuid();
        FactionMember member = faction.getMember(viewerUuid);
        FactionRole viewerRole = member != null ? member.role() : FactionRole.MEMBER;
        boolean isOfficerPlus = viewerRole.getLevel() >= FactionRole.OFFICER.getLevel();
        boolean isLeader = viewerRole == FactionRole.LEADER;

        // Load the main template
        cmd.append("HyperFactions/faction/faction_dashboard.ui");

        // Setup navigation bar
        setupNavBar(cmd, events);

        // Faction header
        buildFactionHeader(cmd);

        // Stat cards
        buildStatCards(cmd);

        // Quick actions (conditional)
        buildQuickActions(cmd, events, isOfficerPlus, isLeader);

        // Activity feed
        buildActivityFeed(cmd, events);
    }

    private void setupNavBar(UICommandBuilder cmd, UIEventBuilder events) {
        // Use NavBarHelper for consistent nav bar setup across all pages
        NavBarHelper.setupBar(playerRef, true, PAGE_ID, cmd, events);
    }

    private void buildFactionHeader(UICommandBuilder cmd) {
        // Faction name with color
        cmd.set("#FactionName.Text", faction.name());

        // Tag in gold if present
        if (faction.tag() != null && !faction.tag().isEmpty()) {
            cmd.set("#FactionTag.Text", "[" + faction.tag() + "]");
        }

        // Description
        if (faction.description() != null && !faction.description().isEmpty()) {
            cmd.set("#FactionDescription.Text", "\"" + faction.description() + "\"");
        }
    }

    private void buildStatCards(UICommandBuilder cmd) {
        PowerManager.FactionPowerStats stats = powerManager.getFactionPowerStats(faction.id());

        // Power card
        cmd.append("#PowerCard", "HyperFactions/faction/dashboard_stat_card.ui");
        cmd.set("#PowerCard #StatTitle.Text", "POWER");
        cmd.set("#PowerCard #StatValue.Text", String.format("%.0f / %.0f", stats.currentPower(), stats.maxPower()));
        int powerPercent = stats.maxPower() > 0 ? (int) ((stats.currentPower() / stats.maxPower()) * 100) : 0;
        cmd.set("#PowerCard #StatSecondary.Text", powerPercent + "%");

        // Claims card
        cmd.append("#ClaimsCard", "HyperFactions/faction/dashboard_stat_card.ui");
        cmd.set("#ClaimsCard #StatTitle.Text", "CLAIMS");
        cmd.set("#ClaimsCard #StatValue.Text", faction.claims().size() + " / " + stats.maxClaims());
        int available = Math.max(0, stats.maxClaims() - faction.claims().size());
        cmd.set("#ClaimsCard #StatSecondary.Text", available + " available");

        // Members card
        cmd.append("#MembersCard", "HyperFactions/faction/dashboard_stat_card.ui");
        cmd.set("#MembersCard #StatTitle.Text", "MEMBERS");
        cmd.set("#MembersCard #StatValue.Text", String.valueOf(faction.members().size()));
        // Count online members (simplified - in reality you'd check online status)
        cmd.set("#MembersCard #StatSecondary.Text", "");
    }

    private void buildQuickActions(UICommandBuilder cmd, UIEventBuilder events,
                                    boolean isOfficerPlus, boolean isLeader) {
        // HOME button - only if faction has a home
        if (faction.hasHome()) {
            cmd.append("#HomeBtnContainer", "HyperFactions/faction/dashboard_action_btn.ui");
            cmd.set("#HomeBtnContainer #ActionBtn.Text", "HOME");
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#HomeBtnContainer #ActionBtn",
                    EventData.of("Button", "Home"),
                    false
            );
        }

        // CLAIM button - only for officers+
        if (isOfficerPlus) {
            cmd.append("#ClaimBtnContainer", "HyperFactions/faction/dashboard_action_btn.ui");
            cmd.set("#ClaimBtnContainer #ActionBtn.Text", "CLAIM");
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#ClaimBtnContainer #ActionBtn",
                    EventData.of("Button", "Claim"),
                    false
            );
        }

        // F-CHAT button (placeholder - chat system not implemented yet)
        cmd.append("#FChatBtnContainer", "HyperFactions/faction/dashboard_action_btn.ui");
        cmd.set("#FChatBtnContainer #ActionBtn.Text", "F-CHAT");
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#FChatBtnContainer #ActionBtn",
                EventData.of("Button", "FChat"),
                false
        );

        // A-CHAT button (placeholder - chat system not implemented yet)
        cmd.append("#AChatBtnContainer", "HyperFactions/faction/dashboard_action_btn.ui");
        cmd.set("#AChatBtnContainer #ActionBtn.Text", "A-CHAT");
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#AChatBtnContainer #ActionBtn",
                EventData.of("Button", "AChat"),
                false
        );

        // LEAVE button - only for non-leaders
        if (!isLeader) {
            cmd.append("#LeaveBtnContainer", "HyperFactions/faction/dashboard_action_btn.ui");
            cmd.set("#LeaveBtnContainer #ActionBtn.Text", "LEAVE");
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#LeaveBtnContainer #ActionBtn",
                    EventData.of("Button", "Leave"),
                    false
            );
        }
    }

    private void buildActivityFeed(UICommandBuilder cmd, UIEventBuilder events) {
        // TODO: Enable View All button when Activity Logs page (logs_viewer.ui) is implemented
        // events.addEventBinding(
        //         CustomUIEventBindingType.Activating,
        //         "#ViewLogsBtn",
        //         EventData.of("Button", "ViewLogs"),
        //         false
        // );

        // Show recent activity entries
        List<FactionLog> logs = faction.logs();
        int displayCount = Math.min(ACTIVITY_ENTRIES, logs.size());

        for (int i = 0; i < displayCount; i++) {
            FactionLog log = logs.get(i);
            String containerId = "#ActivityEntry" + i;

            cmd.append(containerId, "HyperFactions/faction/activity_entry.ui");
            cmd.set(containerId + " #ActivityType.Text", log.type().getDisplayName().toUpperCase());
            cmd.set(containerId + " #ActivityMessage.Text", log.message());
            cmd.set(containerId + " #ActivityTime.Text", formatTimeAgo(log.timestamp()));
        }
    }

    private String formatTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < TimeUnit.MINUTES.toMillis(1)) {
            return "now";
        } else if (diff < TimeUnit.HOURS.toMillis(1)) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            return minutes + "m ago";
        } else if (diff < TimeUnit.DAYS.toMillis(1)) {
            long hours = TimeUnit.MILLISECONDS.toHours(diff);
            return hours + "h ago";
        } else {
            long days = TimeUnit.MILLISECONDS.toDays(diff);
            return days + "d ago";
        }
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                FactionDashboardData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null || data.button == null) {
            sendUpdate();
            return;
        }

        UUID uuid = playerRef.getUuid();
        Faction currentFaction = factionManager.getPlayerFaction(uuid);

        // Handle navigation via NavBarHelper (consistent with other pages)
        if (NavBarHelper.handleNavEvent(data, player, ref, store, playerRef, currentFaction, guiManager)) {
            return;
        }

        // Verify still in faction
        if (currentFaction == null) {
            player.sendMessage(Message.raw("You are no longer in a faction.").color("#FF5555"));
            guiManager.openFactionMain(player, ref, store, playerRef);
            return;
        }

        FactionMember member = currentFaction.getMember(uuid);
        FactionRole viewerRole = member != null ? member.role() : FactionRole.MEMBER;
        boolean isOfficerPlus = viewerRole.getLevel() >= FactionRole.OFFICER.getLevel();
        boolean isLeader = viewerRole == FactionRole.LEADER;

        switch (data.button) {
            case "Home" -> handleHomeAction(player, ref, store, uuid, currentFaction);

            case "Claim" -> {
                if (!isOfficerPlus) {
                    player.sendMessage(Message.raw("Only officers can claim territory.").color("#FF5555"));
                    sendUpdate();
                    return;
                }
                handleClaimAction(player, ref, store, playerRef, uuid, currentFaction);
            }

            case "FChat" -> {
                ChatManager chatManager = plugin.getChatManager();
                ChatManager.ChatChannel newChannel = chatManager.toggleFactionChat(uuid);
                String display = ChatManager.getChannelDisplay(newChannel);
                String color = ChatManager.getChannelColor(newChannel);
                player.sendMessage(Message.raw("Chat mode: ").color("#AAAAAA")
                        .insert(Message.raw(display).color(color)));
                sendUpdate();
            }

            case "AChat" -> {
                ChatManager chatManager = plugin.getChatManager();
                ChatManager.ChatChannel newChannel = chatManager.toggleAllyChat(uuid);
                String display = ChatManager.getChannelDisplay(newChannel);
                String color = ChatManager.getChannelColor(newChannel);
                player.sendMessage(Message.raw("Chat mode: ").color("#AAAAAA")
                        .insert(Message.raw(display).color(color)));
                sendUpdate();
            }

            case "Leave" -> {
                if (isLeader) {
                    player.sendMessage(Message.raw("Leaders cannot leave. Transfer leadership or disband.").color("#FF5555"));
                    sendUpdate();
                    return;
                }
                guiManager.openLeaveConfirm(player, ref, store, playerRef, currentFaction);
            }

            // TODO: Enable when Activity Logs page (logs_viewer.ui) is implemented
            // case "ViewLogs" -> {
            //     guiManager.openLogsViewer(player, ref, store, playerRef, currentFaction);
            // }

            default -> sendUpdate();
        }
    }

    private void handleHomeAction(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                                  UUID uuid, Faction faction) {
        if (!faction.hasHome()) {
            player.sendMessage(Message.raw("Your faction has no home set.").color("#FF5555"));
            sendUpdate();
            return;
        }

        // Get player's current location for start location
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            player.sendMessage(Message.raw("Could not determine your location.").color("#FF5555"));
            sendUpdate();
            return;
        }

        Vector3d pos = transform.getPosition();
        World world = player.getWorld();
        if (world == null) {
            player.sendMessage(Message.raw("Could not determine your world.").color("#FF5555"));
            sendUpdate();
            return;
        }

        TeleportManager.StartLocation startLoc = new TeleportManager.StartLocation(
                world.getName(), pos.getX(), pos.getY(), pos.getZ()
        );

        // Initiate teleport with warmup/combat checking
        TeleportManager.TeleportResult result = teleportManager.teleportToHome(
                uuid,
                startLoc,
                (delayTicks, task) -> plugin.scheduleDelayedTask(delayTicks, task),
                plugin::cancelTask,
                f -> executeTeleport(store, ref, world, f),
                message -> player.sendMessage(Message.raw(message)),
                () -> plugin.getCombatTagManager().isTagged(uuid)
        );

        // Handle immediate result messages
        handleTeleportResult(player, result);
    }

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

    private void handleClaimAction(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                                   PlayerRef playerRef, UUID uuid, Faction faction) {
        // Get player's current chunk
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            player.sendMessage(Message.raw("Could not determine your location.").color("#FF5555"));
            sendUpdate();
            return;
        }

        World world = player.getWorld();
        if (world == null) {
            player.sendMessage(Message.raw("Could not determine your world.").color("#FF5555"));
            sendUpdate();
            return;
        }

        Vector3d pos = transform.getPosition();
        int chunkX = (int) Math.floor(pos.getX()) >> 4;
        int chunkZ = (int) Math.floor(pos.getZ()) >> 4;

        // Attempt to claim
        ClaimManager.ClaimResult result = claimManager.claim(uuid, world.getName(), chunkX, chunkZ);

        switch (result) {
            case SUCCESS -> {
                player.sendMessage(
                        Message.raw("Claimed chunk at (").color("#55FF55")
                                .insert(Message.raw(chunkX + ", " + chunkZ).color("#AAAAAA"))
                                .insert(Message.raw(")").color("#55FF55"))
                );
                // Refresh dashboard with updated faction data
                Faction fresh = factionManager.getFaction(faction.id());
                if (fresh != null) {
                    guiManager.openFactionDashboard(player, ref, store, playerRef, fresh);
                }
            }
            case NOT_IN_FACTION -> player.sendMessage(Message.raw("You are not in a faction.").color("#FF5555"));
            case NOT_OFFICER -> player.sendMessage(Message.raw("Only officers can claim land.").color("#FF5555"));
            case ALREADY_CLAIMED_SELF -> player.sendMessage(Message.raw("This chunk is already claimed by your faction.").color("#FFAA00"));
            case ALREADY_CLAIMED_OTHER, ALREADY_CLAIMED_ALLY, ALREADY_CLAIMED_ENEMY -> player.sendMessage(Message.raw("This chunk is claimed by another faction.").color("#FF5555"));
            case MAX_CLAIMS_REACHED -> player.sendMessage(Message.raw("Your faction has reached its claim limit.").color("#FF5555"));
            case WORLD_NOT_ALLOWED -> player.sendMessage(Message.raw("Claiming is not allowed in this world.").color("#FF5555"));
            case NOT_ADJACENT -> player.sendMessage(Message.raw("You can only claim chunks adjacent to existing claims.").color("#FF5555"));
            case INSUFFICIENT_POWER -> player.sendMessage(Message.raw("Your faction doesn't have enough power to claim more land.").color("#FF5555"));
            default -> player.sendMessage(Message.raw("Could not claim this chunk.").color("#FF5555"));
        }
    }
}
