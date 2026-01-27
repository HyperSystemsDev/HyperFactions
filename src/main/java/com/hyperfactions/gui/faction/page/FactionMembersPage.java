package com.hyperfactions.gui.faction.page;

import com.hyperfactions.data.*;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.nav.NavBarHelper;
import com.hyperfactions.gui.faction.data.FactionPageData;
import com.hyperfactions.manager.FactionManager;
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
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.*;

/**
 * Faction Members page - displays member list with management actions.
 * Uses the unified FactionPageData for event handling.
 */
public class FactionMembersPage extends InteractiveCustomUIPage<FactionPageData> {

    private static final String PAGE_ID = "members";
    private static final int MEMBERS_PER_PAGE = 8;

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final GuiManager guiManager;
    private final Faction faction;
    private int currentPage = 0;

    public FactionMembersPage(PlayerRef playerRef,
                              FactionManager factionManager,
                              GuiManager guiManager,
                              Faction faction) {
        super(playerRef, CustomPageLifetime.CanDismiss, FactionPageData.CODEC);
        this.playerRef = playerRef;
        this.factionManager = factionManager;
        this.guiManager = guiManager;
        this.faction = faction;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        UUID viewerUuid = playerRef.getUuid();
        FactionMember viewer = faction.getMember(viewerUuid);
        FactionRole viewerRole = viewer != null ? viewer.role() : FactionRole.MEMBER;
        boolean canManage = viewerRole.getLevel() >= FactionRole.OFFICER.getLevel();
        boolean isLeader = viewerRole == FactionRole.LEADER;

        // Load the main template
        cmd.append("HyperFactions/faction/faction_members.ui");

        // Setup navigation bar (AdminUI pattern with indexed selectors)
        NavBarHelper.setupBar(playerRef, true, PAGE_ID, cmd, events);

        // Sort members by role priority (leader first) then by name
        List<FactionMember> sortedMembers = faction.members().values().stream()
                .sorted(Comparator
                        .<FactionMember>comparingInt(m -> -m.role().getLevel())
                        .thenComparing(FactionMember::username))
                .toList();

        // Member count
        cmd.set("#MemberCount.Text", sortedMembers.size() + " members");

        // Calculate pagination
        int totalPages = Math.max(1, (int) Math.ceil((double) sortedMembers.size() / MEMBERS_PER_PAGE));
        currentPage = Math.min(currentPage, totalPages - 1);
        int startIdx = currentPage * MEMBERS_PER_PAGE;

        // Build member entries
        buildMemberEntries(cmd, events, sortedMembers, startIdx, viewerUuid, viewerRole, canManage, isLeader);

        // Pagination
        setupPagination(cmd, events, totalPages);
    }

    private void buildMemberEntries(UICommandBuilder cmd, UIEventBuilder events,
                                    List<FactionMember> sortedMembers, int startIdx,
                                    UUID viewerUuid, FactionRole viewerRole,
                                    boolean canManage, boolean isLeader) {
        for (int i = 0; i < MEMBERS_PER_PAGE; i++) {
            String entryId = "#MemberEntry" + i;
            int memberIdx = startIdx + i;

            if (memberIdx < sortedMembers.size()) {
                FactionMember member = sortedMembers.get(memberIdx);
                boolean isSelf = member.uuid().equals(viewerUuid);
                boolean canManageThis = canManage && !isSelf &&
                        viewerRole.getLevel() > member.role().getLevel();

                // Append entry template
                cmd.append(entryId, "HyperFactions/faction/member_entry.ui");

                String prefix = entryId + " ";

                // Member info
                cmd.set(prefix + "#MemberName.Text", member.username());
                cmd.set(prefix + "#MemberRole.Text", member.role().name());

                // Last online
                String lastOnline = formatLastOnline(member.lastOnline());
                cmd.set(prefix + "#LastOnline.Text", lastOnline);

                // Action buttons (only if viewer can manage this member)
                if (canManageThis) {
                    // Promote button (not for officers if viewer is officer)
                    if (member.role() == FactionRole.MEMBER && canManage) {
                        events.addEventBinding(
                                CustomUIEventBindingType.Activating,
                                prefix + "#PromoteBtn",
                                EventData.of("Button", "Promote")
                                        .append("PlayerUuid", member.uuid().toString())
                                        .append("Target", member.username()),
                                false
                        );
                    }

                    // Demote button (only for officers, leader can demote)
                    if (member.role() == FactionRole.OFFICER && isLeader) {
                        events.addEventBinding(
                                CustomUIEventBindingType.Activating,
                                prefix + "#DemoteBtn",
                                EventData.of("Button", "Demote")
                                        .append("PlayerUuid", member.uuid().toString())
                                        .append("Target", member.username()),
                                false
                        );
                    }

                    // Kick button
                    events.addEventBinding(
                            CustomUIEventBindingType.Activating,
                            prefix + "#KickBtn",
                            EventData.of("Button", "Kick")
                                    .append("PlayerUuid", member.uuid().toString())
                                    .append("Target", member.username()),
                            false
                    );
                }

                // Transfer leadership button (leader only, not self)
                if (isLeader && !isSelf) {
                    events.addEventBinding(
                            CustomUIEventBindingType.Activating,
                            prefix + "#TransferBtn",
                            EventData.of("Button", "Transfer")
                                    .append("PlayerUuid", member.uuid().toString())
                                    .append("Target", member.username()),
                            false
                    );
                }
            }
        }
    }

