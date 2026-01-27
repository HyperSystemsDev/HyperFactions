package com.hyperfactions.gui.faction.page;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.data.*;
import com.hyperfactions.gui.faction.FactionPageRegistry;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.nav.NavBarHelper;
import com.hyperfactions.gui.faction.data.FactionPageData;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.InviteManager;
import com.hyperfactions.manager.JoinRequestManager;
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
 * Faction Invites Management page - manages outgoing invites and incoming join requests.
 * Only visible to officers and above.
 */
public class FactionInvitesPage extends InteractiveCustomUIPage<FactionPageData> {

    private static final String PAGE_ID = "invites";

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final InviteManager inviteManager;
    private final JoinRequestManager joinRequestManager;
    private final GuiManager guiManager;
    private final HyperFactions plugin;
    private final Faction faction;

    public FactionInvitesPage(PlayerRef playerRef,
                              FactionManager factionManager,
                              InviteManager inviteManager,
                              JoinRequestManager joinRequestManager,
                              GuiManager guiManager,
                              HyperFactions plugin,
                              Faction faction) {
        super(playerRef, CustomPageLifetime.CanDismiss, FactionPageData.CODEC);
        this.playerRef = playerRef;
        this.factionManager = factionManager;
        this.inviteManager = inviteManager;
        this.joinRequestManager = joinRequestManager;
        this.guiManager = guiManager;
        this.plugin = plugin;
        this.faction = faction;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        // Load the main template
        cmd.append("HyperFactions/faction/faction_invites.ui");

        // Setup navigation bar
        NavBarHelper.setupBar(playerRef, true, PAGE_ID, cmd, events);

        // Get outgoing invites
        Set<UUID> invitedPlayers = inviteManager.getFactionInvites(faction.id());
        List<InviteEntry> outgoingInvites = buildOutgoingInvites(invitedPlayers);

        // Get join requests
        List<JoinRequest> joinRequests = joinRequestManager.getFactionRequests(faction.id());

        // === OUTGOING INVITES SECTION ===
        cmd.set("#OutgoingHeader.Text", "OUTGOING INVITES (" + outgoingInvites.size() + ")");
        if (outgoingInvites.isEmpty()) {
            cmd.append("#OutgoingList", "HyperFactions/faction/relation_empty.ui");
            cmd.set("#OutgoingList[0] #EmptyText.Text", "No outgoing invites. Use /f invite <player> to invite someone.");
        } else {
            buildOutgoingEntries(cmd, events, outgoingInvites);
        }

        // === JOIN REQUESTS SECTION ===
        cmd.set("#RequestsHeader.Text", "JOIN REQUESTS (" + joinRequests.size() + ")");
        if (joinRequests.isEmpty()) {
            cmd.append("#RequestsList", "HyperFactions/faction/relation_empty.ui");
            cmd.set("#RequestsList[0] #EmptyText.Text", "No join requests. Players can request to join with /f request.");
        } else {
            buildRequestEntries(cmd, events, joinRequests);
        }
    }

    private List<InviteEntry> buildOutgoingInvites(Set<UUID> invitedPlayers) {
        List<InviteEntry> entries = new ArrayList<>();

        for (UUID playerUuid : invitedPlayers) {
            PendingInvite invite = inviteManager.getInvite(faction.id(), playerUuid);
            if (invite == null || invite.isExpired()) {
                continue;
            }

            // Try to get player name from faction members
            String playerName = getPlayerName(playerUuid);
            String inviterName = getPlayerName(invite.invitedBy());

            entries.add(new InviteEntry(
                playerUuid,
                playerName,
                inviterName,
                invite.getRemainingSeconds()
            ));
        }

        // Sort by remaining time (expiring soonest first)
        entries.sort(Comparator.comparingInt(InviteEntry::remainingSeconds));
        return entries;
    }

    private void buildOutgoingEntries(UICommandBuilder cmd, UIEventBuilder events,
                                       List<InviteEntry> entries) {
        for (int i = 0; i < entries.size(); i++) {
            InviteEntry entry = entries.get(i);

            // Append the entry template
            cmd.append("#OutgoingList", "HyperFactions/faction/invite_entry.ui");

            String prefix = "#OutgoingList[" + i + "] ";

            // Player info
            cmd.set(prefix + "#PlayerName.Text", entry.playerName);
            cmd.set(prefix + "#InviterName.Text", "Invited by: " + entry.inviterName);
            cmd.set(prefix + "#TimeRemaining.Text", "Expires: " + formatTime(entry.remainingSeconds));

            // Cancel button event
            events.addEventBinding(
                CustomUIEventBindingType.Activating,
                prefix + "#CancelBtn",
                EventData.of("Button", "CancelInvite")
                    .append("PlayerUuid", entry.playerUuid.toString()),
                false
            );
        }
    }

