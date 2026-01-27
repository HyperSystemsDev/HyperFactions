package com.hyperfactions.gui.page.newplayer;

import com.hyperfactions.data.Faction;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.nav.NewPlayerNavBarHelper;
import com.hyperfactions.gui.shared.data.NewPlayerPageData;
import com.hyperfactions.manager.FactionManager;
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

/**
 * Create Faction Wizard - Step 2: Review & Create
 * Shows preview, optional description, and recruitment setting.
 */
public class CreateFactionStep2Page extends InteractiveCustomUIPage<NewPlayerPageData> {

    private static final String PAGE_ID = "create";
    private static final int MAX_DESCRIPTION_LENGTH = 200;

    // Color hex values for preview
    private static final String[] COLOR_HEX = {
            "#000000", "#0000AA", "#00AA00", "#00AAAA",
            "#AA0000", "#AA00AA", "#FFAA00", "#AAAAAA",
            "#555555", "#5555FF", "#55FF55", "#55FFFF",
            "#FF5555", "#FF55FF", "#FFFF55", "#FFFFFF"
    };

    private static final String[] COLOR_NAMES = {
            "Black", "Dark Blue", "Dark Green", "Dark Cyan",
            "Dark Red", "Purple", "Gold", "Gray",
            "Dark Gray", "Blue", "Green", "Cyan",
            "Red", "Pink", "Yellow", "White"
    };

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final GuiManager guiManager;

    // Data from Step 1
    private final String factionName;
    private final String factionColor;
    private final String factionTag;

    // Step 2 selections (immutable - set via constructor for fresh page pattern)
    private final boolean openRecruitment;

    /**
     * Default constructor - opens with default recruitment (invite only).
     */
    public CreateFactionStep2Page(PlayerRef playerRef,
                                  FactionManager factionManager,
                                  GuiManager guiManager,
                                  String factionName,
                                  String factionColor,
                                  String factionTag) {
        this(playerRef, factionManager, guiManager, factionName, factionColor, factionTag, false);
    }

    /**
     * Constructor with preserved state for recruitment selection.
     * Used when user clicks a recruitment option to rebuild the page with new state.
     */
    public CreateFactionStep2Page(PlayerRef playerRef,
                                  FactionManager factionManager,
                                  GuiManager guiManager,
                                  String factionName,
                                  String factionColor,
                                  String factionTag,
                                  boolean openRecruitment) {
        super(playerRef, CustomPageLifetime.CanDismiss, NewPlayerPageData.CODEC);
        this.playerRef = playerRef;
        this.factionManager = factionManager;
        this.guiManager = guiManager;
        this.factionName = factionName;
        this.factionColor = factionColor != null ? factionColor : "b";
        // Auto-generate tag if not provided
        this.factionTag = (factionTag != null && !factionTag.isEmpty()) ? factionTag : generateTag(factionName);
        this.openRecruitment = openRecruitment;
    }

    /**
     * Auto-generates a faction tag from the faction name.
     * Takes the first 3-4 characters of the name (uppercase).
     */
    private static String generateTag(String factionName) {
        if (factionName == null || factionName.isEmpty()) {
            return "FACT";
        }
        // Remove spaces and special characters, take first 3-4 chars
        String cleaned = factionName.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        if (cleaned.isEmpty()) {
            return "FACT";
        }
        int length = Math.min(cleaned.length(), 4);
        if (length < 2) {
            // Pad with first letter if too short
            return (cleaned + cleaned + cleaned + cleaned).substring(0, 3);
        }
        return cleaned.substring(0, length);
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        // Load the main template
        cmd.append("HyperFactions/newplayer/create_step2.ui");

        // Setup navigation bar for new players
        NewPlayerNavBarHelper.setupBar(playerRef, PAGE_ID, cmd, events);

        // Build preview section
        buildPreview(cmd);

        // Build recruitment selection
        buildRecruitmentSelection(cmd, events);

        // BACK button
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#BackBtn",
                EventData.of("Button", "Back"),
                false
        );

