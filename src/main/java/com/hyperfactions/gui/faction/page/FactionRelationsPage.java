package com.hyperfactions.gui.faction.page;

import com.hyperfactions.Permissions;
import com.hyperfactions.data.*;
import com.hyperfactions.gui.ActivePageTracker;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.RefreshablePage;
import com.hyperfactions.gui.nav.NavBarHelper;
import com.hyperfactions.integration.PermissionManager;
import com.hyperfactions.gui.faction.data.FactionRelationsData;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.RelationManager;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Faction Relations page - displays allies, enemies, and pending requests in tabs.
 * Uses tab-based filtering and expandable entries like AdminZonePage.
 */
public class FactionRelationsPage extends InteractiveCustomUIPage<FactionRelationsData> implements RefreshablePage {

    private static final String PAGE_ID = "relations";
    private static final int ITEMS_PER_PAGE = 8;

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final RelationManager relationManager;
    private final GuiManager guiManager;
    private final Faction faction;

    private Tab currentTab = Tab.ALLIES;
    private int currentPage = 0;
    private Set<UUID> expandedItems = new HashSet<>();

    private enum Tab {
        ALLIES,
        ENEMIES,
        PENDING
    }

    public FactionRelationsPage(PlayerRef playerRef,
                                FactionManager factionManager,
                                RelationManager relationManager,
                                GuiManager guiManager,
                                Faction faction) {
        super(playerRef, CustomPageLifetime.CanDismiss, FactionRelationsData.CODEC);
        this.playerRef = playerRef;
        this.factionManager = factionManager;
        this.relationManager = relationManager;
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

        // Load the main template
        cmd.append("HyperFactions/faction/faction_relations.ui");

        // Setup navigation bar
        NavBarHelper.setupBar(playerRef, faction, PAGE_ID, cmd, events);

        // Build the list
        buildList(cmd, events, canManage);

        // Register with active page tracker for real-time updates
        ActivePageTracker activeTracker = guiManager.getActivePageTracker();
        if (activeTracker != null) {
            activeTracker.register(playerRef.getUuid(), PAGE_ID, faction.id(), this);
        }
    }

    @Override
    public void refreshContent() {
        UUID viewerUuid = playerRef.getUuid();
        Faction currentFaction = factionManager.getFaction(faction.id());
        FactionMember viewer = currentFaction != null ? currentFaction.getMember(viewerUuid) : null;
        FactionRole viewerRole = viewer != null ? viewer.role() : FactionRole.MEMBER;
        boolean canManage = viewerRole.getLevel() >= FactionRole.OFFICER.getLevel();
        rebuildList(canManage);
    }

