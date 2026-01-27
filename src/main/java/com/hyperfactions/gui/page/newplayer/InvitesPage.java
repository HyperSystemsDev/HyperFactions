package com.hyperfactions.gui.page.newplayer;

import com.hyperfactions.data.Faction;
import com.hyperfactions.data.JoinRequest;
import com.hyperfactions.data.PendingInvite;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.nav.NewPlayerNavBarHelper;
import com.hyperfactions.gui.shared.data.NewPlayerPageData;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.InviteManager;
import com.hyperfactions.manager.JoinRequestManager;
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

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Invites & Requests Page - shows pending faction invitations and outgoing join requests.
 * Allows players to accept/decline invites and cancel their own requests.
 */
public class InvitesPage extends InteractiveCustomUIPage<NewPlayerPageData> {

    private static final String PAGE_ID = "invites";
    private static final int MAX_INVITES_DISPLAYED = 5;
    private static final int MAX_REQUESTS_DISPLAYED = 5;

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final PowerManager powerManager;
    private final InviteManager inviteManager;
    private final JoinRequestManager joinRequestManager;
    private final GuiManager guiManager;

    public InvitesPage(PlayerRef playerRef,
                       FactionManager factionManager,
                       PowerManager powerManager,
                       InviteManager inviteManager,
                       JoinRequestManager joinRequestManager,
                       GuiManager guiManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, NewPlayerPageData.CODEC);
        this.playerRef = playerRef;
        this.factionManager = factionManager;
        this.powerManager = powerManager;
        this.inviteManager = inviteManager;
        this.joinRequestManager = joinRequestManager;
        this.guiManager = guiManager;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        // Load the main template
        cmd.append("HyperFactions/newplayer/invites.ui");

        // Setup navigation bar for new players
        NewPlayerNavBarHelper.setupBar(playerRef, PAGE_ID, cmd, events);

        // Get pending invites and outgoing requests
        List<PendingInvite> invites = inviteManager.getPlayerInvites(playerRef.getUuid());
        List<JoinRequest> requests = joinRequestManager.getPlayerRequests(playerRef.getUuid());

        // Set header with counts
        int totalCount = invites.size() + requests.size();
        cmd.set("#InviteCount.Text", totalCount + " pending");

        // === RECEIVED INVITES SECTION ===
        cmd.set("#InvitesHeader.Text", "RECEIVED INVITES (" + invites.size() + ")");
        if (invites.isEmpty()) {
            cmd.append("#InviteListContainer", "HyperFactions/faction/relation_empty.ui");
            cmd.set("#InviteListContainer[0] #EmptyText.Text", "No invites. Browse factions to find one!");
        } else {
            buildInviteCards(cmd, events, invites);
        }