        // CREATE FACTION button
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#CreateBtn",
                EventData.of("Button", "Create")
                        .append("@Description", "#DescInput.Value")
                        .append("Recruitment", openRecruitment ? "open" : "closed"),
                false
        );
    }

    private void buildPreview(UICommandBuilder cmd) {
        String colorHex = getColorHex(factionColor);
        String colorName = getColorName(factionColor);

        // Faction name with tag (use TextSpans for colored text)
        String displayName = factionName;
        if (factionTag != null && !factionTag.isEmpty()) {
            displayName = factionName + " [" + factionTag + "]";
        }
        cmd.set("#PreviewName.TextSpans", Message.raw(displayName).color(colorHex));

        // Color indicator - use TextSpans for colored text (cannot set Background dynamically)
        cmd.set("#PreviewColor.TextSpans", Message.raw(colorName).color(colorHex));

        // Leader
        cmd.set("#PreviewLeader.Text", "Leader: " + playerRef.getUsername());
    }

    private void buildRecruitmentSelection(UICommandBuilder cmd, UIEventBuilder events) {
        // Show current recruitment selection using TextSpans for colored text
        // (Cannot set .Style dynamically - causes crash)
        if (openRecruitment) {
            cmd.set("#CurrentRecruitment.TextSpans", Message.raw("Open").color("#55FF55"));
        } else {
            cmd.set("#CurrentRecruitment.TextSpans", Message.raw("Invite Only").color("#FFAA00"));
        }

        // Bind recruitment button events (styled like recruitment_modal)
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#InviteOnlyBtn",
                EventData.of("Button", "SetRecruitment").append("Recruitment", "closed"),
                false
        );

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#OpenBtn",
                EventData.of("Button", "SetRecruitment").append("Recruitment", "open"),
                false
        );
    }

    private String getColorHex(String colorCode) {
        int index = getColorIndex(colorCode);
        return COLOR_HEX[index];
    }

    private String getColorName(String colorCode) {
        int index = getColorIndex(colorCode);
        return COLOR_NAMES[index];
    }

    private int getColorIndex(String colorCode) {
        String[] codes = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equalsIgnoreCase(colorCode)) {
                return i;
            }
        }
        return 11; // Default to cyan
    }

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
            case "Back" -> guiManager.openCreateFactionWizard(player, ref, store, playerRef);

            case "SetRecruitment" -> {
                // Open fresh page with new recruitment state - fixes binding-time capture issue
                boolean newRecruitment = "open".equals(data.inputRecruitment);
                guiManager.openCreateFactionStep2(player, ref, store, playerRef,
                        factionName, factionColor, factionTag, newRecruitment);
            }

            case "Create" -> handleCreate(player, ref, store, playerRef, data);

            default -> sendUpdate();
        }
    }

    private void handleCreate(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                              PlayerRef playerRef, NewPlayerPageData data) {
        String description = data.inputDescription != null ? data.inputDescription.trim() : "";
        boolean isOpen = "open".equals(data.inputRecruitment);

        // Validate description length
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            player.sendMessage(Message.raw("Description cannot exceed " + MAX_DESCRIPTION_LENGTH + " characters.").color("#FF5555"));
            sendUpdate();
            return;
        }

        // Check if player is already in a faction
        if (factionManager.isInFaction(playerRef.getUuid())) {
            player.sendMessage(Message.raw("You are already in a faction.").color("#FF5555"));
            sendUpdate();
            return;
        }

        // Double-check name is still available
        if (factionManager.getFactionByName(factionName) != null) {
            player.sendMessage(Message.raw("A faction with this name was just created. Please choose another name.").color("#FF5555"));
            guiManager.openCreateFactionWizard(player, ref, store, playerRef);
            return;
        }

        // Create the faction
        FactionManager.FactionResult result = factionManager.createFaction(
                factionName,
                playerRef.getUuid(),
                playerRef.getUsername()
        );

        switch (result) {
            case SUCCESS -> {
                // Get the newly created faction
                Faction faction = factionManager.getPlayerFaction(playerRef.getUuid());
                if (faction != null) {
                    // Update faction with additional settings using immutable pattern
                    Faction updated = faction;

                    // Set color
                    if (factionColor != null && !factionColor.isEmpty()) {
                        updated = updated.withColor(factionColor);
                    }

                    // Set tag
                    if (factionTag != null && !factionTag.isEmpty()) {
                        updated = updated.withTag(factionTag);
                    }

                    // Set description
                    if (!description.isEmpty()) {
                        updated = updated.withDescription(description);
                    }

                    // Set recruitment status
                    updated = updated.withOpen(isOpen);

                    // Save the updated faction
                    factionManager.updateFaction(updated);

                    // Success message
                    player.sendMessage(
                            Message.raw("Faction ").color("#55FF55")
                                    .insert(Message.raw(factionName).color("#00FFFF"))
                                    .insert(Message.raw(" created successfully!").color("#55FF55"))
                    );

                    // Open faction dashboard with fresh faction data
                    Faction freshFaction = factionManager.getFaction(updated.id());
                    if (freshFaction != null) {
                        guiManager.openFactionDashboard(player, ref, store, playerRef, freshFaction);
                    } else {
                        guiManager.openFactionMain(player, ref, store, playerRef);
                    }
                } else {
                    player.sendMessage(Message.raw("Faction created but could not open dashboard.").color("#FFAA00"));
                    guiManager.openFactionMain(player, ref, store, playerRef);
                }
            }

            case ALREADY_IN_FACTION -> {
                player.sendMessage(Message.raw("You are already in a faction.").color("#FF5555"));
                sendUpdate();
            }

            case NAME_TAKEN -> {
                player.sendMessage(Message.raw("A faction with this name already exists.").color("#FF5555"));
                guiManager.openCreateFactionWizard(player, ref, store, playerRef);
            }

            case NAME_TOO_SHORT, NAME_TOO_LONG -> {
                player.sendMessage(Message.raw("Invalid faction name.").color("#FF5555"));
                guiManager.openCreateFactionWizard(player, ref, store, playerRef);
            }

            default -> {
                player.sendMessage(Message.raw("Could not create faction.").color("#FF5555"));
                sendUpdate();
            }
        }
    }
}
