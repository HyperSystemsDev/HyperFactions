package com.hyperfactions.gui.faction.page;

import com.hyperfactions.data.*;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.nav.NavBarHelper;
import com.hyperfactions.gui.faction.data.FactionMembersData;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.PowerManager;
import com.hyperfactions.util.TimeUtil;
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
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Faction Members page - displays member list with expandable entries.
 * Uses AdminUI pattern exactly.
 */
public class FactionMembersPage extends InteractiveCustomUIPage<FactionMembersData> {

    private static final String PAGE_ID = "members";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy")
            .withZone(ZoneId.systemDefault());

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final PowerManager powerManager;
    private final GuiManager guiManager;
    private Faction faction;

    private Set<UUID> expandedMembers = new HashSet<>();
    private SortMode sortMode = SortMode.ROLE;

    private enum SortMode {
        ROLE,
        LAST_ONLINE
    }

    public FactionMembersPage(PlayerRef playerRef,
                              FactionManager factionManager,
                              PowerManager powerManager,
                              GuiManager guiManager,
                              Faction faction) {
        super(playerRef, CustomPageLifetime.CanDismiss, FactionMembersData.CODEC);
        this.playerRef = playerRef;
        this.factionManager = factionManager;
        this.powerManager = powerManager;
        this.guiManager = guiManager;
        this.faction = faction;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        // Load the main template
        cmd.append("HyperFactions/faction/faction_members.ui");

        // Setup navigation bar
        NavBarHelper.setupBar(playerRef, true, PAGE_ID, cmd, events);

        // Build member list
        buildMemberList(cmd, events);
    }

    private void buildMemberList(UICommandBuilder cmd, UIEventBuilder events) {
        List<FactionMember> members = getFilteredSortedMembers();

        cmd.set("#MemberCount.Text", members.size() + " members");

        // Sort buttons
        cmd.set("#SortByRole.Disabled", sortMode == SortMode.ROLE);
        cmd.set("#SortByOnline.Disabled", sortMode == SortMode.LAST_ONLINE);

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#SortByRole",
                EventData.of("Button", "SortByRole"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#SortByOnline",
                EventData.of("Button", "SortByOnline"),
                false
        );

        // Clear MembersList (exists in template), then create IndexCards container inside it
        cmd.clear("#MembersList");
        cmd.appendInline("#MembersList", "Group #IndexCards { LayoutMode: Top; }");

        // Build entries
        int i = 0;
        for (FactionMember member : members) {
            buildMemberEntry(cmd, events, i, member);
            i++;
        }
    }

    private void buildMemberEntry(UICommandBuilder cmd, UIEventBuilder events, int index,
                                   FactionMember member) {
        boolean isExpanded = expandedMembers.contains(member.uuid());
        boolean memberIsOnline = isOnline(member);

        // Append entry template to IndexCards
        cmd.append("#IndexCards", "HyperFactions/faction/member_entry.ui");

        // Use indexed selector like NavBarHelper does
        String idx = "#IndexCards[" + index + "]";

        // Basic info
        cmd.set(idx + " #MemberName.Text", member.username());
        cmd.set(idx + " #MemberRole.Text", formatRole(member.role()));

        // Role indicator color
        cmd.set(idx + " #RoleIndicator.Background.Color", getRoleColor(member.role()));

        // Online status
        if (memberIsOnline) {
            cmd.set(idx + " #OnlineStatus.Text", "Online");
            cmd.set(idx + " #OnlineStatus.Style.TextColor", "#55FF55");
            cmd.set(idx + " #LastOnline.Text", "");
        } else {
            cmd.set(idx + " #OnlineStatus.Text", "Offline");
            cmd.set(idx + " #OnlineStatus.Style.TextColor", "#888888");
            cmd.set(idx + " #LastOnline.Text", formatLastOnline(member.lastOnline()));
        }

        // Expansion state
        cmd.set(idx + " #ExpandIcon.Visible", !isExpanded);
        cmd.set(idx + " #CollapseIcon.Visible", isExpanded);
        cmd.set(idx + " #ExtendedInfo.Visible", isExpanded);

        // Bind to #Header TextButton inside the indexed element (like NavBarHelper pattern)
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                idx + " #Header",
                EventData.of("Button", "ToggleExpanded")
                        .append("PlayerUuid", member.uuid().toString()),
                false
        );

