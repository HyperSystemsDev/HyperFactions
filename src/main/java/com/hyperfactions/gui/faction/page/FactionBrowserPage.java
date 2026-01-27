package com.hyperfactions.gui.faction.page;

import com.hyperfactions.data.*;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.nav.NavBarHelper;
import com.hyperfactions.gui.faction.data.FactionPageData;
import com.hyperfactions.manager.FactionManager;
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

import java.util.*;

/**
 * Faction Browser page - displays a paginated list of all factions.
 * Uses the unified FactionPageData for event handling.
 */
public class FactionBrowserPage extends InteractiveCustomUIPage<FactionPageData> {

    private static final String PAGE_ID = "browser";
    private static final int FACTIONS_PER_PAGE = 8;

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final PowerManager powerManager;
    private final GuiManager guiManager;

    private int currentPage = 0;
    private String sortBy = "power"; // power, members, name

    public FactionBrowserPage(PlayerRef playerRef,
                              FactionManager factionManager,
                              PowerManager powerManager,
                              GuiManager guiManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, FactionPageData.CODEC);
        this.playerRef = playerRef;
        this.factionManager = factionManager;
        this.powerManager = powerManager;
        this.guiManager = guiManager;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        UUID viewerUuid = playerRef.getUuid();
        Faction viewerFaction = factionManager.getPlayerFaction(viewerUuid);
        boolean hasFaction = viewerFaction != null;

        // Load the main template
        cmd.append("HyperFactions/faction/faction_browser.ui");

        // Setup navigation bar (AdminUI pattern with indexed selectors)
        NavBarHelper.setupBar(playerRef, hasFaction, PAGE_ID, cmd, events);

        // Get all factions and sort
        List<FactionEntry> entries = buildFactionEntryList();

        // Total count
        cmd.set("#FactionCount.Text", entries.size() + " factions");

        // Sort button bindings
        setupSortButtons(events);

        // Calculate pagination
        int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / FACTIONS_PER_PAGE));
        currentPage = Math.min(currentPage, totalPages - 1);
        int startIdx = currentPage * FACTIONS_PER_PAGE;

        // Build faction cards
        buildFactionCards(cmd, events, entries, startIdx, viewerFaction);

        // Pagination
        setupPagination(cmd, events, totalPages);
    }

    private List<FactionEntry> buildFactionEntryList() {
        List<FactionEntry> entries = new ArrayList<>();
        for (Faction faction : factionManager.getAllFactions()) {
            PowerManager.FactionPowerStats stats = powerManager.getFactionPowerStats(faction.id());
            entries.add(new FactionEntry(
                    faction.id(),
                    faction.name(),
                    faction.color() != null ? faction.color() : "#00FFFF",
                    faction.members().size(),
                    stats.currentPower(),
                    faction.claims().size()
            ));
        }

        // Sort
        switch (sortBy) {
            case "power" -> entries.sort(Comparator.comparingDouble(FactionEntry::power).reversed());
            case "members" -> entries.sort(Comparator.comparingInt(FactionEntry::memberCount).reversed());
            case "name" -> entries.sort(Comparator.comparing(FactionEntry::name));
        }

        return entries;
    }

    private void setupSortButtons(UIEventBuilder events) {
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#SortPower",
                EventData.of("Button", "Sort").append("SortMode", "power"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#SortMembers",
                EventData.of("Button", "Sort").append("SortMode", "members"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#SortName",
                EventData.of("Button", "Sort").append("SortMode", "name"),
                false
        );
    }

    private void buildFactionCards(UICommandBuilder cmd, UIEventBuilder events,
                                   List<FactionEntry> entries, int startIdx,
                                   Faction viewerFaction) {
        for (int i = 0; i < FACTIONS_PER_PAGE; i++) {
            String cardId = "#FactionCard" + i;
            int factionIdx = startIdx + i;

            if (factionIdx < entries.size()) {
                FactionEntry entry = entries.get(factionIdx);
                boolean isOwnFaction = viewerFaction != null && viewerFaction.id().equals(entry.id);

                cmd.append(cardId, "HyperFactions/faction/faction_card.ui");

                String prefix = cardId + " ";

                // Faction info
                cmd.set(prefix + "#FactionName.Text", entry.name);
                cmd.set(prefix + "#MemberCount.Text", entry.memberCount + " members");
                cmd.set(prefix + "#PowerCount.Text", String.format("%.0f power", entry.power));
                cmd.set(prefix + "#ClaimCount.Text", entry.claimCount + " claims");

                // Highlight own faction
                if (isOwnFaction) {
                    cmd.set(prefix + "#OwnIndicator.Text", "(Your Faction)");
                } else if (viewerFaction != null) {
                    // Show relation indicator for faction players
                    RelationType relation = viewerFaction.getRelationType(entry.id);
                    if (relation == RelationType.ALLY) {
                        cmd.append(prefix + "#RelationSlot", "HyperFactions/faction/indicator_ally.ui");
                    } else if (relation == RelationType.ENEMY) {
                        cmd.append(prefix + "#RelationSlot", "HyperFactions/faction/indicator_enemy.ui");
                    }
                    // Neutral factions don't show an indicator
                }

                // View button
                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        prefix + "#ViewBtn",
                        EventData.of("Button", "ViewFaction")
                                .append("FactionId", entry.id.toString())
                                .append("Target", entry.name),
                        false
                );
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

    private record FactionEntry(UUID id, String name, String color, int memberCount, double power, int claimCount) {}

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

        Faction viewerFaction = factionManager.getPlayerFaction(playerRef.getUuid());

        // Handle navigation
        if (NavBarHelper.handleNavEvent(data, player, ref, store, playerRef, viewerFaction, guiManager)) {
            return;
        }

        switch (data.button) {
            case "Page" -> {
                currentPage = data.page;
                guiManager.openFactionBrowser(player, ref, store, playerRef);
            }

            case "Sort" -> {
                if (data.sortMode != null) {
                    sortBy = data.sortMode;
                    currentPage = 0;
                    guiManager.openFactionBrowser(player, ref, store, playerRef);
                }
            }

            case "ViewFaction" -> handleViewFaction(player, data);

            default -> sendUpdate();
        }
    }

    private void handleViewFaction(Player player, FactionPageData data) {
        if (data.factionId == null) return;

        try {
            UUID factionId = UUID.fromString(data.factionId);
            Faction faction = factionManager.getFaction(factionId);
            if (faction != null) {
                // Show faction info via chat (could be enhanced with a detail page)
                PowerManager.FactionPowerStats stats = powerManager.getFactionPowerStats(factionId);
                player.sendMessage(Message.raw("=== " + faction.name() + " ===").color("#00FFFF"));
                player.sendMessage(Message.raw("Members: " + faction.members().size()).color("#AAAAAA"));
                player.sendMessage(Message.raw("Power: " + String.format("%.0f/%.0f", stats.currentPower(), stats.maxPower())).color("#AAAAAA"));
                player.sendMessage(Message.raw("Claims: " + faction.claims().size() + "/" + stats.maxClaims()).color("#AAAAAA"));

                // List some members
                String members = faction.members().values().stream()
                        .limit(5)
                        .map(FactionMember::username)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("None");
                if (faction.members().size() > 5) {
                    members += " (+" + (faction.members().size() - 5) + " more)";
                }
                player.sendMessage(Message.raw("Members: " + members).color("#888888"));
            }
        } catch (IllegalArgumentException e) {
            player.sendMessage(Message.raw("Invalid faction.").color("#FF5555"));
        }
    }
}
