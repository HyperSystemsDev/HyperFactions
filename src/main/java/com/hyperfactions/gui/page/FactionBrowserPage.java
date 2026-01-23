package com.hyperfactions.gui.page;

import com.hyperfactions.data.*;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.data.FactionBrowserData;
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
 * Faction Browser page - displays a list of all factions.
 */
public class FactionBrowserPage extends InteractiveCustomUIPage<FactionBrowserData> {

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
        super(playerRef, CustomPageLifetime.CanDismiss, FactionBrowserData.CODEC);
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

        // Load the main template
        cmd.append("HyperFactions/faction_browser.ui");

        // Set title
        cmd.set("#Title #TitleText.Text", "All Factions");

        // Get all factions and sort
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

        // Total count
        cmd.set("#FactionCount.Text", entries.size() + " factions");

        // Sort buttons
        cmd.set("#SortPower.Style.TextColor", sortBy.equals("power") ? "#00FFFF" : "#888888");
        cmd.set("#SortMembers.Style.TextColor", sortBy.equals("members") ? "#00FFFF" : "#888888");
        cmd.set("#SortName.Style.TextColor", sortBy.equals("name") ? "#00FFFF" : "#888888");

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#SortPower",
                EventData.of("Button", "SortPower"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#SortMembers",
                EventData.of("Button", "SortMembers"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#SortName",
                EventData.of("Button", "SortName"),
                false
        );

        // Calculate pagination
        int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / FACTIONS_PER_PAGE));
        currentPage = Math.min(currentPage, totalPages - 1);
        int startIdx = currentPage * FACTIONS_PER_PAGE;

        // Build faction cards
        for (int i = 0; i < FACTIONS_PER_PAGE; i++) {
            String cardId = "#FactionCard" + i;
            int factionIdx = startIdx + i;

            if (factionIdx < entries.size()) {
                FactionEntry entry = entries.get(factionIdx);
                boolean isOwnFaction = viewerFaction != null && viewerFaction.id().equals(entry.id);

                cmd.append(cardId, "HyperFactions/faction_card.ui");

                String prefix = cardId + " ";

                // Faction info
                cmd.set(prefix + "#FactionName.Text", entry.name);
                cmd.set(prefix + "#FactionName.Style.TextColor", entry.color);
                cmd.set(prefix + "#MemberCount.Text", entry.memberCount + " members");
                cmd.set(prefix + "#PowerCount.Text", entry.power + " power");
                cmd.set(prefix + "#ClaimCount.Text", entry.claimCount + " claims");

                // Highlight own faction
                if (isOwnFaction) {
                    cmd.set(prefix + "#OwnIndicator.Text", "(Your Faction)");
                }

                // View button
                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        prefix + "#ViewBtn",
                        EventData.of("Button", "ViewFaction")
                                .append("FactionId", entry.id.toString())
                                .append("FactionName", entry.name),
                        false
                );
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

    private record FactionEntry(UUID id, String name, String color, int memberCount, double power, int claimCount) {}

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                FactionBrowserData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null || data.button == null) {
            return;
        }

        switch (data.button) {
            case "Back" -> guiManager.openFactionMain(player, ref, store, playerRef);

            case "PrevPage" -> {
                currentPage = Math.max(0, data.page);
                guiManager.openFactionBrowser(player, ref, store, playerRef);
            }

            case "NextPage" -> {
                currentPage = data.page;
                guiManager.openFactionBrowser(player, ref, store, playerRef);
            }

            case "SortPower" -> {
                sortBy = "power";
                currentPage = 0;
                guiManager.openFactionBrowser(player, ref, store, playerRef);
            }

            case "SortMembers" -> {
                sortBy = "members";
                currentPage = 0;
                guiManager.openFactionBrowser(player, ref, store, playerRef);
            }

            case "SortName" -> {
                sortBy = "name";
                currentPage = 0;
                guiManager.openFactionBrowser(player, ref, store, playerRef);
            }

            case "ViewFaction" -> {
                if (data.factionId != null) {
                    try {
                        UUID factionId = UUID.fromString(data.factionId);
                        Faction faction = factionManager.getFaction(factionId);
                        if (faction != null) {
                            // Show faction info via chat (could be enhanced with a detail page)
                            PowerManager.FactionPowerStats stats = powerManager.getFactionPowerStats(factionId);
                            player.sendMessage(Message.raw("=== " + faction.name() + " ===").color("#00FFFF"));
                            player.sendMessage(Message.raw("Members: " + faction.members().size()).color("#AAAAAA"));
                            player.sendMessage(Message.raw("Power: " + stats.currentPower() + "/" + stats.maxPower()).color("#AAAAAA"));
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
        }
    }
}
