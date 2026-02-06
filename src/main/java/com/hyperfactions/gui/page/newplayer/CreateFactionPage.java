package com.hyperfactions.gui.page.newplayer;

import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionPermissions;
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
 * Create Faction page — two-column form combining name, tag, color,
 * description, recruitment, territory permissions, and preview into
 * one 950x650 screen.
 */
public class CreateFactionPage extends InteractiveCustomUIPage<NewPlayerPageData> {

    private static final String PAGE_ID = "create";
    private static final int MIN_NAME_LENGTH = 3;
    private static final int MAX_NAME_LENGTH = 20;
    private static final int MIN_TAG_LENGTH = 2;
    private static final int MAX_TAG_LENGTH = 4;
    private static final int MAX_DESCRIPTION_LENGTH = 200;

    // Minecraft-style color codes
    private static final String[] COLOR_CODES = {
            "0", "1", "2", "3", "4", "5", "6", "7",
            "8", "9", "a", "b", "c", "d", "e", "f"
    };

    private static final String[] COLOR_HEX = {
            "#000000", "#0000AA", "#00AA00", "#00AAAA",
            "#AA0000", "#AA00AA", "#FFAA00", "#AAAAAA",
            "#555555", "#5555FF", "#55FF55", "#55FFFF",
            "#FF5555", "#FF55FF", "#FFFF55", "#FFFFFF"
    };

    // Pre-computed RGB values for color distance calculation
    private static final int[][] COLOR_RGB = new int[16][3];

    static {
        for (int i = 0; i < COLOR_HEX.length; i++) {
            COLOR_RGB[i][0] = Integer.parseInt(COLOR_HEX[i].substring(1, 3), 16);
            COLOR_RGB[i][1] = Integer.parseInt(COLOR_HEX[i].substring(3, 5), 16);
            COLOR_RGB[i][2] = Integer.parseInt(COLOR_HEX[i].substring(5, 7), 16);
        }
    }

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final GuiManager guiManager;

    // Mutable state — updated by toggles via sendUpdate()
    private boolean openRecruitment;
    private FactionPermissions permissions = FactionPermissions.defaults();

    public CreateFactionPage(PlayerRef playerRef,
                             FactionManager factionManager,
                             GuiManager guiManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, NewPlayerPageData.CODEC);
        this.playerRef = playerRef;
        this.factionManager = factionManager;
        this.guiManager = guiManager;
        this.openRecruitment = false;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        cmd.append("HyperFactions/newplayer/create_faction.ui");

        // Setup navigation bar
        NewPlayerNavBarHelper.setupBar(playerRef, PAGE_ID, cmd, events);

        // Set default ColorPicker value (cyan)
        cmd.set("#FactionColorPicker.Value", COLOR_HEX[11]);

        // Set preview defaults
        cmd.set("#PreviewName.TextSpans", Message.raw("Your Faction Name").color(COLOR_HEX[11]));
        cmd.set("#PreviewLeader.Text", "Leader: " + playerRef.getUsername());

        // Recruitment button active state (disabled = selected)
        cmd.set("#InviteOnlyBtn.Disabled", !openRecruitment);
        cmd.set("#OpenBtn.Disabled", openRecruitment);
        if (openRecruitment) {
            cmd.set("#RecruitmentStatus.TextSpans", Message.raw("Open").color("#55FF55"));
        } else {
            cmd.set("#RecruitmentStatus.TextSpans", Message.raw("Invite Only").color("#FFAA00"));
        }

