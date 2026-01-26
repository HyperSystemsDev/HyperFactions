package com.hyperfactions.gui.page;

import com.hyperfactions.data.*;
import com.hyperfactions.gui.FactionPageRegistry;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.NavBarHelper;
import com.hyperfactions.gui.data.FactionRelationsData;
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
 * Faction Relations page - displays allies, enemies, and pending requests in sections.
 * All sections visible at once (no tabs).
 */
public class FactionRelationsPage extends InteractiveCustomUIPage<FactionRelationsData> {

    private static final String PAGE_ID = "relations";

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final RelationManager relationManager;
    private final GuiManager guiManager;
    private final Faction faction;

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
        cmd.append("HyperFactions/faction_relations.ui");

        // Setup navigation bar
        NavBarHelper.setupBar(playerRef, true, PAGE_ID, cmd, events);

        // Conditionally append SET RELATION button for officers+
        if (canManage) {
            cmd.append("#ActionBtnContainer", "HyperFactions/relation_set_btn.ui");
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#ActionBtnContainer #SetRelationBtn",
                    EventData.of("Button", "SetRelation"),
                    false
            );
        }

        // Get all relations
        List<FactionRelationEntry> allies = getRelationsOfType(RelationType.ALLY);
        List<FactionRelationEntry> enemies = getRelationsOfType(RelationType.ENEMY);
        List<FactionRelationEntry> requests = getPendingRequests();

        // === ALLIES SECTION ===
        cmd.set("#AlliesHeader.Text", "ALLIES (" + allies.size() + ")");
        if (allies.isEmpty()) {
            cmd.append("#AlliesList", "HyperFactions/relation_empty.ui");
            String allyMsg = canManage
                    ? "No allies yet. Click SET RELATION below to request alliances."
                    : "No allies yet.";
            cmd.set("#AlliesList[0] #EmptyText.Text", allyMsg);
        } else {
            buildRelationEntries(cmd, events, "#AlliesList", allies, "ally", canManage);
        }

        // === ENEMIES SECTION ===
        cmd.set("#EnemiesHeader.Text", "ENEMIES (" + enemies.size() + ")");
        if (enemies.isEmpty()) {
            cmd.append("#EnemiesList", "HyperFactions/relation_empty.ui");
            String enemyMsg = canManage
                    ? "No enemies declared. Click SET RELATION below to declare enemies."
                    : "No enemies declared.";
            cmd.set("#EnemiesList[0] #EmptyText.Text", enemyMsg);
        } else {
            buildRelationEntries(cmd, events, "#EnemiesList", enemies, "enemy", canManage);
        }

        // === PENDING REQUESTS SECTION ===
        cmd.set("#RequestsHeader.Text", "PENDING REQUESTS (" + requests.size() + ")");
        if (requests.isEmpty()) {
            cmd.append("#RequestsList", "HyperFactions/relation_empty.ui");
            cmd.set("#RequestsList[0] #EmptyText.Text", "No pending ally requests.");
        } else {
            buildRelationEntries(cmd, events, "#RequestsList", requests, "request", canManage);
        }
    }

    private void buildRelationEntries(UICommandBuilder cmd, UIEventBuilder events,
                                       String containerSelector, List<FactionRelationEntry> entries,
                                       String entryType, boolean canManage) {
        for (int i = 0; i < entries.size(); i++) {
            FactionRelationEntry entry = entries.get(i);

            // Append the entry template
            cmd.append(containerSelector, "HyperFactions/relation_entry.ui");

            String prefix = containerSelector + "[" + i + "] ";

            // Faction info
            cmd.set(prefix + "#FactionName.Text", entry.factionName);
            // Note: cmd.set() doesn't support .Style.* - color is shown via the type badge instead
            cmd.set(prefix + "#LeaderName.Text", "Leader: " + entry.leaderName);

            // Date established
            String dateText = formatDate(entry.sinceMillis, entryType);
            cmd.set(prefix + "#DateEstablished.Text", dateText);

            // Relation type badge
            cmd.set(prefix + "#RelationType.Text", entry.type);

            // Conditionally append action buttons based on type and permissions
            if (canManage) {
                String btnContainer = prefix + "#ButtonsContainer";

                switch (entryType) {
                    case "ally" -> {
                        // Append NEUTRAL and ENEMY buttons
                        cmd.append(btnContainer, "HyperFactions/relation_btn_neutral.ui");
                        cmd.append(btnContainer, "HyperFactions/relation_btn_enemy.ui");

                        events.addEventBinding(
                                CustomUIEventBindingType.Activating,
                                btnContainer + " #NeutralBtn",
                                EventData.of("Button", "SetNeutral")
                                        .append("FactionId", entry.factionId.toString())
                                        .append("FactionName", entry.factionName),
                                false
                        );
                        events.addEventBinding(
                                CustomUIEventBindingType.Activating,
                                btnContainer + " #EnemyBtn",
                                EventData.of("Button", "SetEnemy")
                                        .append("FactionId", entry.factionId.toString())
                                        .append("FactionName", entry.factionName),
                                false
                        );
                    }
                    case "enemy" -> {
                        // Append NEUTRAL and ALLY buttons
                        cmd.append(btnContainer, "HyperFactions/relation_btn_neutral.ui");
                        cmd.append(btnContainer, "HyperFactions/relation_btn_ally.ui");

                        events.addEventBinding(
                                CustomUIEventBindingType.Activating,
                                btnContainer + " #NeutralBtn",
                                EventData.of("Button", "SetNeutral")
                                        .append("FactionId", entry.factionId.toString())
                                        .append("FactionName", entry.factionName),
                                false
                        );
                        events.addEventBinding(
                                CustomUIEventBindingType.Activating,
                                btnContainer + " #AllyBtn",
                                EventData.of("Button", "RequestAlly")
                                        .append("FactionId", entry.factionId.toString())
                                        .append("FactionName", entry.factionName),
                                false
                        );
                    }
                    case "request" -> {
                        // Append ACCEPT and DECLINE buttons
                        cmd.append(btnContainer, "HyperFactions/relation_btn_accept.ui");
                        cmd.append(btnContainer, "HyperFactions/relation_btn_decline.ui");

                        events.addEventBinding(
                                CustomUIEventBindingType.Activating,
                                btnContainer + " #AcceptBtn",
                                EventData.of("Button", "AcceptAlly")
                                        .append("FactionId", entry.factionId.toString())
                                        .append("FactionName", entry.factionName),
                                false
                        );
                        events.addEventBinding(
                                CustomUIEventBindingType.Activating,
                                btnContainer + " #DeclineBtn",
                                EventData.of("Button", "DeclineAlly")
                                        .append("FactionId", entry.factionId.toString())
                                        .append("FactionName", entry.factionName),
                                false
                        );
                    }
                }
            }
        }
    }

    private String formatDate(long sinceMillis, String entryType) {
        if ("request".equals(entryType)) {
            return "Requested recently";
        }
        long daysSince = ChronoUnit.DAYS.between(
                Instant.ofEpochMilli(sinceMillis),
                Instant.now()
        );
        if (daysSince == 0) {
            return "Since: today";
        } else if (daysSince == 1) {
            return "Since: 1 day ago";
        } else {
            return "Since: " + daysSince + " days ago";
        }
    }

    private List<FactionRelationEntry> getRelationsOfType(RelationType targetType) {
        List<FactionRelationEntry> entries = new ArrayList<>();

        for (FactionRelation relation : faction.relations().values()) {
            if (relation.type() == targetType) {
                Faction other = factionManager.getFaction(relation.targetFactionId());
                if (other != null) {
                    FactionMember leader = other.getLeader();
                    String leaderName = leader != null ? leader.username() : "Unknown";
                    String color = targetType == RelationType.ALLY ? "#00AAFF" : "#FF5555";
                    String typeText = targetType == RelationType.ALLY ? "ALLY" : "ENEMY";
                    entries.add(new FactionRelationEntry(
                            other.id(),
                            other.name(),
                            leaderName,
                            typeText,
                            color,
                            relation.since()
                    ));
                }
            }
        }

        // Sort alphabetically
        entries.sort(Comparator.comparing(e -> e.factionName));
        return entries;
    }

    private List<FactionRelationEntry> getPendingRequests() {
        List<FactionRelationEntry> entries = new ArrayList<>();

        Set<UUID> pendingRequests = relationManager.getPendingRequests(faction.id());
        for (UUID requesterId : pendingRequests) {
            Faction requester = factionManager.getFaction(requesterId);
            if (requester != null) {
                FactionMember leader = requester.getLeader();
                String leaderName = leader != null ? leader.username() : "Unknown";
                entries.add(new FactionRelationEntry(
                        requester.id(),
                        requester.name(),
                        leaderName,
                        "PENDING",
                        "#FFAA00",
                        System.currentTimeMillis()
                ));
            }
        }

        // Sort alphabetically
        entries.sort(Comparator.comparing(e -> e.factionName));
        return entries;
    }

    private record FactionRelationEntry(UUID factionId, String factionName, String leaderName,
                                         String type, String color, long sinceMillis) {}

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

        // Handle navigation
        if ("Nav".equals(data.button) && data.navBar != null) {
            FactionPageRegistry.Entry entry = FactionPageRegistry.getInstance().getEntry(data.navBar);
            if (entry != null) {
                var page = entry.guiSupplier().create(player, ref, store, playerRef, faction, guiManager);
                if (page != null) {
                    player.getPageManager().openCustomPage(ref, store, page);
                    return;
                }
            }
            sendUpdate();
            return;
        }

        switch (data.button) {
            case "SetRelation" -> {
                guiManager.openSetRelationModal(player, ref, store, playerRef, faction);
            }

            case "SetNeutral" -> {
                if (data.factionId != null) {
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
            }

            case "SetEnemy" -> {
                if (data.factionId != null) {
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
            }

            case "RequestAlly" -> {
                if (data.factionId != null) {
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
            }

            case "AcceptAlly" -> {
                if (data.factionId != null) {
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
            }

            case "DeclineAlly" -> {
                if (data.factionId != null) {
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
            }

            default -> sendUpdate();
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
}
