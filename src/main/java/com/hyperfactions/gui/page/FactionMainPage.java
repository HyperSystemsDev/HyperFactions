package com.hyperfactions.gui.page;

import com.hyperfactions.data.*;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.data.FactionMainData;
import com.hyperfactions.manager.*;
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
 * Faction Main page - displays faction dashboard with stats and quick actions.
 */
public class FactionMainPage extends InteractiveCustomUIPage<FactionMainData> {

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final ClaimManager claimManager;
    private final PowerManager powerManager;
    private final TeleportManager teleportManager;
    private final InviteManager inviteManager;
    private final GuiManager guiManager;

    public FactionMainPage(PlayerRef playerRef,
                           FactionManager factionManager,
                           ClaimManager claimManager,
                           PowerManager powerManager,
                           TeleportManager teleportManager,
                           InviteManager inviteManager,
                           GuiManager guiManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, FactionMainData.CODEC);
        this.playerRef = playerRef;
        this.factionManager = factionManager;
        this.claimManager = claimManager;
        this.powerManager = powerManager;
        this.teleportManager = teleportManager;
        this.inviteManager = inviteManager;
        this.guiManager = guiManager;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        UUID uuid = playerRef.getUuid();
        Faction faction = factionManager.getPlayerFaction(uuid);

        // Load the main template
        cmd.append("HyperFactions/faction_main.ui");

