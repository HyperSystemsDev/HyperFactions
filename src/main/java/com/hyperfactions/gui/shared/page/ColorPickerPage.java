package com.hyperfactions.gui.shared.page;

import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.shared.data.ColorPickerData;
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

import java.util.List;
import java.util.UUID;

/**
 * Color picker page for selecting faction color.
 */
public class ColorPickerPage extends InteractiveCustomUIPage<ColorPickerData> {

    // Minecraft color codes with hex values and names
    private static final List<ColorInfo> COLORS = List.of(
            new ColorInfo("0", "#000000", "Black"),
            new ColorInfo("1", "#0000AA", "Dark Blue"),
            new ColorInfo("2", "#00AA00", "Dark Green"),
            new ColorInfo("3", "#00AAAA", "Dark Aqua"),
            new ColorInfo("4", "#AA0000", "Dark Red"),
            new ColorInfo("5", "#AA00AA", "Dark Purple"),
            new ColorInfo("6", "#FFAA00", "Gold"),
            new ColorInfo("7", "#AAAAAA", "Gray"),
            new ColorInfo("8", "#555555", "Dark Gray"),
            new ColorInfo("9", "#5555FF", "Blue"),
            new ColorInfo("a", "#55FF55", "Green"),
            new ColorInfo("b", "#55FFFF", "Aqua"),
            new ColorInfo("c", "#FF5555", "Red"),
            new ColorInfo("d", "#FF55FF", "Light Purple"),
            new ColorInfo("e", "#FFFF55", "Yellow"),
            new ColorInfo("f", "#FFFFFF", "White")
    );

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final GuiManager guiManager;
    private final Faction faction;

    public ColorPickerPage(PlayerRef playerRef,
                           FactionManager factionManager,
                           GuiManager guiManager,
                           Faction faction) {
        super(playerRef, CustomPageLifetime.CanDismiss, ColorPickerData.CODEC);
        this.playerRef = playerRef;
        this.factionManager = factionManager;
        this.guiManager = guiManager;
        this.faction = faction;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        // Load the color picker template
        cmd.append("HyperFactions/shared/color_picker.ui");

        // Find current color info
        ColorInfo currentColor = COLORS.stream()
                .filter(c -> c.code.equals(faction.color()))
                .findFirst()
                .orElse(COLORS.get(11)); // Default to Aqua

        // Set current color preview
        // Note: Can set Background.Color directly (NOT .Style.*)
        cmd.set("#CurrentColorPreview.Background.Color", currentColor.hex);
        cmd.set("#CurrentColorName.Text", currentColor.name + " (" + currentColor.hex + ")");
        // Note: Can't set Style.TextColor dynamically - text shows in default color

        // Build color buttons - bind click events only
        // Note: Can't set button Style properties dynamically - they crash cmd.set()
        // The UI template defines color buttons with preset background colors
        for (int i = 0; i < COLORS.size(); i++) {
            ColorInfo color = COLORS.get(i);
            String btnSelector = "#Color" + i;

            // Bind click event
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    btnSelector,
                    EventData.of("Button", "SelectColor")
                            .append("ColorCode", color.code)
                            .append("ColorHex", color.hex),
                    false
            );
        }

        // Cancel button
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#CancelBtn",
                EventData.of("Button", "Cancel"),
                false
        );
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                ColorPickerData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null || data.button == null) {
            return;
        }

        UUID uuid = playerRef.getUuid();
        FactionMember member = faction.getMember(uuid);

        // Verify permissions
        if (member == null || member.role().getLevel() < FactionRole.OFFICER.getLevel()) {
            player.sendMessage(Message.raw("You don't have permission to change settings.").color("#FF5555"));
            guiManager.openFactionSettings(player, ref, store, playerRef,
                    factionManager.getFaction(faction.id()));
            return;
        }

        switch (data.button) {
            case "SelectColor" -> {
                if (data.colorCode != null) {
                    Faction updatedFaction = faction.withColor(data.colorCode);
                    factionManager.updateFaction(updatedFaction);

                    // Find color name for message
                    String colorName = COLORS.stream()
                            .filter(c -> c.code.equals(data.colorCode))
                            .findFirst()
                            .map(c -> c.name)
                            .orElse("Custom");

                    player.sendMessage(
                            Message.raw("Faction color changed to ").color("#AAAAAA")
                                    .insert(Message.raw(colorName).color(data.colorHex))
                                    .insert(Message.raw("!").color("#55FF55"))
                    );

                    guiManager.openFactionSettings(player, ref, store, playerRef,
                            factionManager.getFaction(faction.id()));
                }
            }

            case "Cancel" -> {
                guiManager.openFactionSettings(player, ref, store, playerRef,
                        factionManager.getFaction(faction.id()));
            }
        }
    }

    private record ColorInfo(String code, String hex, String name) {}
}
