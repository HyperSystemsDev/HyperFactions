package com.hyperfactions.gui.page;

import com.hyperfactions.data.*;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.data.FactionMembersData;
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
 */
public class FactionMembersPage extends InteractiveCustomUIPage<FactionMembersData> {

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
        super(playerRef, CustomPageLifetime.CanDismiss, FactionMembersData.CODEC);
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
        cmd.append("HyperFactions/faction_members.ui");

        // Set title
        cmd.set("#Title #TitleText.Text", faction.name() + " - Members");

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
        for (int i = 0; i < MEMBERS_PER_PAGE; i++) {
            String entryId = "#MemberEntry" + i;
            int memberIdx = startIdx + i;

            if (memberIdx < sortedMembers.size()) {
                FactionMember member = sortedMembers.get(memberIdx);
                boolean isSelf = member.uuid().equals(viewerUuid);
                boolean canManageThis = canManage && !isSelf &&
                        viewerRole.getLevel() > member.role().getLevel();

                // Append entry template
                cmd.append(entryId, "HyperFactions/member_entry.ui");

                String prefix = entryId + " ";

                // Member info
                cmd.set(prefix + "#MemberName.Text", member.username());
                cmd.set(prefix + "#MemberName.Style.TextColor", getRoleColor(member.role()));
                cmd.set(prefix + "#MemberRole.Text", member.role().name());
                cmd.set(prefix + "#MemberRole.Style.TextColor", getRoleColor(member.role()));

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
                                        .append("MemberUuid", member.uuid().toString())
                                        .append("MemberName", member.username()),
                                false
                        );
                    }

                    // Demote button (only for officers, leader can demote)
                    if (member.role() == FactionRole.OFFICER && isLeader) {
                        events.addEventBinding(
                                CustomUIEventBindingType.Activating,
                                prefix + "#DemoteBtn",
                                EventData.of("Button", "Demote")
                                        .append("MemberUuid", member.uuid().toString())
                                        .append("MemberName", member.username()),
                                false
                        );
                    }

                    // Kick button
                    events.addEventBinding(
                            CustomUIEventBindingType.Activating,
                            prefix + "#KickBtn",
                            EventData.of("Button", "Kick")
                                    .append("MemberUuid", member.uuid().toString())
                                    .append("MemberName", member.username()),
                            false
                    );
                }

                // Transfer leadership button (leader only, not self)
                if (isLeader && !isSelf) {
                    events.addEventBinding(
                            CustomUIEventBindingType.Activating,
                            prefix + "#TransferBtn",
                            EventData.of("Button", "Transfer")
                                    .append("MemberUuid", member.uuid().toString())
                                    .append("MemberName", member.username()),
                            false
                    );
                }
            }
        }

        // Pagination
        cmd.set("#PageInfo.Text", (currentPage + 1) + "/" + totalPages);

        if (currentPage > 0) {
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#PrevBtn",
                    EventData.of("Button", "PrevPage")
                            .append("Page", String.valueOf(currentPage - 1)),
                    false
            );
        }

        if (currentPage < totalPages - 1) {
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#NextBtn",
                    EventData.of("Button", "NextPage")
                            .append("Page", String.valueOf(currentPage + 1)),
                    false
            );
        }

        // Back button
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#BackBtn",
                EventData.of("Button", "Back"),
                false
        );
    }

    private String getRoleColor(FactionRole role) {
        return switch (role) {
            case LEADER -> "#FFD700";
            case OFFICER -> "#87CEEB";
            case MEMBER -> "#AAAAAA";
        };
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
                                FactionMembersData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null || data.button == null) {
            return;
        }

        UUID viewerUuid = playerRef.getUuid();

        switch (data.button) {
            case "Back" -> guiManager.openFactionMain(player, ref, store, playerRef);

            case "PrevPage" -> {
                currentPage = Math.max(0, data.page);
                refresh(player, ref, store, playerRef);
            }

            case "NextPage" -> {
                currentPage = data.page;
                refresh(player, ref, store, playerRef);
            }

            case "Promote" -> {
                if (data.memberUuid != null) {
                    try {
                        UUID targetUuid = UUID.fromString(data.memberUuid);
                        UUID actorUuid = playerRef.getUuid();
                        FactionManager.FactionResult result = factionManager.promoteMember(
                                faction.id(), targetUuid, actorUuid
                        );
                        if (result == FactionManager.FactionResult.SUCCESS) {
                            player.sendMessage(Message.raw(data.memberName + " promoted to Officer!").color("#44CC44"));
                        } else {
                            player.sendMessage(Message.raw("Failed to promote: " + result).color("#FF5555"));
                        }
                        refresh(player, ref, store, playerRef);
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(Message.raw("Invalid player.").color("#FF5555"));
                    }
                }
            }

            case "Demote" -> {
                if (data.memberUuid != null) {
                    try {
                        UUID targetUuid = UUID.fromString(data.memberUuid);
                        UUID actorUuid = playerRef.getUuid();
                        FactionManager.FactionResult result = factionManager.demoteMember(
                                faction.id(), targetUuid, actorUuid
                        );
                        if (result == FactionManager.FactionResult.SUCCESS) {
                            player.sendMessage(Message.raw(data.memberName + " demoted to Member.").color("#FFAA00"));
                        } else {
                            player.sendMessage(Message.raw("Failed to demote: " + result).color("#FF5555"));
                        }
                        refresh(player, ref, store, playerRef);
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(Message.raw("Invalid player.").color("#FF5555"));
                    }
                }
            }

            case "Kick" -> {
                if (data.memberUuid != null) {
                    try {
                        UUID targetUuid = UUID.fromString(data.memberUuid);
                        UUID actorUuid = playerRef.getUuid();
                        FactionManager.FactionResult result = factionManager.removeMember(faction.id(), targetUuid, actorUuid, true);
                        if (result == FactionManager.FactionResult.SUCCESS) {
                            player.sendMessage(Message.raw(data.memberName + " kicked from faction.").color("#FF5555"));
                        } else {
                            player.sendMessage(Message.raw("Failed to kick: " + result).color("#FF5555"));
                        }
                        refresh(player, ref, store, playerRef);
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(Message.raw("Invalid player.").color("#FF5555"));
                    }
                }
            }

            case "Transfer" -> {
                if (data.memberUuid != null) {
                    guiManager.closePage(player, ref, store);
                    player.sendMessage(Message.raw("Use /f transfer " + data.memberName + " to confirm leadership transfer.").color("#FFAA00"));
                }
            }
        }
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