        // Extended info
        if (isExpanded) {
            // Power info
            PlayerPower power = powerManager.getPlayerPower(member.uuid());
            String powerText = String.format("%.0f/%.0f", power.power(), power.maxPower());
            cmd.set(idx + " #PowerValue.Text", powerText);
            // Color based on power percentage
            int powerPercent = power.getPowerPercent();
            String powerColor = powerPercent >= 80 ? "#55FF55" : powerPercent >= 40 ? "#FFAA00" : "#FF5555";
            cmd.set(idx + " #PowerValue.Style.TextColor", powerColor);

            // Joined date
            String joinedDate = member.joinedAt() > 0
                    ? DATE_FORMAT.format(Instant.ofEpochMilli(member.joinedAt()))
                    : "Unknown";
            cmd.set(idx + " #JoinedDate.Text", joinedDate);

            // Last death (relative format)
            String lastDeathText = power.lastDeath() > 0
                    ? TimeUtil.formatDuration(System.currentTimeMillis() - power.lastDeath()) + " ago"
                    : "Never";
            cmd.set(idx + " #LastDeath.Text", lastDeathText);

            // Determine what actions the viewer can take on this member
            FactionMember viewer = faction.members().get(playerRef.getUuid());
            boolean isSelf = member.uuid().equals(playerRef.getUuid());
            boolean viewerIsLeader = viewer != null && viewer.role() == FactionRole.LEADER;
            boolean viewerIsOfficer = viewer != null && viewer.role() == FactionRole.OFFICER;
            boolean targetIsMember = member.role() == FactionRole.MEMBER;
            boolean targetIsOfficer = member.role() == FactionRole.OFFICER;

            // Show "(You)" label for self
            cmd.set(idx + " #SelfLabel.Visible", isSelf);

            // Promote: Leader can promote Members to Officer
            boolean canPromote = viewerIsLeader && targetIsMember && !isSelf;
            cmd.set(idx + " #PromoteBtn.Visible", canPromote);
            cmd.set(idx + " #PromoteSpacer.Visible", canPromote);
            if (canPromote) {
                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        idx + " #PromoteBtn",
                        EventData.of("Button", "Promote")
                                .append("PlayerUuid", member.uuid().toString()),
                        false
                );
            }