    private void buildList(UICommandBuilder cmd, UIEventBuilder events, boolean canManage) {
        // Tab buttons - highlight active tab using Disabled property
        cmd.set("#TabAllies.Disabled", currentTab == Tab.ALLIES);
        cmd.set("#TabEnemies.Disabled", currentTab == Tab.ENEMIES);
        cmd.set("#TabPending.Disabled", currentTab == Tab.PENDING);

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TabAllies",
                EventData.of("Button", "Tab").append("Tab", "ALLIES"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TabEnemies",
                EventData.of("Button", "Tab").append("Tab", "ENEMIES"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TabPending",
                EventData.of("Button", "Tab").append("Tab", "PENDING"),
                false
        );

        // Set Relation button (visible for officers+)
        if (canManage) {
            cmd.set("#SetRelationBtn.Visible", true);
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#SetRelationBtn",
                    EventData.of("Button", "SetRelation"),
                    false
            );
        } else {
            cmd.set("#SetRelationBtn.Visible", false);
        }

        // Get items based on current tab
        List<RelationItem> items = switch (currentTab) {
            case ALLIES -> getRelationsOfType(RelationType.ALLY);
            case ENEMIES -> getRelationsOfType(RelationType.ENEMY);
            case PENDING -> getPendingRequests();
        };

        // Count
        String countText = items.size() + " " + switch (currentTab) {
            case ALLIES -> items.size() == 1 ? "ally" : "allies";
            case ENEMIES -> items.size() == 1 ? "enemy" : "enemies";
            case PENDING -> items.size() == 1 ? "request" : "requests";
        };
        cmd.set("#ItemCount.Text", countText);

        // Calculate pagination
        int totalPages = Math.max(1, (int) Math.ceil((double) items.size() / ITEMS_PER_PAGE));
        currentPage = Math.min(currentPage, totalPages - 1);
        int startIdx = currentPage * ITEMS_PER_PAGE;

        // Clear list, create IndexCards container
        cmd.clear("#ItemList");
        cmd.appendInline("#ItemList", "Group #IndexCards { LayoutMode: Top; }");

        // Build entries
        int i = 0;
        for (int idx = startIdx; idx < Math.min(startIdx + ITEMS_PER_PAGE, items.size()); idx++) {
            RelationItem item = items.get(idx);
            buildEntry(cmd, events, i, item, canManage);
            i++;
        }

        // Show empty message if no items
        if (items.isEmpty()) {
            cmd.clear("#ItemList");
            String msg = getEmptyMessage(canManage);
            cmd.appendInline("#ItemList", "Group { LayoutMode: Top; Padding: (Top: 20); " +
                    "Label #EmptyText { Text: \"" + msg + "\"; " +
                    "Style: (FontSize: 12, TextColor: #666666, HorizontalAlignment: Center); } }");
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
    }

    private List<RelationItem> getRelationsOfType(RelationType targetType) {
        List<RelationItem> items = new ArrayList<>();

        for (FactionRelation relation : faction.relations().values()) {
            if (relation.type() == targetType) {
                Faction other = factionManager.getFaction(relation.targetFactionId());
                if (other != null) {
                    FactionMember leader = other.getLeader();
                    String leaderName = leader != null ? leader.username() : "Unknown";
                    String typeText = targetType == RelationType.ALLY ? "Ally" : "Enemy";
                    items.add(new RelationItem(
                            other.id(),
                            other.name(),
                            leaderName,
                            typeText,
                            relation.since(),
                            false,
                            false
                    ));
                }
            }
        }

        // Sort alphabetically
        items.sort(Comparator.comparing(RelationItem::factionName));
        return items;
    }

    private List<RelationItem> getPendingRequests() {
        List<RelationItem> items = new ArrayList<>();

        // Inbound requests (other factions requesting to ally with us)
        Set<UUID> inboundRequests = relationManager.getPendingRequests(faction.id());
        for (UUID requesterId : inboundRequests) {
            Faction requester = factionManager.getFaction(requesterId);
            if (requester != null) {
                FactionMember leader = requester.getLeader();
                String leaderName = leader != null ? leader.username() : "Unknown";
                items.add(new RelationItem(
                        requester.id(),
                        requester.name(),
                        leaderName,
                        "Incoming",
                        System.currentTimeMillis(),
                        true,
                        false
                ));
            }
        }

        // Outbound requests (requests we sent to other factions)
        Set<UUID> outboundRequests = relationManager.getOutboundRequests(faction.id());
        for (UUID targetId : outboundRequests) {
            Faction target = factionManager.getFaction(targetId);
            if (target != null) {
                FactionMember leader = target.getLeader();
                String leaderName = leader != null ? leader.username() : "Unknown";
                items.add(new RelationItem(
                        target.id(),
                        target.name(),
                        leaderName,
                        "Outgoing",
                        System.currentTimeMillis(),
                        false,
                        true
                ));
            }
        }

        // Sort alphabetically
        items.sort(Comparator.comparing(RelationItem::factionName));
        return items;
    }

    private void buildEntry(UICommandBuilder cmd, UIEventBuilder events, int index,
                            RelationItem item, boolean canManage) {
        boolean isExpanded = expandedItems.contains(item.factionId);

        // Append entry template
        cmd.append("#IndexCards", "HyperFactions/faction/faction_relation_entry.ui");

        String idx = "#IndexCards[" + index + "]";

        // Basic info
        cmd.set(idx + " #FactionName.Text", item.factionName);
        cmd.set(idx + " #LeaderName.Text", "Leader: " + item.leaderName);

        // Relation type badge with appropriate color
        cmd.set(idx + " #RelationType.Text", item.type);
        String typeColor = switch (item.type) {
            case "Ally" -> "#00AAFF";
            case "Enemy" -> "#FF5555";
            case "Incoming" -> "#FFAA00";
            case "Outgoing" -> "#88AAFF";
            default -> "#888888";
        };
        cmd.set(idx + " #RelationType.Style.TextColor", typeColor);

        // Expansion state
        cmd.set(idx + " #ExpandIcon.Visible", !isExpanded);
        cmd.set(idx + " #CollapseIcon.Visible", isExpanded);
        cmd.set(idx + " #ExtendedInfo.Visible", isExpanded);

        // Bind header click
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                idx + " #Header",
                EventData.of("Button", "ToggleExpanded")
                        .append("FactionId", item.factionId.toString()),
                false
        );

        // Extended info (only set if expanded)
        if (isExpanded) {
            // Since date
            cmd.set(idx + " #InfoLabel.Text", item.isIncoming || item.isOutbound ? "Requested:" : "Since:");
            cmd.set(idx + " #InfoValue.Text", formatDate(item.sinceMillis, item.isIncoming || item.isOutbound));

            // Hide all buttons initially
            cmd.set(idx + " #NeutralBtn.Visible", false);
            cmd.set(idx + " #EnemyBtn.Visible", false);
            cmd.set(idx + " #AllyBtn.Visible", false);
            cmd.set(idx + " #AcceptBtn.Visible", false);
            cmd.set(idx + " #DeclineBtn.Visible", false);
            cmd.set(idx + " #CancelBtn.Visible", false);

            if (canManage) {
                UUID viewerUuid = playerRef.getUuid();
                boolean hasNeutral = PermissionManager.get().hasPermission(viewerUuid, Permissions.NEUTRAL);
                boolean hasEnemy = PermissionManager.get().hasPermission(viewerUuid, Permissions.ENEMY);
                boolean hasAlly = PermissionManager.get().hasPermission(viewerUuid, Permissions.ALLY);

                if ("Ally".equals(item.type)) {
                    // For allies: NEUTRAL and ENEMY buttons (permission-gated)
                    if (hasNeutral) {
                        cmd.set(idx + " #NeutralBtn.Visible", true);
                        events.addEventBinding(
                                CustomUIEventBindingType.Activating,
                                idx + " #NeutralBtn",
                                EventData.of("Button", "SetNeutral")
                                        .append("FactionId", item.factionId.toString())
                                        .append("FactionName", item.factionName),
                                false
                        );
                    }
                    if (hasEnemy) {
                        cmd.set(idx + " #EnemyBtn.Visible", true);
                        events.addEventBinding(
                                CustomUIEventBindingType.Activating,
                                idx + " #EnemyBtn",
                                EventData.of("Button", "SetEnemy")
                                        .append("FactionId", item.factionId.toString())
                                        .append("FactionName", item.factionName),
                                false
                        );
                    }
                } else if ("Enemy".equals(item.type)) {
                    // For enemies: NEUTRAL and ALLY buttons (permission-gated)
                    if (hasNeutral) {
                        cmd.set(idx + " #NeutralBtn.Visible", true);
                        events.addEventBinding(
                                CustomUIEventBindingType.Activating,
                                idx + " #NeutralBtn",
                                EventData.of("Button", "SetNeutral")
                                        .append("FactionId", item.factionId.toString())
                                        .append("FactionName", item.factionName),
                                false
                        );
                    }
                    if (hasAlly) {
                        cmd.set(idx + " #AllyBtn.Visible", true);
                        events.addEventBinding(
                                CustomUIEventBindingType.Activating,
                                idx + " #AllyBtn",
                                EventData.of("Button", "RequestAlly")
                                        .append("FactionId", item.factionId.toString())
                                        .append("FactionName", item.factionName),
                                false
                        );
                    }
                } else if (item.isIncoming) {
                    // Inbound request: ACCEPT and DECLINE buttons (requires ALLY permission)
                    if (hasAlly) {
                        cmd.set(idx + " #AcceptBtn.Visible", true);
                        cmd.set(idx + " #DeclineBtn.Visible", true);

                        events.addEventBinding(
                                CustomUIEventBindingType.Activating,
                                idx + " #AcceptBtn",
                                EventData.of("Button", "AcceptAlly")
                                        .append("FactionId", item.factionId.toString())
                                        .append("FactionName", item.factionName),
                                false
                        );
                        events.addEventBinding(
                                CustomUIEventBindingType.Activating,
                                idx + " #DeclineBtn",
                                EventData.of("Button", "DeclineAlly")
                                        .append("FactionId", item.factionId.toString())
                                        .append("FactionName", item.factionName),
                                false
                        );
                    }
                } else if (item.isOutbound) {
                    // Outbound request: CANCEL button (requires ALLY permission)
                    if (hasAlly) {
                        cmd.set(idx + " #CancelBtn.Visible", true);

                        events.addEventBinding(
                                CustomUIEventBindingType.Activating,
                                idx + " #CancelBtn",
                                EventData.of("Button", "CancelRequest")
                                        .append("FactionId", item.factionId.toString())
                                        .append("FactionName", item.factionName),
                                false
                        );
                    }
                }
            }
        }
    }

