package com.hyperfactions.gui.page;

import com.hyperfactions.data.*;
import com.hyperfactions.gui.GuiManager;
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

import java.util.*;

/**
 * Faction Relations page - displays allies, enemies, and pending requests.
 */
public class FactionRelationsPage extends InteractiveCustomUIPage<FactionRelationsData> {

    private static final int ENTRIES_PER_PAGE = 6;

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final RelationManager relationManager;
    private final GuiManager guiManager;
    private final Faction faction;

    private String currentTab = "allies"; // allies, enemies, requests
    private int currentPage = 0;

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


        // Tab buttons

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TabAllies",
                EventData.of("Button", "TabAllies").append("Tab", "allies"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TabEnemies",
                EventData.of("Button", "TabEnemies").append("Tab", "enemies"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TabRequests",
                EventData.of("Button", "TabRequests").append("Tab", "requests"),
                false
        );

        // Get the list based on current tab
        List<FactionRelationEntry> entries = getEntriesForTab();

        // Calculate pagination
        int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / ENTRIES_PER_PAGE));
        currentPage = Math.min(currentPage, totalPages - 1);
        int startIdx = currentPage * ENTRIES_PER_PAGE;

        // Count display
        cmd.set("#EntryCount.Text", entries.size() + " " + currentTab);

        // Build entries
        for (int i = 0; i < ENTRIES_PER_PAGE; i++) {
            String entryId = "#RelationEntry" + i;
            int entryIdx = startIdx + i;

            if (entryIdx < entries.size()) {
                FactionRelationEntry entry = entries.get(entryIdx);

                cmd.append(entryId, "HyperFactions/relation_entry.ui");

                String prefix = entryId + " ";

                // Faction info
                cmd.set(prefix + "#FactionName.Text", entry.factionName);
                cmd.set(prefix + "#RelationType.Text", entry.type);

                // Action buttons based on tab and permissions
                if (canManage) {
                    switch (currentTab) {
                        case "allies" -> {
                            // Remove ally button
                            events.addEventBinding(
                                    CustomUIEventBindingType.Activating,
                                    prefix + "#RemoveBtn",
                                    EventData.of("Button", "RemoveAlly")
                                            .append("FactionId", entry.factionId.toString())
                                            .append("FactionName", entry.factionName),
                                    false
                            );
                        }
                        case "enemies" -> {
                            // Set neutral button
                            events.addEventBinding(
                                    CustomUIEventBindingType.Activating,
                                    prefix + "#NeutralBtn",
                                    EventData.of("Button", "SetNeutral")
                                            .append("FactionId", entry.factionId.toString())
                                            .append("FactionName", entry.factionName),
                                    false
                            );
                        }
                        case "requests" -> {
                            // Accept/Decline ally request
                            events.addEventBinding(
                                    CustomUIEventBindingType.Activating,
                                    prefix + "#AcceptBtn",
                                    EventData.of("Button", "AcceptAlly")
                                            .append("FactionId", entry.factionId.toString())
                                            .append("FactionName", entry.factionName),
                                    false
                            );
                            events.addEventBinding(
                                    CustomUIEventBindingType.Activating,
                                    prefix + "#DeclineBtn",
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

    private List<FactionRelationEntry> getEntriesForTab() {
        List<FactionRelationEntry> entries = new ArrayList<>();

        switch (currentTab) {
            case "allies" -> {
                for (FactionRelation relation : faction.relations().values()) {
                    if (relation.type() == RelationType.ALLY) {
                        Faction other = factionManager.getFaction(relation.targetFactionId());
                        if (other != null) {
                            entries.add(new FactionRelationEntry(
                                    other.id(),
                                    other.name(),
                                    "ALLY",
                                    "#00AAFF"
                            ));
                        }
                    }
                }
            }
            case "enemies" -> {
                for (FactionRelation relation : faction.relations().values()) {
                    if (relation.type() == RelationType.ENEMY) {
                        Faction other = factionManager.getFaction(relation.targetFactionId());
                        if (other != null) {
                            entries.add(new FactionRelationEntry(
                                    other.id(),
                                    other.name(),
                                    "ENEMY",
                                    "#FF5555"
                            ));
                        }
                    }
                }
            }
            case "requests" -> {
                Set<UUID> pendingRequests = relationManager.getPendingRequests(faction.id());
                for (UUID requesterId : pendingRequests) {
                    Faction requester = factionManager.getFaction(requesterId);
                    if (requester != null) {
                        entries.add(new FactionRelationEntry(
                                requester.id(),
                                requester.name(),
                                "PENDING",
                                "#FFAA00"
                        ));
                    }
                }
            }
        }

        // Sort alphabetically
        entries.sort(Comparator.comparing(e -> e.factionName));
        return entries;
    }

    private record FactionRelationEntry(UUID factionId, String factionName, String type, String color) {}

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                FactionRelationsData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null || data.button == null) {
            return;
        }

        switch (data.button) {
            case "Back" -> guiManager.openFactionMain(player, ref, store, playerRef);

            case "TabAllies", "TabEnemies", "TabRequests" -> {
                currentTab = data.tab;
                currentPage = 0;
                refresh(player, ref, store, playerRef);
            }

            case "PrevPage" -> {
                currentPage = Math.max(0, data.page);
                refresh(player, ref, store, playerRef);
            }

            case "NextPage" -> {
                currentPage = data.page;
                refresh(player, ref, store, playerRef);
            }

            case "RemoveAlly" -> {
                if (data.factionId != null) {
                    try {
                        UUID targetId = UUID.fromString(data.factionId);
                        UUID actorUuid = playerRef.getUuid();
                        RelationManager.RelationResult result = relationManager.setNeutral(actorUuid, targetId);
                        if (result == RelationManager.RelationResult.SUCCESS) {
                            player.sendMessage(Message.raw("Alliance with " + data.factionName + " ended.").color("#FFAA00"));
                        } else {
                            player.sendMessage(Message.raw("Failed: " + result).color("#FF5555"));
                        }
                        refresh(player, ref, store, playerRef);
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(Message.raw("Invalid faction.").color("#FF5555"));
                    }
                }
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
                    // No direct declineAlly method - use setNeutral to clear any relation
                    try {
                        UUID requesterId = UUID.fromString(data.factionId);
                        UUID actorUuid = playerRef.getUuid();
                        // Setting enemy then neutral effectively declines the request
                        relationManager.setEnemy(actorUuid, requesterId);
                        relationManager.setNeutral(actorUuid, requesterId);
                        player.sendMessage(Message.raw("Ally request from " + data.factionName + " declined.").color("#888888"));
                        refresh(player, ref, store, playerRef);
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(Message.raw("Invalid faction.").color("#FF5555"));
                    }
                }
            }
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