        // Bind ColorPicker ValueChanged for real-time preview
        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#FactionColorPicker",
                EventData.of("Button", "ColorChanged")
                        .append("@Color", "#FactionColorPicker.Value")
                        .append("@Name", "#NameInput.Value")
                        .append("@Tag", "#TagInput.Value"),
                false
        );

        // Bind recruitment buttons
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#InviteOnlyBtn",
                EventData.of("Button", "SetRecruitment")
                        .append("Recruitment", "closed"),
                false
        );

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#OpenBtn",
                EventData.of("Button", "SetRecruitment")
                        .append("Recruitment", "open"),
                false
        );

        // Setup permission toggle buttons
        buildPermissionToggles(cmd, events);

        // Bind CREATE button — captures all inputs
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#CreateBtn",
                EventData.of("Button", "Create")
                        .append("@Name", "#NameInput.Value")
                        .append("@Color", "#FactionColorPicker.Value")
                        .append("@Tag", "#TagInput.Value")
                        .append("@Description", "#DescInput.Value"),
                false
        );
    }

    private void buildPermissionToggles(UICommandBuilder cmd, UIEventBuilder events) {
        // Outsider permissions
        buildPermissionToggle(cmd, events, "OutsiderBreakToggle", "outsiderBreak", permissions.outsiderBreak());
        buildPermissionToggle(cmd, events, "OutsiderPlaceToggle", "outsiderPlace", permissions.outsiderPlace());
        buildPermissionToggle(cmd, events, "OutsiderInteractToggle", "outsiderInteract", permissions.outsiderInteract());

        // Ally permissions
        buildPermissionToggle(cmd, events, "AllyBreakToggle", "allyBreak", permissions.allyBreak());
        buildPermissionToggle(cmd, events, "AllyPlaceToggle", "allyPlace", permissions.allyPlace());
        buildPermissionToggle(cmd, events, "AllyInteractToggle", "allyInteract", permissions.allyInteract());

        // Member permissions
        buildPermissionToggle(cmd, events, "MemberBreakToggle", "memberBreak", permissions.memberBreak());
        buildPermissionToggle(cmd, events, "MemberPlaceToggle", "memberPlace", permissions.memberPlace());
        buildPermissionToggle(cmd, events, "MemberInteractToggle", "memberInteract", permissions.memberInteract());

        // PvP toggle
        buildPermissionToggle(cmd, events, "PvPToggle", "pvpEnabled", permissions.pvpEnabled());
        cmd.set("#PvPStatus.Text", permissions.pvpEnabled() ? "Enabled" : "Disabled");
        cmd.set("#PvPStatus.Style.TextColor", permissions.pvpEnabled() ? "#55FF55" : "#FF5555");
    }

    private void buildPermissionToggle(UICommandBuilder cmd, UIEventBuilder events,
                                       String elementId, String permName, boolean value) {
        String selector = "#" + elementId;
        cmd.set(selector + ".Text", value ? "ON" : "OFF");
        String color = value ? "#55FF55" : "#FF5555";
        cmd.set(selector + ".Style.Default.LabelStyle.TextColor", color);
        cmd.set(selector + ".Style.Hovered.LabelStyle.TextColor", color);

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                selector,
                EventData.of("Button", "TogglePerm")
                        .append("Perm", permName),
                false
        );
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
            case "ColorChanged" -> handleColorChanged(data);
            case "SetRecruitment" -> handleRecruitmentToggle(data);
            case "TogglePerm" -> handleTogglePerm(data);
            case "Create" -> handleCreate(player, ref, store, playerRef, data);
            default -> sendUpdate();
        }
    }

    private void handleColorChanged(NewPlayerPageData data) {
        String hex = extractHex(data.inputColor);
        String name = data.inputName != null ? data.inputName : "";
        String tag = data.inputTag != null ? data.inputTag : "";
        String previewText = !name.isEmpty() ? name : "Your Faction Name";
        if (!tag.isEmpty()) {
            previewText += " [" + tag + "]";
        }

        UICommandBuilder updateCmd = new UICommandBuilder();
        updateCmd.set("#PreviewName.TextSpans", Message.raw(previewText).color(hex));
        sendUpdate(updateCmd);
    }

    private void handleRecruitmentToggle(NewPlayerPageData data) {
        boolean newOpen = "open".equals(data.inputRecruitment);
        this.openRecruitment = newOpen;

        UICommandBuilder updateCmd = new UICommandBuilder();
        updateCmd.set("#InviteOnlyBtn.Disabled", !newOpen);
        updateCmd.set("#OpenBtn.Disabled", newOpen);
        updateCmd.set("#RecruitmentStatus.TextSpans",
                newOpen ? Message.raw("Open").color("#55FF55")
                        : Message.raw("Invite Only").color("#FFAA00"));
        sendUpdate(updateCmd);
    }

    private void handleTogglePerm(NewPlayerPageData data) {
        String permName = data.perm;
        if (permName == null) {
            sendUpdate();
            return;
        }

        // Toggle in-memory (no database — faction doesn't exist yet)
        this.permissions = this.permissions.toggle(permName);
        boolean newValue = this.permissions.get(permName);

        // Update just the toggled button via sendUpdate
        String elementId = permToElementId(permName);
        String selector = "#" + elementId;
        UICommandBuilder updateCmd = new UICommandBuilder();
        updateCmd.set(selector + ".Text", newValue ? "ON" : "OFF");
        String color = newValue ? "#55FF55" : "#FF5555";
        updateCmd.set(selector + ".Style.Default.LabelStyle.TextColor", color);
        updateCmd.set(selector + ".Style.Hovered.LabelStyle.TextColor", color);

        // Update PvP status label if toggling pvpEnabled
        if ("pvpEnabled".equals(permName)) {
            updateCmd.set("#PvPStatus.Text", newValue ? "Enabled" : "Disabled");
            updateCmd.set("#PvPStatus.Style.TextColor", newValue ? "#55FF55" : "#FF5555");
        }

        sendUpdate(updateCmd);
    }

    private void handleCreate(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                              PlayerRef playerRef, NewPlayerPageData data) {
        String name = data.inputName != null ? data.inputName.trim() : "";
        String tag = data.inputTag != null ? data.inputTag.trim() : "";
        String description = data.inputDescription != null ? data.inputDescription.trim() : "";

        // Snap ColorPicker hex to nearest Minecraft color code
        String hex = extractHex(data.inputColor);
        String color = hexToNearestColorCode(hex);

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

        // Validate tag format if provided
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

        // Auto-generate tag if empty
        if (tag.isEmpty()) {
            tag = generateTag(name);
        }

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

        // Create the faction
        FactionManager.FactionResult result = factionManager.createFaction(
                name,
                playerRef.getUuid(),
                playerRef.getUsername()
        );

        switch (result) {
            case SUCCESS -> {
                Faction faction = factionManager.getPlayerFaction(playerRef.getUuid());
                if (faction != null) {
                    // Apply settings using immutable pattern
                    Faction updated = faction;

                    if (color != null && !color.isEmpty()) {
                        updated = updated.withColor(color);
                    }

                    if (!tag.isEmpty()) {
                        updated = updated.withTag(tag);
                    }

                    if (!description.isEmpty()) {
                        updated = updated.withDescription(description);
                    }

                    updated = updated.withOpen(openRecruitment);
                    updated = updated.withPermissions(this.permissions);

                    factionManager.updateFaction(updated);

                    player.sendMessage(
                            Message.raw("Faction ").color("#55FF55")
                                    .insert(Message.raw(name).color("#00FFFF"))
                                    .insert(Message.raw(" created successfully!").color("#55FF55"))
                    );

                    // Open faction dashboard
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
                sendUpdate();
            }

            case NAME_TOO_SHORT, NAME_TOO_LONG -> {
                player.sendMessage(Message.raw("Invalid faction name.").color("#FF5555"));
                sendUpdate();
            }

            default -> {
                player.sendMessage(Message.raw("Could not create faction.").color("#FF5555"));
                sendUpdate();
            }
        }
    }

    // ── Utility methods ──────────────────────────────────────────────

    private static String permToElementId(String permName) {
        return switch (permName) {
            case "outsiderBreak" -> "OutsiderBreakToggle";
            case "outsiderPlace" -> "OutsiderPlaceToggle";
            case "outsiderInteract" -> "OutsiderInteractToggle";
            case "allyBreak" -> "AllyBreakToggle";
            case "allyPlace" -> "AllyPlaceToggle";
            case "allyInteract" -> "AllyInteractToggle";
            case "memberBreak" -> "MemberBreakToggle";
            case "memberPlace" -> "MemberPlaceToggle";
            case "memberInteract" -> "MemberInteractToggle";
            case "pvpEnabled" -> "PvPToggle";
            default -> permName;
        };
    }

    /**
     * Snaps an arbitrary hex color (#RRGGBB) to the nearest of the 16 Minecraft color codes.
     * Uses Euclidean distance in RGB color space.
     */
    private String hexToNearestColorCode(String hex) {
        int r, g, b;
        try {
            r = Integer.parseInt(hex.substring(1, 3), 16);
            g = Integer.parseInt(hex.substring(3, 5), 16);
            b = Integer.parseInt(hex.substring(5, 7), 16);
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            return "b"; // default cyan
        }

        int bestIndex = 11; // default cyan
        int bestDistance = Integer.MAX_VALUE;

        for (int i = 0; i < COLOR_RGB.length; i++) {
            int dr = r - COLOR_RGB[i][0];
            int dg = g - COLOR_RGB[i][1];
            int db = b - COLOR_RGB[i][2];
            int distance = dr * dr + dg * dg + db * db;

            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }

        return COLOR_CODES[bestIndex];
    }

    /**
     * Extracts a #RRGGBB hex string from the ColorPicker value.
     * The ColorPicker returns #RRGGBBAA (hex with alpha), so we strip the alpha suffix.
     */
    private String extractHex(String pickerValue) {
        if (pickerValue != null && pickerValue.length() >= 7) {
            return pickerValue.substring(0, 7);
        }
        return COLOR_HEX[11]; // default cyan
    }

    /**
     * Auto-generates a faction tag from the faction name.
     * Takes the first 3-4 characters of the name (uppercase).
     */
    private static String generateTag(String factionName) {
        if (factionName == null || factionName.isEmpty()) {
            return "FACT";
        }
        String cleaned = factionName.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        if (cleaned.isEmpty()) {
            return "FACT";
        }
        int length = Math.min(cleaned.length(), 4);
        if (length < 2) {
            return (cleaned + cleaned + cleaned + cleaned).substring(0, 3);
        }
        return cleaned.substring(0, length);
    }
}