        // Check for pending invites
        List<PendingInvite> invites = inviteManager.getPlayerInvites(uuid);
        PendingInvite invite = invites.isEmpty() ? null : invites.get(0);
        if (invite != null) {
            Faction invitingFaction = factionManager.getFaction(invite.factionId());
            if (invitingFaction != null) {
                cmd.append("#InviteNotification", "HyperFactions/invite_notification.ui");
                cmd.set("#InviteNotification #InviteFactionName.Text", invitingFaction.name());

                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        "#InviteNotification #AcceptInviteBtn",
                        EventData.of("Button", "AcceptInvite")
                                .append("FactionId", invite.factionId().toString()),
                        false
                );

                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        "#InviteNotification #DeclineInviteBtn",
                        EventData.of("Button", "DeclineInvite")
                                .append("FactionId", invite.factionId().toString()),
                        false
                );
            }
        }

        if (faction == null) {
            // Player has no faction - show "no faction" state
            buildNoFactionView(cmd, events);
        } else {
            // Player has a faction - show dashboard
            buildFactionDashboard(cmd, events, faction, uuid);
        }
    }

    private void buildNoFactionView(UICommandBuilder cmd, UIEventBuilder events) {
        cmd.set("#FactionName.Text", "No Faction");

        // Show create button
        cmd.append("#ActionArea", "HyperFactions/no_faction_actions.ui");

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ActionArea #CreateFactionBtn",
                EventData.of("Button", "CreateFaction"),
                false
        );

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ActionArea #BrowseFactionsBtn",
                EventData.of("Button", "BrowseFactions"),
                false
        );
    }

    private void buildFactionDashboard(UICommandBuilder cmd, UIEventBuilder events,
                                       Faction faction, UUID playerUuid) {
        FactionMember member = faction.getMember(playerUuid);
        FactionRole role = member != null ? member.role() : FactionRole.MEMBER;
        boolean isLeader = role == FactionRole.LEADER;
        boolean isOfficer = role.getLevel() >= FactionRole.OFFICER.getLevel();

        // Set faction name with color
        String colorHex = faction.color() != null ? faction.color() : "#00FFFF";
        cmd.set("#FactionName.Text", faction.name());

        // Power stats
        PowerManager.FactionPowerStats powerStats = powerManager.getFactionPowerStats(faction.id());
        cmd.set("#PowerValue.Text", String.format("%.0f / %.0f", powerStats.currentPower(), powerStats.maxPower()));

        // Claim stats
        int claimCount = faction.claims().size();
        int claimCapacity = powerStats.maxClaims();
        cmd.set("#ClaimsValue.Text", String.format("%d / %d", claimCount, claimCapacity));

        // Member count
        cmd.set("#MembersValue.Text", String.valueOf(faction.members().size()));

        // Relation counts
        long allyCount = faction.relations().values().stream()
                .filter(r -> r.type() == RelationType.ALLY).count();
        long enemyCount = faction.relations().values().stream()
                .filter(r -> r.type() == RelationType.ENEMY).count();
        cmd.set("#AlliesValue.Text", String.valueOf(allyCount));
        cmd.set("#EnemiesValue.Text", String.valueOf(enemyCount));

        // Role display
        cmd.set("#RoleValue.Text", role.name());

        // Quick action buttons
        cmd.append("#ActionArea", "HyperFactions/faction_actions.ui");

        // Home button
        if (faction.home() != null) {
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#ActionArea #HomeBtn",
                    EventData.of("Button", "Home"),
                    false
            );
        }

        // Members button
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ActionArea #MembersBtn",
                EventData.of("Button", "Members"),
                false
        );

        // Map button
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ActionArea #MapBtn",
                EventData.of("Button", "Map"),
                false
        );

        // Relations button
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ActionArea #RelationsBtn",
                EventData.of("Button", "Relations"),
                false
        );

        // Leave button (for non-leaders)
        if (!isLeader) {
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#ActionArea #LeaveBtn",
                    EventData.of("Button", "Leave"),
                    false
            );
        }

        // Disband button (for leader only)
        if (isLeader) {
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#ActionArea #DisbandBtn",
                    EventData.of("Button", "Disband"),
                    false
            );
        }
    }

    private String getRoleColor(FactionRole role) {
        return switch (role) {
            case LEADER -> "#FFD700";  // Gold
            case OFFICER -> "#87CEEB"; // Sky blue
            case MEMBER -> "#AAAAAA";  // Gray
        };
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                FactionMainData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null || data.button == null) {
            return;
        }

        UUID uuid = playerRef.getUuid();
        Faction faction = factionManager.getPlayerFaction(uuid);

        switch (data.button) {
            case "CreateFaction" -> {
                guiManager.closePage(player, ref, store);
                player.sendMessage(Message.raw("Use /f create <name> to create a faction.").color("#00FFFF"));
            }

            case "BrowseFactions" -> guiManager.openFactionBrowser(player, ref, store, playerRef);

            case "AcceptInvite" -> {
                if (data.factionId != null) {
                    try {
                        UUID factionId = UUID.fromString(data.factionId);
                        PendingInvite pendingInvite = inviteManager.getInvite(factionId, uuid);
                        if (pendingInvite != null) {
                            FactionManager.FactionResult result = factionManager.addMember(
                                    factionId, uuid, playerRef.getUsername()
                            );
                            if (result == FactionManager.FactionResult.SUCCESS) {
                                inviteManager.removeInvite(factionId, uuid);
                                player.sendMessage(Message.raw("You joined the faction!").color("#44CC44"));
                                // Refresh the page
                                guiManager.openFactionMain(player, ref, store, playerRef);
                            } else {
                                player.sendMessage(Message.raw("Failed to join faction: " + result).color("#FF5555"));
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(Message.raw("Invalid faction ID.").color("#FF5555"));
                    }
                }
            }

            case "DeclineInvite" -> {
                if (data.factionId != null) {
                    try {
                        UUID factionId = UUID.fromString(data.factionId);
                        inviteManager.removeInvite(factionId, uuid);
                        player.sendMessage(Message.raw("Invite declined.").color("#FFAA00"));
                        // Refresh the page
                        guiManager.openFactionMain(player, ref, store, playerRef);
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(Message.raw("Invalid faction ID.").color("#FF5555"));
                    }
                }
            }

            case "Home" -> {
                if (faction != null && faction.home() != null) {
                    guiManager.closePage(player, ref, store);
                    player.sendMessage(Message.raw("Use /f home to teleport to your faction home.").color("#00FFFF"));
                }
            }

            case "Members" -> {
                if (faction != null) {
                    guiManager.openFactionMembers(player, ref, store, playerRef, faction);
                }
            }

            case "Map" -> guiManager.openChunkMap(player, ref, store, playerRef);

            case "Relations" -> {
                if (faction != null) {
                    guiManager.openFactionRelations(player, ref, store, playerRef, faction);
                }
            }

            case "Leave" -> {
                if (faction != null) {
                    FactionManager.FactionResult result = factionManager.removeMember(faction.id(), uuid, uuid, false);
                    if (result == FactionManager.FactionResult.SUCCESS) {
                        player.sendMessage(Message.raw("You left the faction.").color("#FFAA00"));
                        guiManager.openFactionMain(player, ref, store, playerRef);
                    } else {
                        player.sendMessage(Message.raw("Failed to leave: " + result).color("#FF5555"));
                    }
                }
            }

            case "Disband" -> {
                if (faction != null) {
                    FactionMember member = faction.getMember(uuid);
                    if (member != null && member.role() == FactionRole.LEADER) {
                        guiManager.closePage(player, ref, store);
                        player.sendMessage(Message.raw("Use /f disband to confirm disbanding your faction.").color("#FF5555"));
                    }
                }
            }
        }
    }
}