    private String getEmptyMessage(boolean canManage) {
        return switch (currentTab) {
            case ALLIES -> canManage
                    ? "No allies yet. Click + SET RELATION to request alliances."
                    : "No allies yet.";
            case ENEMIES -> canManage
                    ? "No enemies declared. Click + SET RELATION to declare enemies."
                    : "No enemies declared.";
            case PENDING -> "No pending ally requests.";
        };
    }

    private String formatDate(long sinceMillis, boolean isPending) {
        if (isPending) {
            return "Recently";
        }
        long daysSince = ChronoUnit.DAYS.between(
                Instant.ofEpochMilli(sinceMillis),
                Instant.now()
        );
        if (daysSince == 0) {
            return "Today";
        } else if (daysSince == 1) {
            return "1 day ago";
        } else {
            return daysSince + " days ago";
        }
    }

    private record RelationItem(UUID factionId, String factionName, String leaderName,
                                String type, long sinceMillis, boolean isIncoming, boolean isOutbound) {}

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                FactionRelationsData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null || data.button == null) {
            sendUpdate();
            return;
        }

        Faction freshFaction = factionManager.getFaction(faction.id());
        FactionMember viewer = freshFaction != null ? freshFaction.getMember(playerRef.getUuid()) : null;
        FactionRole viewerRole = viewer != null ? viewer.role() : FactionRole.MEMBER;
        boolean canManage = viewerRole.getLevel() >= FactionRole.OFFICER.getLevel();

        // Handle navigation
        if (NavBarHelper.handleNavEvent(data, player, ref, store, playerRef, freshFaction, guiManager)) {
            return;
        }

        switch (data.button) {
            case "Tab" -> {
                if (data.tab != null) {
                    currentTab = switch (data.tab) {
                        case "ENEMIES" -> Tab.ENEMIES;
                        case "PENDING" -> Tab.PENDING;
                        default -> Tab.ALLIES;
                    };
                    currentPage = 0;
                    expandedItems.clear();
                    rebuildList(canManage);
                }
            }

            case "ToggleExpanded" -> {
                if (data.factionId != null) {
                    try {
                        UUID uuid = UUID.fromString(data.factionId);
                        if (expandedItems.contains(uuid)) {
                            expandedItems.remove(uuid);
                        } else {
                            expandedItems.add(uuid);
                        }
                        rebuildList(canManage);
                    } catch (IllegalArgumentException e) {
                        sendUpdate();
                    }
                }
            }

            case "PrevPage" -> {
                currentPage = Math.max(0, data.page);
                expandedItems.clear();
                rebuildList(canManage);
            }

            case "NextPage" -> {
                currentPage = data.page;
                expandedItems.clear();
                rebuildList(canManage);
            }

            case "SetRelation" -> guiManager.openSetRelationModal(player, ref, store, playerRef, freshFaction);

            case "SetNeutral" -> handleSetNeutral(player, ref, store, playerRef, data);

            case "SetEnemy" -> handleSetEnemy(player, ref, store, playerRef, data);

            case "RequestAlly" -> handleRequestAlly(player, ref, store, playerRef, data);

            case "AcceptAlly" -> handleAcceptAlly(player, ref, store, playerRef, data);

            case "DeclineAlly" -> handleDeclineAlly(player, ref, store, playerRef, data);

            case "CancelRequest" -> handleCancelRequest(player, ref, store, playerRef, data);

            default -> sendUpdate();
        }
    }

    private void handleSetNeutral(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                                  PlayerRef playerRef, FactionRelationsData data) {
        if (data.factionId == null) return;

        try {
            UUID targetId = UUID.fromString(data.factionId);
            UUID actorUuid = playerRef.getUuid();
            RelationManager.RelationResult result = relationManager.setNeutral(actorUuid, targetId);
            if (result == RelationManager.RelationResult.SUCCESS) {
                player.sendMessage(Message.raw("Now neutral with " + data.factionName + ".").color("#888888"));
            } else {
                player.sendMessage(Message.raw("Failed: " + result).color("#FF5555"));
            }
            refresh(player, ref, store, playerRef);
        } catch (IllegalArgumentException e) {
            player.sendMessage(Message.raw("Invalid faction.").color("#FF5555"));
        }
    }

    private void handleSetEnemy(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                                PlayerRef playerRef, FactionRelationsData data) {
        if (data.factionId == null) return;

        try {
            UUID targetId = UUID.fromString(data.factionId);
            UUID actorUuid = playerRef.getUuid();
            RelationManager.RelationResult result = relationManager.setEnemy(actorUuid, targetId);
            if (result == RelationManager.RelationResult.SUCCESS) {
                player.sendMessage(Message.raw("Now enemies with " + data.factionName + "!").color("#FF5555"));
            } else {
                player.sendMessage(Message.raw("Failed: " + result).color("#FF5555"));
            }
            refresh(player, ref, store, playerRef);
        } catch (IllegalArgumentException e) {
            player.sendMessage(Message.raw("Invalid faction.").color("#FF5555"));
        }
    }

    private void handleRequestAlly(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                                   PlayerRef playerRef, FactionRelationsData data) {
        if (data.factionId == null) return;

        try {
            UUID targetId = UUID.fromString(data.factionId);
            UUID actorUuid = playerRef.getUuid();
            RelationManager.RelationResult result = relationManager.requestAlly(actorUuid, targetId);
            if (result == RelationManager.RelationResult.REQUEST_SENT) {
                player.sendMessage(Message.raw("Alliance request sent to " + data.factionName + ".").color("#00AAFF"));
            } else if (result == RelationManager.RelationResult.REQUEST_ACCEPTED) {
                player.sendMessage(Message.raw("Now allied with " + data.factionName + "!").color("#00AAFF"));
            } else {
                player.sendMessage(Message.raw("Failed: " + result).color("#FF5555"));
            }
            refresh(player, ref, store, playerRef);
        } catch (IllegalArgumentException e) {
            player.sendMessage(Message.raw("Invalid faction.").color("#FF5555"));
        }
    }

    private void handleAcceptAlly(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                                  PlayerRef playerRef, FactionRelationsData data) {
        if (data.factionId == null) return;

        try {
            UUID requesterId = UUID.fromString(data.factionId);
            UUID actorUuid = playerRef.getUuid();
            RelationManager.RelationResult result = relationManager.acceptAlly(actorUuid, requesterId);
            if (result == RelationManager.RelationResult.REQUEST_ACCEPTED) {
                player.sendMessage(Message.raw("Now allied with " + data.factionName + "!").color("#00AAFF"));
            } else {
                player.sendMessage(Message.raw("Failed: " + result).color("#FF5555"));
            }
            refresh(player, ref, store, playerRef);
        } catch (IllegalArgumentException e) {
            player.sendMessage(Message.raw("Invalid faction.").color("#FF5555"));
        }
    }

    private void handleDeclineAlly(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                                   PlayerRef playerRef, FactionRelationsData data) {
        if (data.factionId == null) return;

        try {
            UUID requesterId = UUID.fromString(data.factionId);
            UUID actorUuid = playerRef.getUuid();
            // No direct decline method - use setEnemy then setNeutral to clear the request
            relationManager.setEnemy(actorUuid, requesterId);
            relationManager.setNeutral(actorUuid, requesterId);
            player.sendMessage(Message.raw("Ally request from " + data.factionName + " declined.").color("#888888"));
            refresh(player, ref, store, playerRef);
        } catch (IllegalArgumentException e) {
            player.sendMessage(Message.raw("Invalid faction.").color("#FF5555"));
        }
    }

    private void handleCancelRequest(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                                     PlayerRef playerRef, FactionRelationsData data) {
        if (data.factionId == null) return;

        try {
            UUID targetId = UUID.fromString(data.factionId);
            UUID actorUuid = playerRef.getUuid();
            RelationManager.RelationResult result = relationManager.cancelRequest(actorUuid, targetId);
            if (result == RelationManager.RelationResult.SUCCESS) {
                player.sendMessage(Message.raw("Ally request to " + data.factionName + " cancelled.").color("#888888"));
            } else {
                player.sendMessage(Message.raw("Failed: " + result).color("#FF5555"));
            }
            refresh(player, ref, store, playerRef);
        } catch (IllegalArgumentException e) {
            player.sendMessage(Message.raw("Invalid faction.").color("#FF5555"));
        }
    }

    private void refresh(Player player, Ref<EntityStore> ref, Store<EntityStore> store, PlayerRef playerRef) {
        Faction freshFaction = factionManager.getFaction(faction.id());
        if (freshFaction != null) {
            guiManager.openFactionRelations(player, ref, store, playerRef, freshFaction);
        } else {
            guiManager.openFactionMain(player, ref, store, playerRef);
        }
    }

    private void rebuildList(boolean canManage) {
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        buildList(cmd, events, canManage);

        sendUpdate(cmd, events, false);
    }
}