    private void buildRequestEntries(UICommandBuilder cmd, UIEventBuilder events,
                                      List<JoinRequest> requests) {
        for (int i = 0; i < requests.size(); i++) {
            JoinRequest request = requests.get(i);

            // Append the entry template
            cmd.append("#RequestsList", "HyperFactions/faction/request_entry.ui");

            String prefix = "#RequestsList[" + i + "] ";

            // Player info
            cmd.set(prefix + "#PlayerName.Text", request.playerName());

            // Message (truncate if too long)
            String message = request.message();
            if (message == null || message.isBlank()) {
                cmd.set(prefix + "#RequestMessage.Text", "\"No message\"");
            } else {
                if (message.length() > 40) {
                    message = message.substring(0, 37) + "...";
                }
                cmd.set(prefix + "#RequestMessage.Text", "\"" + message + "\"");
            }

            // Time remaining
            int remainingHours = request.getRemainingHours();
            cmd.set(prefix + "#TimeRemaining.Text", "Expires: " + remainingHours + "h");

            // Accept button event
            events.addEventBinding(
                CustomUIEventBindingType.Activating,
                prefix + "#AcceptBtn",
                EventData.of("Button", "AcceptRequest")
                    .append("PlayerUuid", request.playerUuid().toString()),
                false
            );

            // Decline button event
            events.addEventBinding(
                CustomUIEventBindingType.Activating,
                prefix + "#DeclineBtn",
                EventData.of("Button", "DeclineRequest")
                    .append("PlayerUuid", request.playerUuid().toString()),
                false
            );
        }
    }

    /**
     * Gets player name from faction member data.
     * Note: For outgoing invites, the invited player won't be in any faction yet,
     * so we may not find them. The name is cached in PendingInvite for invitedBy.
     */
    private String getPlayerName(UUID playerUuid) {
        // Check faction members across all factions
        for (Faction f : factionManager.getAllFactions()) {
            FactionMember member = f.getMember(playerUuid);
            if (member != null) {
                return member.username();
            }
        }
        return "Unknown";
    }

    private String formatTime(int seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m";
        } else {
            return (seconds / 3600) + "h";
        }
    }

    private record InviteEntry(UUID playerUuid, String playerName, String inviterName, int remainingSeconds) {}

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                FactionPageData data) {
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
                Faction freshFaction = factionManager.getFaction(faction.id());
                var page = entry.guiSupplier().create(player, ref, store, playerRef, freshFaction, guiManager);
                if (page != null) {
                    player.getPageManager().openCustomPage(ref, store, page);
                    return;
                }
            }
            sendUpdate();
            return;
        }

        switch (data.button) {
            case "CancelInvite" -> {
                if (data.playerUuid != null) {
                    try {
                        UUID targetUuid = UUID.fromString(data.playerUuid);
                        inviteManager.removeInvite(faction.id(), targetUuid);

                        String playerName = getPlayerName(targetUuid);
                        player.sendMessage(Message.raw("Cancelled invite to " + playerName + ".").color("#AAAAAA"));

                        refresh(player, ref, store, playerRef);
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(Message.raw("Invalid player.").color("#FF5555"));
                        sendUpdate();
                    }
                } else {
                    sendUpdate();
                }
            }

            case "AcceptRequest" -> {
                if (data.playerUuid != null) {
                    try {
                        UUID targetUuid = UUID.fromString(data.playerUuid);
                        JoinRequest request = joinRequestManager.acceptRequest(faction.id(), targetUuid);

                        if (request != null) {
                            // Add player to faction
                            FactionManager.FactionResult result = factionManager.addMember(
                                faction.id(), targetUuid, request.playerName()
                            );

                            if (result == FactionManager.FactionResult.SUCCESS) {
                                // Clear player's other requests since they joined a faction
                                joinRequestManager.clearPlayerRequests(targetUuid);

                                player.sendMessage(Message.raw(request.playerName() + " has joined the faction!").color("#55FF55"));

                                // Note: The joining player will see a welcome message next time they join/interact
                                // Real-time notification requires access to tracked players which isn't available here
                            } else if (result == FactionManager.FactionResult.FACTION_FULL) {
                                player.sendMessage(Message.raw("Faction is full. Cannot accept request.").color("#FF5555"));
                            } else {
                                player.sendMessage(Message.raw("Failed to add player to faction.").color("#FF5555"));
                            }
                        } else {
                            player.sendMessage(Message.raw("Request not found or expired.").color("#FF5555"));
                        }

                        refresh(player, ref, store, playerRef);
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(Message.raw("Invalid player.").color("#FF5555"));
                        sendUpdate();
                    }
                } else {
                    sendUpdate();
                }
            }

            case "DeclineRequest" -> {
                if (data.playerUuid != null) {
                    try {
                        UUID targetUuid = UUID.fromString(data.playerUuid);
                        JoinRequest request = joinRequestManager.getRequest(faction.id(), targetUuid);
                        String playerName = request != null ? request.playerName() : "Unknown";

                        joinRequestManager.declineRequest(faction.id(), targetUuid);

                        player.sendMessage(Message.raw("Declined join request from " + playerName + ".").color("#AAAAAA"));

                        refresh(player, ref, store, playerRef);
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(Message.raw("Invalid player.").color("#FF5555"));
                        sendUpdate();
                    }
                } else {
                    sendUpdate();
                }
            }

            default -> sendUpdate();
        }
    }

    private void refresh(Player player, Ref<EntityStore> ref, Store<EntityStore> store, PlayerRef playerRef) {
        Faction freshFaction = factionManager.getFaction(faction.id());
        if (freshFaction != null) {
            guiManager.openFactionInvites(player, ref, store, playerRef, freshFaction);
        } else {
            guiManager.openFactionMain(player, ref, store, playerRef);
        }
    }
}
