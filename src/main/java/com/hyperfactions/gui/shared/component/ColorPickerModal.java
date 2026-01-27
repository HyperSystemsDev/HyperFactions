package com.hyperfactions.gui.shared.component;

import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reusable color picker modal component.
 * Displays a grid of predefined colors for faction customization.
 */
public class ColorPickerModal {

    private final String title;
    private final String currentColor;
    private final Map<String, String> colorPalette;
    private final String selectEventName;
    private final String cancelEventName;
    private final EventData cancelEventData;

    /**
     * Predefined color palette for factions.
     */
    public static final Map<String, String> DEFAULT_PALETTE = new LinkedHashMap<>();
    static {
        // Primary colors
        DEFAULT_PALETTE.put("Red", "#FF5555");
        DEFAULT_PALETTE.put("Green", "#55FF55");
        DEFAULT_PALETTE.put("Blue", "#5555FF");
        DEFAULT_PALETTE.put("Yellow", "#FFFF55");
        DEFAULT_PALETTE.put("Cyan", "#55FFFF");
        DEFAULT_PALETTE.put("Magenta", "#FF55FF");

        // Secondary colors
        DEFAULT_PALETTE.put("Orange", "#FFAA00");
        DEFAULT_PALETTE.put("Purple", "#AA00FF");
        DEFAULT_PALETTE.put("Pink", "#FF55AA");
        DEFAULT_PALETTE.put("Lime", "#AAFF00");
        DEFAULT_PALETTE.put("Teal", "#00FFAA");
        DEFAULT_PALETTE.put("Indigo", "#5500FF");

        // Neutral colors
        DEFAULT_PALETTE.put("White", "#FFFFFF");
        DEFAULT_PALETTE.put("Light Gray", "#AAAAAA");
        DEFAULT_PALETTE.put("Gray", "#555555");
        DEFAULT_PALETTE.put("Dark Gray", "#333333");

        // Metallic colors
        DEFAULT_PALETTE.put("Gold", "#FFD700");
        DEFAULT_PALETTE.put("Silver", "#C0C0C0");
    }

    /**
     * Builder for ColorPickerModal.
     */
    public static class Builder {
        private String title = "Choose Color";
        private String currentColor = null;
        private Map<String, String> colorPalette = DEFAULT_PALETTE;
        private String selectEventName = "ColorSelect";
        private String cancelEventName = "Cancel";
        private EventData cancelEventData;

        public Builder title(@NotNull String title) {
            this.title = title;
            return this;
        }

        public Builder currentColor(@Nullable String currentColor) {
            this.currentColor = currentColor;
            return this;
        }

        public Builder palette(@NotNull Map<String, String> palette) {
            this.colorPalette = palette;
            return this;
        }

        public Builder selectEvent(@NotNull String eventName) {
            this.selectEventName = eventName;
            return this;
        }

        public Builder cancelEvent(@NotNull String eventName) {
            this.cancelEventName = eventName;
            return this;
        }

        public Builder cancelEvent(@NotNull String eventName, @NotNull EventData data) {
            this.cancelEventName = eventName;
            this.cancelEventData = data;
            return this;
        }

        public ColorPickerModal build() {
            if (cancelEventData == null) {
                cancelEventData = EventData.of("Button", cancelEventName);
            }
            return new ColorPickerModal(
                title, currentColor, colorPalette,
                selectEventName, cancelEventName, cancelEventData
            );
        }
    }

    private ColorPickerModal(String title, String currentColor, Map<String, String> colorPalette,
                             String selectEventName, String cancelEventName, EventData cancelEventData) {
        this.title = title;
        this.currentColor = currentColor;
        this.colorPalette = colorPalette;
        this.selectEventName = selectEventName;
        this.cancelEventName = cancelEventName;
        this.cancelEventData = cancelEventData;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Renders the color picker modal into the UI.
     *
     * @param cmd        UI command builder
     * @param events     UI event builder
     * @param targetId   Target element ID where modal should be appended (e.g., "#ModalContainer")
     */
    public void render(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events,
                       @NotNull String targetId) {
        // Append modal template
        cmd.append(targetId, "HyperFactions/shared/modal_color_picker.ui");

        // Set title
        cmd.set(targetId + " #ModalTitle.Text", title);

        // Preview current color
        if (currentColor != null) {
            cmd.set(targetId + " #CurrentColorPreview.BackgroundColor", currentColor);
            cmd.set(targetId + " #CurrentColorLabel.Text", "Current: " + getColorName(currentColor));
        }

        // Render color palette grid
        int index = 0;
        for (Map.Entry<String, String> entry : colorPalette.entrySet()) {
            String colorName = entry.getKey();
            String colorHex = entry.getValue();
            String swatchId = "#ColorSwatch" + index;

            // Append color swatch
            cmd.append(targetId + " #ColorGrid", "HyperFactions/shared/color_swatch.ui");
            cmd.set(targetId + " #ColorGrid " + swatchId + ".BackgroundColor", colorHex);
            cmd.set(targetId + " #ColorGrid " + swatchId + " #ColorName.Text", colorName);

            // Highlight if current color
            if (colorHex.equalsIgnoreCase(currentColor)) {
                cmd.set(targetId + " #ColorGrid " + swatchId + ".BorderColor", "#FFFF00");
                cmd.set(targetId + " #ColorGrid " + swatchId + ".BorderWidth", "2");
            }

            // Bind selection event
            events.addEventBinding(
                CustomUIEventBindingType.Activating,
                targetId + " #ColorGrid " + swatchId,
                EventData.of("Button", selectEventName)
                    .append("ColorName", colorName)
                    .append("ColorHex", colorHex),
                false
            );

            index++;
        }

        // Bind cancel button
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            targetId + " #CancelBtn",
            cancelEventData,
            false
        );
    }

    /**
     * Gets the color name from hex value.
     */
    private String getColorName(String hex) {
        for (Map.Entry<String, String> entry : colorPalette.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(hex)) {
                return entry.getKey();
            }
        }
        return hex;
    }

    /**
     * Quick helper for faction color selection.
     */
    public static ColorPickerModal factionColor(String currentColor) {
        return builder()
            .title("Choose Faction Color")
            .currentColor(currentColor)
            .selectEvent("FactionColorSelect")
            .build();
    }

    // Getters
    public String getTitle() { return title; }
    public String getCurrentColor() { return currentColor; }
    public Map<String, String> getColorPalette() { return colorPalette; }
}