            // Demote: Leader can demote Officers to Member
            boolean canDemote = viewerIsLeader && targetIsOfficer && !isSelf;
            cmd.set(idx + " #DemoteBtn.Visible", canDemote);
            cmd.set(idx + " #DemoteSpacer.Visible", canDemote);
            if (canDemote) {
                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        idx + " #DemoteBtn",
                        EventData.of("Button", "Demote")
                                .append("PlayerUuid", member.uuid().toString()),
                        false
                );
            }

            // Kick: Leader can kick anyone, Officer can kick Members
            boolean canKick = !isSelf && (viewerIsLeader || (viewerIsOfficer && targetIsMember));
            cmd.set(idx + " #KickBtn.Visible", canKick);
            cmd.set(idx + " #KickSpacer.Visible", canKick);
            if (canKick) {
                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        idx + " #KickBtn",
                        EventData.of("Button", "Kick")
                                .append("PlayerUuid", member.uuid().toString()),
                        false
                );
            }

            // Transfer: Leader can transfer leadership to anyone else
            boolean canTransfer = viewerIsLeader && !isSelf;
            cmd.set(idx + " #TransferBtn.Visible", canTransfer);
            if (canTransfer) {
                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        idx + " #TransferBtn",
                        EventData.of("Button", "Transfer")
                                .append("PlayerUuid", member.uuid().toString()),
                        false
                );
            }
        }
    }

    private List<FactionMember> getFilteredSortedMembers() {
        return faction.members().values().stream()
                .sorted(getSortComparator())
                .toList();
    }

    private Comparator<FactionMember> getSortComparator() {
        if (sortMode == SortMode.LAST_ONLINE) {
            return Comparator
                    .<FactionMember>comparingLong(m -> isOnline(m) ? Long.MAX_VALUE : m.lastOnline())
                    .reversed()
                    .thenComparing(FactionMember::username);
        } else {
            return Comparator
                    .<FactionMember>comparingInt(m -> -m.role().getLevel())
                    .thenComparing(FactionMember::username);
        }
    }

    private boolean isOnline(FactionMember member) {
        PlayerRef onlinePlayer = Universe.get().getPlayer(member.uuid());
        return onlinePlayer != null && onlinePlayer.isValid();
    }

    private String formatRole(FactionRole role) {
        return switch (role) {
            case LEADER -> "Leader";
            case OFFICER -> "Officer";
            case MEMBER -> "Member";
        };
    }

    private String getRoleColor(FactionRole role) {
        return switch (role) {
            case LEADER -> "#FFD700";
            case OFFICER -> "#00AAFF";
            case MEMBER -> "#888888";
        };
    }

    private String formatLastOnline(long lastOnlineMs) {
        if (lastOnlineMs <= 0) {
            return "";
        }
        long diffMs = System.currentTimeMillis() - lastOnlineMs;
        if (diffMs < 60000) {
            return "just now";
        }
        return TimeUtil.formatDuration(diffMs) + " ago";
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                FactionMembersData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null) {
            sendUpdate();
            return;
        }

        // Handle navigation - FactionMembersData now implements NavAwareData
        if (NavBarHelper.handleNavEvent(data, player, ref, store, playerRef, faction, guiManager)) {
            return;
        }

        if (data.button == null) {
            sendUpdate();
            return;
        }

        switch (data.button) {
            case "ToggleExpanded" -> {
                if (data.playerUuid != null) {
                    try {
                        UUID uuid = UUID.fromString(data.playerUuid);
                        if (expandedMembers.contains(uuid)) {
                            expandedMembers.remove(uuid);
                        } else {
                            expandedMembers.add(uuid);
                        }
                        rebuildList(ref, store);
                    } catch (IllegalArgumentException e) {
                        sendUpdate();
                    }
                }
            }

            case "SortByRole" -> {
                sortMode = SortMode.ROLE;
                rebuildList(ref, store);
            }

            case "SortByOnline" -> {
                sortMode = SortMode.LAST_ONLINE;
                rebuildList(ref, store);
            }

            case "Promote" -> handlePromote(player, ref, store, data.playerUuid);
            case "Demote" -> handleDemote(player, ref, store, data.playerUuid);
            case "Kick" -> handleKick(player, ref, store, data.playerUuid);
            case "Transfer" -> handleTransfer(player, ref, store, data.playerUuid);

            default -> sendUpdate();
        }
    }

    private void handlePromote(Player player, Ref<EntityStore> ref, Store<EntityStore> store, String targetUuidStr) {
        if (targetUuidStr == null) {
            sendUpdate();
            return;
        }

        try {
            UUID targetUuid = UUID.fromString(targetUuidStr);
            FactionMember target = faction.members().get(targetUuid);
            if (target == null) {
                player.sendMessage(Message.raw("Member not found.").color("#FF5555"));
                sendUpdate();
                return;
            }

            var result = factionManager.promoteMember(faction.id(), targetUuid, playerRef.getUuid());
            if (result == FactionManager.FactionResult.SUCCESS) {
                player.sendMessage(Message.raw("Promoted " + target.username() + " to Officer.").color("#55FF55"));
            } else {
                player.sendMessage(Message.raw("Failed to promote: " + result.name()).color("#FF5555"));
            }
            rebuildList(ref, store);
        } catch (IllegalArgumentException e) {
            sendUpdate();
        }
    }

    private void handleDemote(Player player, Ref<EntityStore> ref, Store<EntityStore> store, String targetUuidStr) {
        if (targetUuidStr == null) {
            sendUpdate();
            return;
        }

        try {
            UUID targetUuid = UUID.fromString(targetUuidStr);
            FactionMember target = faction.members().get(targetUuid);
            if (target == null) {
                player.sendMessage(Message.raw("Member not found.").color("#FF5555"));
                sendUpdate();
                return;
            }

            var result = factionManager.demoteMember(faction.id(), targetUuid, playerRef.getUuid());
            if (result == FactionManager.FactionResult.SUCCESS) {
                player.sendMessage(Message.raw("Demoted " + target.username() + " to Member.").color("#55FF55"));
            } else {
                player.sendMessage(Message.raw("Failed to demote: " + result.name()).color("#FF5555"));
            }
            rebuildList(ref, store);
        } catch (IllegalArgumentException e) {
            sendUpdate();
        }
    }

    private void handleKick(Player player, Ref<EntityStore> ref, Store<EntityStore> store, String targetUuidStr) {
        if (targetUuidStr == null) {
            sendUpdate();
            return;
        }

        try {
            UUID targetUuid = UUID.fromString(targetUuidStr);
            FactionMember target = faction.members().get(targetUuid);
            if (target == null) {
                player.sendMessage(Message.raw("Member not found.").color("#FF5555"));
                sendUpdate();
                return;
            }

            var result = factionManager.removeMember(faction.id(), targetUuid, playerRef.getUuid(), true);
            if (result == FactionManager.FactionResult.SUCCESS) {
                player.sendMessage(Message.raw("Kicked " + target.username() + " from the faction.").color("#55FF55"));
            } else {
                player.sendMessage(Message.raw("Failed to kick: " + result.name()).color("#FF5555"));
            }
            rebuildList(ref, store);
        } catch (IllegalArgumentException e) {
            sendUpdate();
        }
    }

    private void handleTransfer(Player player, Ref<EntityStore> ref, Store<EntityStore> store, String targetUuidStr) {
        if (targetUuidStr == null) {
            sendUpdate();
            return;
        }

        try {
            UUID targetUuid = UUID.fromString(targetUuidStr);
            FactionMember target = faction.members().get(targetUuid);
            if (target == null) {
                player.sendMessage(Message.raw("Member not found.").color("#FF5555"));
                sendUpdate();
                return;
            }

            // Open confirmation modal instead of directly transferring
            guiManager.openTransferConfirm(player, ref, store, playerRef, faction, targetUuid, target.username());
        } catch (IllegalArgumentException e) {
            sendUpdate();
        }
    }

    private void rebuildList(Ref<EntityStore> ref, Store<EntityStore> store) {
        Faction freshFaction = factionManager.getFaction(faction.id());
        if (freshFaction != null) {
            this.faction = freshFaction;
        }

        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        buildMemberList(cmd, events);

        sendUpdate(cmd, events, false);
    }
}
