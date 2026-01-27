package com.hyperfactions.gui.page.newplayer;

import com.hyperfactions.data.*;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.NewPlayerNavBarHelper;
import com.hyperfactions.gui.data.NewPlayerPageData;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.InviteManager;
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
 * Enhanced Browse Factions page for new players.
 * Default landing page showing all factions with JOIN/REQUEST buttons.
 * Helps players discover and join existing factions.
 */
public class NewPlayerBrowsePage extends InteractiveCustomUIPage<NewPlayerPageData> {

    private static final String PAGE_ID = "browse";
    private static final int FACTIONS_PER_PAGE = 8;

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final PowerManager powerManager;
    private final InviteManager inviteManager;
    private final GuiManager guiManager;

    private int currentPage = 0;
    private String sortBy = "power"; // power, members, name

    public NewPlayerBrowsePage(PlayerRef playerRef,
                               FactionManager factionManager,
                               PowerManager powerManager,
                               InviteManager inviteManager,
                               GuiManager guiManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, NewPlayerPageData.CODEC);
        this.playerRef = playerRef;
        this.factionManager = factionManager;
        this.powerManager = powerManager;
        this.inviteManager = inviteManager;
        this.guiManager = guiManager;
    }

    /**
     * Constructor with custom page and sort state.
     */
    public NewPlayerBrowsePage(PlayerRef playerRef,
                               FactionManager factionManager,
                               PowerManager powerManager,
                               InviteManager inviteManager,
                               GuiManager guiManager,
                               int page,
                               String sortBy) {
        this(playerRef, factionManager, powerManager, inviteManager, guiManager);
        this.currentPage = page;
        this.sortBy = sortBy != null ? sortBy : "power";
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        // Load the main template
        cmd.append("HyperFactions/newplayer/browse.ui");

        // Setup navigation bar for new players
        NewPlayerNavBarHelper.setupBar(playerRef, PAGE_ID, cmd, events);

        // Get all factions and sort
        List<FactionEntry> entries = buildFactionEntryList();

        // Total count with friendly message
        cmd.set("#FactionCount.Text", entries.size() + " factions");
        cmd.set("#Subtitle.Text", "Find your new home!");

        // Sort button bindings
        setupSortButtons(cmd, events);

        // Calculate pagination
        int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / FACTIONS_PER_PAGE));
        currentPage = Math.min(currentPage, totalPages - 1);
        int startIdx = currentPage * FACTIONS_PER_PAGE;

        // Build faction cards with JOIN/REQUEST buttons
        buildFactionCards(cmd, events, entries, startIdx);

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
                    faction.claims().size(),
                    faction.open()
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

    private void setupSortButtons(UICommandBuilder cmd, UIEventBuilder events) {
        // Indicate active sort with brackets (cannot set .Style dynamically)
        cmd.set("#SortPower.Text", sortBy.equals("power") ? "[Power]" : "Power");
        cmd.set("#SortMembers.Text", sortBy.equals("members") ? "[Members]" : "Members");
        cmd.set("#SortName.Text", sortBy.equals("name") ? "[Name]" : "Name");

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
                                   List<FactionEntry> entries, int startIdx) {
        UUID viewerUuid = playerRef.getUuid();

        for (int i = 0; i < FACTIONS_PER_PAGE; i++) {
            String cardId = "#FactionCard" + i;
            int factionIdx = startIdx + i;

            if (factionIdx < entries.size()) {
                FactionEntry entry = entries.get(factionIdx);

                cmd.append(cardId, "HyperFactions/newplayer/faction_card.ui");

                String prefix = cardId + " ";

                // Faction info
                cmd.set(prefix + "#FactionName.Text", entry.name);
                cmd.set(prefix + "#MemberCount.Text", entry.memberCount + " members");
                cmd.set(prefix + "#PowerCount.Text", String.format("%.0f power", entry.power));
                cmd.set(prefix + "#ClaimCount.Text", entry.claimCount + " claims");

                // Recruitment status badge (use TextSpans for color - cannot set .Style dynamically)
                if (entry.isOpen) {
                    cmd.set(prefix + "#RecruitmentBadge.TextSpans", Message.raw("OPEN RECRUITMENT").color("#44CC44"));
                } else {
                    cmd.set(prefix + "#RecruitmentBadge.TextSpans", Message.raw("INVITE ONLY").color("#FFAA00"));
                }

                // Action button - JOIN for open factions, REQUEST for invite-only
                // Check if player already has an invite from this faction
                boolean hasInvite = inviteManager.hasInvite(entry.id, viewerUuid);

                // Action button (cannot set .Style dynamically - uses default template style)
                if (hasInvite) {
                    // Player has pending invite - show ACCEPT button
                    cmd.set(prefix + "#ActionBtn.Text", "ACCEPT");
                    events.addEventBinding(
                            CustomUIEventBindingType.Activating,
                            prefix + "#ActionBtn",
                            EventData.of("Button", "AcceptInvite")
                                    .append("FactionId", entry.id.toString())
                                    .append("FactionName", entry.name),
                            false
                    );
                } else if (entry.isOpen) {
                    // Open faction - JOIN button
                    cmd.set(prefix + "#ActionBtn.Text", "JOIN");
                    events.addEventBinding(
                            CustomUIEventBindingType.Activating,
                            prefix + "#ActionBtn",
                            EventData.of("Button", "JoinFaction")
                                    .append("FactionId", entry.id.toString())
                                    .append("FactionName", entry.name),
                            false
                    );
                } else {
                    // Invite-only faction - REQUEST button
                    cmd.set(prefix + "#ActionBtn.Text", "REQUEST");
                    events.addEventBinding(
                            CustomUIEventBindingType.Activating,
                            prefix + "#ActionBtn",
                            EventData.of("Button", "RequestJoin")
                                    .append("FactionId", entry.id.toString())
                                    .append("FactionName", entry.name),
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

    private record FactionEntry(UUID id, String name, String color, int memberCount, double power, int claimCount, boolean isOpen) {}

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                NewPlayerPageData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null || data.button == null) {
            sendUpdate();
            return;
        }

        // Handle navigation
        if (NewPlayerNavBarHelper.handleNavEvent(data, player, ref, store, playerRef, guiManager)) {
            return;
        }

        switch (data.button) {
            case "Page" -> {
                guiManager.openNewPlayerBrowse(player, ref, store, playerRef, data.page, sortBy);
            }

            case "Sort" -> {
                if (data.sortMode != null) {
                    guiManager.openNewPlayerBrowse(player, ref, store, playerRef, 0, data.sortMode);
                } else {
                    sendUpdate();
                }
            }

            case "JoinFaction" -> handleJoinFaction(player, ref, store, playerRef, data);

            case "AcceptInvite" -> handleAcceptInvite(player, ref, store, playerRef, data);

            case "RequestJoin" -> handleRequestJoin(player, data);

            default -> sendUpdate();
        }
    }

    private void handleJoinFaction(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                                   PlayerRef playerRef, NewPlayerPageData data) {
        if (data.factionId == null) {
            sendUpdate();
            return;
        }

        try {
            UUID factionId = UUID.fromString(data.factionId);
            Faction faction = factionManager.getFaction(factionId);

            if (faction == null) {
                player.sendMessage(Message.raw("Faction not found.").color("#FF5555"));
                sendUpdate();
                return;
            }

            if (!faction.open()) {
                player.sendMessage(Message.raw("This faction is invite-only.").color("#FF5555"));
                sendUpdate();
                return;
            }

            // Join the faction using addMember
            FactionManager.FactionResult result = factionManager.addMember(
                    factionId,
                    playerRef.getUuid(),
                    playerRef.getUsername()
            );

            switch (result) {
                case SUCCESS -> {
                    player.sendMessage(
                            Message.raw("You joined ").color("#55FF55")
                                    .insert(Message.raw(faction.name()).color("#00FFFF"))
                                    .insert(Message.raw("!").color("#55FF55"))
                    );
                    // Clear any pending invites
                    inviteManager.clearPlayerInvites(playerRef.getUuid());
                    // Open faction dashboard
                    Faction freshFaction = factionManager.getPlayerFaction(playerRef.getUuid());
                    if (freshFaction != null) {
                        guiManager.openFactionDashboard(player, ref, store, playerRef, freshFaction);
                    }
                }
                case ALREADY_IN_FACTION -> {
                    player.sendMessage(Message.raw("You are already in a faction.").color("#FF5555"));
                    sendUpdate();
                }
                case FACTION_NOT_FOUND -> {
                    player.sendMessage(Message.raw("Faction not found.").color("#FF5555"));
                    sendUpdate();
                }
                case FACTION_FULL -> {
                    player.sendMessage(Message.raw("This faction is full.").color("#FF5555"));
                    sendUpdate();
                }
                default -> {
                    player.sendMessage(Message.raw("Could not join faction.").color("#FF5555"));
                    sendUpdate();
                }
            }
        } catch (IllegalArgumentException e) {
            player.sendMessage(Message.raw("Invalid faction.").color("#FF5555"));
            sendUpdate();
        }
    }

    private void handleAcceptInvite(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                                    PlayerRef playerRef, NewPlayerPageData data) {
        if (data.factionId == null) {
            sendUpdate();
            return;
        }

        try {
            UUID factionId = UUID.fromString(data.factionId);
            UUID playerUuid = playerRef.getUuid();

            // Check if invite exists
            if (!inviteManager.hasInvite(factionId, playerUuid)) {
                player.sendMessage(Message.raw("This invite has expired or was revoked.").color("#FF5555"));
                sendUpdate();
                return;
            }

            Faction faction = factionManager.getFaction(factionId);
            if (faction == null) {
                player.sendMessage(Message.raw("Faction no longer exists.").color("#FF5555"));
                inviteManager.removeInvite(factionId, playerUuid);
                sendUpdate();
                return;
            }

            // Join the faction using addMember (invite allows joining closed factions)
            FactionManager.FactionResult result = factionManager.addMember(
                    factionId,
                    playerUuid,
                    playerRef.getUsername()
            );

            switch (result) {
                case SUCCESS -> {
                    player.sendMessage(
                            Message.raw("You joined ").color("#55FF55")
                                    .insert(Message.raw(faction.name()).color("#00FFFF"))
                                    .insert(Message.raw("!").color("#55FF55"))
                    );
                    // Clear invite and other pending invites
                    inviteManager.clearPlayerInvites(playerUuid);
                    // Open faction dashboard
                    Faction freshFaction = factionManager.getPlayerFaction(playerUuid);
                    if (freshFaction != null) {
                        guiManager.openFactionDashboard(player, ref, store, playerRef, freshFaction);
                    }
                }
                case ALREADY_IN_FACTION -> {
                    player.sendMessage(Message.raw("You are already in a faction.").color("#FF5555"));
                    sendUpdate();
                }
                case FACTION_FULL -> {
                    player.sendMessage(Message.raw("This faction is full.").color("#FF5555"));
                    sendUpdate();
                }
                default -> {
                    player.sendMessage(Message.raw("Could not join faction.").color("#FF5555"));
                    sendUpdate();
                }
            }
        } catch (IllegalArgumentException e) {
            player.sendMessage(Message.raw("Invalid faction.").color("#FF5555"));
            sendUpdate();
        }
    }

    private void handleRequestJoin(Player player, NewPlayerPageData data) {
        if (data.factionId == null || data.factionName == null) {
            sendUpdate();
            return;
        }

        // For now, just inform the player to ask an officer
        // In the future, this could create a join request that officers can review
        player.sendMessage(
                Message.raw("To join ").color("#AAAAAA")
                        .insert(Message.raw(data.factionName).color("#00FFFF"))
                        .insert(Message.raw(", ask an officer to invite you using:").color("#AAAAAA"))
        );
        player.sendMessage(Message.raw("/f invite " + playerRef.getUsername()).color("#FFFF55"));
        sendUpdate();
    }
}