        // === YOUR REQUESTS SECTION ===
        cmd.set("#RequestsHeader.Text", "YOUR REQUESTS (" + requests.size() + ")");
        if (requests.isEmpty()) {
            cmd.append("#RequestListContainer", "HyperFactions/faction/relation_empty.ui");
            cmd.set("#RequestListContainer[0] #EmptyText.Text", "No pending requests.");
        } else {
            buildRequestCards(cmd, events, requests);
        }
    }

    private void buildInviteCards(UICommandBuilder cmd, UIEventBuilder events,
                                  List<PendingInvite> invites) {
        int displayCount = Math.min(MAX_INVITES_DISPLAYED, invites.size());

        for (int i = 0; i < displayCount; i++) {
            PendingInvite invite = invites.get(i);
            Faction faction = factionManager.getFaction(invite.factionId());

            if (faction == null) {
                continue;
            }

            cmd.append("#InviteListContainer", "HyperFactions/newplayer/invite_card.ui");

            String prefix = "#InviteListContainer[" + i + "] ";

            // Faction info
            cmd.set(prefix + "#FactionName.Text", faction.name());

            // Invited by
            String inviterName = getPlayerName(invite.invitedBy());
            cmd.set(prefix + "#InvitedBy.Text", "Invited by: " + inviterName);

            // Stats
            PowerManager.FactionPowerStats stats = powerManager.getFactionPowerStats(faction.id());
            cmd.set(prefix + "#MemberCount.Text", faction.members().size() + " members");
            cmd.set(prefix + "#PowerCount.Text", String.format("%.0f power", stats.currentPower()));
            cmd.set(prefix + "#ClaimCount.Text", faction.claims().size() + " claims");

            // Time ago
            cmd.set(prefix + "#TimeAgo.Text", formatTimeAgo(invite.createdAt()));

            // Accept button
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    prefix + "#AcceptBtn",
                    EventData.of("Button", "Accept")
                            .append("FactionId", invite.factionId().toString())
                            .append("FactionName", faction.name()),
                    false
            );

            // Decline button
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    prefix + "#DeclineBtn",
                    EventData.of("Button", "Decline")
                            .append("FactionId", invite.factionId().toString()),
                    false
            );
        }
    }

    private void buildRequestCards(UICommandBuilder cmd, UIEventBuilder events,
                                   List<JoinRequest> requests) {
        int displayCount = Math.min(MAX_REQUESTS_DISPLAYED, requests.size());

        for (int i = 0; i < displayCount; i++) {
            JoinRequest request = requests.get(i);
            Faction faction = factionManager.getFaction(request.factionId());

            if (faction == null) {
                continue;
            }

            cmd.append("#RequestListContainer", "HyperFactions/newplayer/request_card.ui");

            String prefix = "#RequestListContainer[" + i + "] ";

            // Faction info
            cmd.set(prefix + "#FactionName.Text", faction.name());

            // Status
            cmd.set(prefix + "#StatusText.Text", "Awaiting review");

            // Stats
            PowerManager.FactionPowerStats stats = powerManager.getFactionPowerStats(faction.id());
            cmd.set(prefix + "#MemberCount.Text", faction.members().size() + " members");
            cmd.set(prefix + "#PowerCount.Text", String.format("%.0f power", stats.currentPower()));

            // Time remaining
            int hoursRemaining = request.getRemainingHours();
            cmd.set(prefix + "#TimeRemaining.Text", "Expires in " + hoursRemaining + "h");

            // Cancel button
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    prefix + "#CancelBtn",
                    EventData.of("Button", "CancelRequest")
                            .append("FactionId", request.factionId().toString()),
                    false
            );
        }
    }

    private String getPlayerName(UUID uuid) {
        for (Faction faction : factionManager.getAllFactions()) {
            var member = faction.getMember(uuid);
            if (member != null) {
                return member.username();
            }
        }
        return uuid.toString().substring(0, 8);
    }

    private String formatTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < TimeUnit.MINUTES.toMillis(1)) {
            return "just now";
        } else if (diff < TimeUnit.HOURS.toMillis(1)) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            return minutes + " min ago";
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
                                NewPlayerPageData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null || data.button == null) {
            sendUpdate();
            return;
        }

        // Handle navigation
        if (NewPlayerNavBarHelper.handleNavEvent(data, player, ref, store, playerRef, guiManager)) {
            return;
        }

        switch (data.button) {
            case "Browse" -> guiManager.openNewPlayerBrowse(player, ref, store, playerRef);

            case "Accept" -> handleAccept(player, ref, store, playerRef, data);

            case "Decline" -> handleDecline(player, ref, store, playerRef, data);

            case "CancelRequest" -> handleCancelRequest(player, ref, store, playerRef, data);

            default -> sendUpdate();
        }
    }

    private void handleAccept(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                              PlayerRef playerRef, NewPlayerPageData data) {
        if (data.factionId == null) {
            sendUpdate();
            return;
        }

        try {
            UUID factionId = UUID.fromString(data.factionId);
            UUID playerUuid = playerRef.getUuid();

            if (!inviteManager.hasInvite(factionId, playerUuid)) {
                player.sendMessage(Message.raw("This invite has expired or was revoked.").color("#FF5555"));
                sendUpdate();
                return;
            }

            Faction faction = factionManager.getFaction(factionId);
            if (faction == null) {
                player.sendMessage(Message.raw("Faction no longer exists.").color("#FF5555"));
                inviteManager.removeInvite(factionId, playerUuid);
                sendUpdate();
                return;
            }

            FactionManager.FactionResult result = factionManager.addMember(
                    factionId,
                    playerUuid,
                    playerRef.getUsername()
            );

            switch (result) {
                case SUCCESS -> {
                    player.sendMessage(
                            Message.raw("You joined ").color("#55FF55")
                                    .insert(Message.raw(faction.name()).color("#00FFFF"))
                                    .insert(Message.raw("!").color("#55FF55"))
                    );
                    // Clear all invites and requests
                    inviteManager.clearPlayerInvites(playerUuid);
                    joinRequestManager.clearPlayerRequests(playerUuid);
                    // Open faction dashboard
                    Faction freshFaction = factionManager.getPlayerFaction(playerUuid);
                    if (freshFaction != null) {
                        guiManager.openFactionDashboard(player, ref, store, playerRef, freshFaction);
                    }
                }
                case ALREADY_IN_FACTION -> {
                    player.sendMessage(Message.raw("You are already in a faction.").color("#FF5555"));
                    sendUpdate();
                }
                case FACTION_FULL -> {
                    player.sendMessage(Message.raw("This faction is full.").color("#FF5555"));
                    sendUpdate();
                }
                default -> {
                    player.sendMessage(Message.raw("Could not join faction.").color("#FF5555"));
                    sendUpdate();
                }
            }
        } catch (IllegalArgumentException e) {
            player.sendMessage(Message.raw("Invalid faction.").color("#FF5555"));
            sendUpdate();
        }
    }

    private void handleDecline(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                               PlayerRef playerRef, NewPlayerPageData data) {
        if (data.factionId == null) {
            sendUpdate();
            return;
        }

        try {
            UUID factionId = UUID.fromString(data.factionId);

            inviteManager.removeInvite(factionId, playerRef.getUuid());

            player.sendMessage(Message.raw("Invite declined.").color("#AAAAAA"));

            // Refresh the page
            guiManager.openInvitesPage(player, ref, store, playerRef);
        } catch (IllegalArgumentException e) {
            sendUpdate();
        }
    }

    private void handleCancelRequest(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                                     PlayerRef playerRef, NewPlayerPageData data) {
        if (data.factionId == null) {
            sendUpdate();
            return;
        }

        try {
            UUID factionId = UUID.fromString(data.factionId);

            Faction faction = factionManager.getFaction(factionId);
            String factionName = faction != null ? faction.name() : "the faction";

            joinRequestManager.removeRequest(factionId, playerRef.getUuid());

            player.sendMessage(Message.raw("Cancelled request to join " + factionName + ".").color("#AAAAAA"));

            // Refresh the page
            guiManager.openInvitesPage(player, ref, store, playerRef);
        } catch (IllegalArgumentException e) {
            sendUpdate();
        }
    }
}
