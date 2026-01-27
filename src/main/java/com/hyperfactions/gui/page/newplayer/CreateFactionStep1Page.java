package com.hyperfactions.gui.page.newplayer;

import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.NewPlayerNavBarHelper;
import com.hyperfactions.gui.data.NewPlayerPageData;
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
 * Create Faction Wizard - Step 1: Basic Info
 * Collects faction name, color, and optional tag.
 */
public class CreateFactionStep1Page extends InteractiveCustomUIPage<NewPlayerPageData> {

    private static final String PAGE_ID = "create";
    private static final int MIN_NAME_LENGTH = 3;
    private static final int MAX_NAME_LENGTH = 20;
    private static final int MIN_TAG_LENGTH = 2;
    private static final int MAX_TAG_LENGTH = 4;

    // Available Minecraft-style color codes
    private static final String[] COLOR_CODES = {
            "0", "1", "2", "3", "4", "5", "6", "7",
            "8", "9", "a", "b", "c", "d", "e", "f"
    };

    private static final String[] COLOR_NAMES = {
            "Black", "Dark Blue", "Dark Green", "Dark Cyan",
            "Dark Red", "Purple", "Gold", "Gray",
            "Dark Gray", "Blue", "Green", "Cyan",
            "Red", "Pink", "Yellow", "White"
    };

    private static final String[] COLOR_HEX = {
            "#000000", "#0000AA", "#00AA00", "#00AAAA",
            "#AA0000", "#AA00AA", "#FFAA00", "#AAAAAA",
            "#555555", "#5555FF", "#55FF55", "#55FFFF",
            "#FF5555", "#FF55FF", "#FFFF55", "#FFFFFF"
    };

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final GuiManager guiManager;

    // State preserved across page rebuilds
    private final String selectedColor;
    private final String preservedName;
    private final String preservedTag;

    /**
     * Default constructor - opens with default color (cyan).
     */
    public CreateFactionStep1Page(PlayerRef playerRef,
                                  FactionManager factionManager,
                                  GuiManager guiManager) {
        this(playerRef, factionManager, guiManager, "b", "", "");
    }

    /**
     * Constructor with preserved state for color selection.
     * Used when user clicks a color button to rebuild the page with new color.
     */
    public CreateFactionStep1Page(PlayerRef playerRef,
                                  FactionManager factionManager,
                                  GuiManager guiManager,
                                  String selectedColor,
                                  String preservedName,
                                  String preservedTag) {
        super(playerRef, CustomPageLifetime.CanDismiss, NewPlayerPageData.CODEC);
        this.playerRef = playerRef;
        this.factionManager = factionManager;
        this.guiManager = guiManager;
        this.selectedColor = selectedColor != null ? selectedColor : "b";
        this.preservedName = preservedName != null ? preservedName : "";
        this.preservedTag = preservedTag != null ? preservedTag : "";
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        // Load the main template
        cmd.append("HyperFactions/newplayer/create_step1.ui");

        // Setup navigation bar for new players
        NewPlayerNavBarHelper.setupBar(playerRef, PAGE_ID, cmd, events);

        // Restore preserved input values (for color selection rebuilds)
        if (!preservedName.isEmpty()) {
            cmd.set("#NameInput.Value", preservedName);
        }
        if (!preservedTag.isEmpty()) {
            cmd.set("#TagInput.Value", preservedTag);
        }

        // Build color picker grid
        buildColorPicker(cmd, events);

        // Set preview with selected color - only the faction name is colored (not "Preview:" label)
        int colorIndex = getColorIndex(selectedColor);
        String previewName = !preservedName.isEmpty() ? preservedName : "Your Faction Name";
        cmd.set("#ColorPreview.TextSpans", Message.raw(previewName).color(COLOR_HEX[colorIndex]));

        // NEXT button
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#NextBtn",
                EventData.of("Button", "Next")
                        .append("@Name", "#NameInput.Value")
                        .append("Color", selectedColor)
                        .append("@Tag", "#TagInput.Value"),
                false
        );
    }

    private void buildColorPicker(UICommandBuilder cmd, UIEventBuilder events) {
        // Bind click events for 16 color buttons
        // Colors are already set in the .ui template - we just bind the events
        for (int i = 0; i < 16; i++) {
            String btnId = "#Color" + i;
            String colorCode = COLOR_CODES[i];

            // Bind click event
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    btnId,
                    EventData.of("Button", "SelectColor")
                            .append("Color", colorCode)
                            .append("@Name", "#NameInput.Value")
                            .append("@Tag", "#TagInput.Value"),
                    false
            );
        }
    }

    private int getColorIndex(String colorCode) {
        for (int i = 0; i < COLOR_CODES.length; i++) {
            if (COLOR_CODES[i].equals(colorCode)) {
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
            case "SelectColor" -> {
                // Open fresh page with new color state - fixes binding-time capture issue
                String newColor = data.inputColor != null ? data.inputColor : selectedColor;
                String name = data.inputName != null ? data.inputName : preservedName;
                String tag = data.inputTag != null ? data.inputTag : preservedTag;
                guiManager.openCreateFactionStep1(player, ref, store, playerRef, newColor, name, tag);
            }

            case "Next" -> handleNext(player, ref, store, playerRef, data);

            default -> sendUpdate();
        }
    }

    private void handleNext(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                            PlayerRef playerRef, NewPlayerPageData data) {
        String name = data.inputName != null ? data.inputName.trim() : "";
        String tag = data.inputTag != null ? data.inputTag.trim() : "";
        String color = data.inputColor != null ? data.inputColor : selectedColor;

        // Validate faction name
        if (name.isEmpty()) {
            player.sendMessage(Message.raw("Please enter a faction name.").color("#FF5555"));
            sendUpdate();
            return;
        }

        if (name.length() < MIN_NAME_LENGTH) {
            player.sendMessage(Message.raw("Faction name must be at least " + MIN_NAME_LENGTH + " characters.").color("#FF5555"));
            sendUpdate();
            return;
        }

        if (name.length() > MAX_NAME_LENGTH) {
            player.sendMessage(Message.raw("Faction name cannot exceed " + MAX_NAME_LENGTH + " characters.").color("#FF5555"));
            sendUpdate();
            return;
        }

        // Check if name is already taken
        if (factionManager.getFactionByName(name) != null) {
            player.sendMessage(Message.raw("A faction with this name already exists.").color("#FF5555"));
            sendUpdate();
            return;
        }

        // Validate tag format if provided (will be auto-generated in Step 2 if empty)
        if (!tag.isEmpty()) {
            if (tag.length() < MIN_TAG_LENGTH || tag.length() > MAX_TAG_LENGTH) {
                player.sendMessage(Message.raw("Faction tag must be " + MIN_TAG_LENGTH + "-" + MAX_TAG_LENGTH + " characters.").color("#FF5555"));
                sendUpdate();
                return;
            }

            if (!tag.matches("^[a-zA-Z0-9]+$")) {
                player.sendMessage(Message.raw("Faction tag can only contain letters and numbers.").color("#FF5555"));
                sendUpdate();
                return;
            }
        }

        // Proceed to Step 2 (tag will be auto-generated there if empty)
        guiManager.openCreateFactionStep2(player, ref, store, playerRef, name, color, tag);
    }
}