    private void setupPagination(UICommandBuilder cmd, UIEventBuilder events, int totalPages) {
        cmd.set("#PageInfo.Text", (currentPage + 1) + "/" + totalPages);

        if (currentPage > 0) {
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#PrevBtn",
                    EventData.of("Button", "Page").append("Page", String.valueOf(currentPage - 1)),
                    false
            );
        }

        if (currentPage < totalPages - 1) {
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#NextBtn",
                    EventData.of("Button", "Page").append("Page", String.valueOf(currentPage + 1)),
                    false
            );
        }
    }

    private String formatLastOnline(long lastOnlineMs) {
        if (lastOnlineMs <= 0) {
            return "Unknown";
        }
        long diffMs = System.currentTimeMillis() - lastOnlineMs;
        if (diffMs < 60000) {
            return "Online now";
        }
        return TimeUtil.formatDuration(diffMs) + " ago";
    }

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
        if (NavBarHelper.handleNavEvent(data, player, ref, store, playerRef, faction, guiManager)) {
            return;
        }

        switch (data.button) {
            case "Page" -> {
                currentPage = data.page;
                refresh(player, ref, store, playerRef);
            }

            case "Promote" -> handlePromote(player, ref, store, playerRef, data);

            case "Demote" -> handleDemote(player, ref, store, playerRef, data);

            case "Kick" -> handleKick(player, ref, store, playerRef, data);

            case "Transfer" -> handleTransfer(player, ref, store, data);

            default -> sendUpdate();
        }
    }

    private void handlePromote(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                               PlayerRef playerRef, FactionPageData data) {
        if (data.playerUuid == null) return;

        try {
            UUID targetUuid = UUID.fromString(data.playerUuid);
            UUID actorUuid = playerRef.getUuid();
            FactionManager.FactionResult result = factionManager.promoteMember(
                    faction.id(), targetUuid, actorUuid
            );
            if (result == FactionManager.FactionResult.SUCCESS) {
                player.sendMessage(Message.raw(data.target + " promoted to Officer!").color("#44CC44"));
            } else {
                player.sendMessage(Message.raw("Failed to promote: " + result).color("#FF5555"));
            }
            refresh(player, ref, store, playerRef);
        } catch (IllegalArgumentException e) {
            player.sendMessage(Message.raw("Invalid player.").color("#FF5555"));
        }
    }

    private void handleDemote(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                              PlayerRef playerRef, FactionPageData data) {
        if (data.playerUuid == null) return;

        try {
            UUID targetUuid = UUID.fromString(data.playerUuid);
            UUID actorUuid = playerRef.getUuid();
            FactionManager.FactionResult result = factionManager.demoteMember(
                    faction.id(), targetUuid, actorUuid
            );
            if (result == FactionManager.FactionResult.SUCCESS) {
                player.sendMessage(Message.raw(data.target + " demoted to Member.").color("#FFAA00"));
            } else {
                player.sendMessage(Message.raw("Failed to demote: " + result).color("#FF5555"));
            }
            refresh(player, ref, store, playerRef);
        } catch (IllegalArgumentException e) {
            player.sendMessage(Message.raw("Invalid player.").color("#FF5555"));
        }
    }

    private void handleKick(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                            PlayerRef playerRef, FactionPageData data) {
        if (data.playerUuid == null) return;

        try {
            UUID targetUuid = UUID.fromString(data.playerUuid);
            UUID actorUuid = playerRef.getUuid();
            FactionManager.FactionResult result = factionManager.removeMember(
                    faction.id(), targetUuid, actorUuid, true
            );
            if (result == FactionManager.FactionResult.SUCCESS) {
                player.sendMessage(Message.raw(data.target + " kicked from faction.").color("#FF5555"));
            } else {
                player.sendMessage(Message.raw("Failed to kick: " + result).color("#FF5555"));
            }
            refresh(player, ref, store, playerRef);
        } catch (IllegalArgumentException e) {
            player.sendMessage(Message.raw("Invalid player.").color("#FF5555"));
        }
    }

    private void handleTransfer(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                                FactionPageData data) {
        if (data.playerUuid == null) return;

        guiManager.closePage(player, ref, store);
        player.sendMessage(Message.raw("Use /f transfer " + data.target + " to confirm leadership transfer.").color("#FFAA00"));
    }

    private void refresh(Player player, Ref<EntityStore> ref, Store<EntityStore> store, PlayerRef playerRef) {
        // Get fresh faction data
        Faction freshFaction = factionManager.getFaction(faction.id());
        if (freshFaction != null) {
            guiManager.openFactionMembers(player, ref, store, playerRef, freshFaction);
        } else {
            guiManager.openFactionMain(player, ref, store, playerRef);
        }
    }
}
