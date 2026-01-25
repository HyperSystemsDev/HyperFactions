package com.hyperfactions.gui.component;

import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reusable input modal component.
 * Displays a text input prompt with custom title, placeholder, and validation.
 */
public class InputModal {

    private final String title;
    private final String label;
    private final String placeholder;
    private final String currentValue;
    private final int maxLength;
    private final boolean multiline;
    private final String submitEventName;
    private final String cancelEventName;
    private final EventData submitEventData;
    private final EventData cancelEventData;

    /**
     * Builder for InputModal.
     */
    public static class Builder {
        private String title = "Input";
        private String label = "Enter value:";
        private String placeholder = "";
        private String currentValue = "";
        private int maxLength = 50;
        private boolean multiline = false;
        private String submitEventName = "Submit";
        private String cancelEventName = "Cancel";
        private EventData submitEventData;
        private EventData cancelEventData;

        public Builder title(@NotNull String title) {
            this.title = title;
            return this;
        }

        public Builder label(@NotNull String label) {
            this.label = label;
            return this;
        }

        public Builder placeholder(@NotNull String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        public Builder currentValue(@Nullable String currentValue) {
            this.currentValue = currentValue != null ? currentValue : "";
            return this;
        }

        public Builder maxLength(int maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        public Builder multiline(boolean multiline) {
            this.multiline = multiline;
            return this;
        }

        public Builder submitEvent(@NotNull String eventName) {
            this.submitEventName = eventName;
            return this;
        }

        public Builder submitEvent(@NotNull String eventName, @NotNull EventData data) {
            this.submitEventName = eventName;
            this.submitEventData = data;
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

        public InputModal build() {
            if (submitEventData == null) {
                submitEventData = EventData.of("Button", submitEventName);
            }
            if (cancelEventData == null) {
                cancelEventData = EventData.of("Button", cancelEventName);
            }
            return new InputModal(
                title, label, placeholder, currentValue, maxLength, multiline,
                submitEventName, cancelEventName, submitEventData, cancelEventData
            );
        }
    }

    private InputModal(String title, String label, String placeholder, String currentValue,
                       int maxLength, boolean multiline, String submitEventName,
                       String cancelEventName, EventData submitEventData, EventData cancelEventData) {
        this.title = title;
        this.label = label;
        this.placeholder = placeholder;
        this.currentValue = currentValue;
        this.maxLength = maxLength;
        this.multiline = multiline;
        this.submitEventName = submitEventName;
        this.cancelEventName = cancelEventName;
        this.submitEventData = submitEventData;
        this.cancelEventData = cancelEventData;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Renders the input modal into the UI.
     *
     * @param cmd        UI command builder
     * @param events     UI event builder
     * @param targetId   Target element ID where modal should be appended (e.g., "#ModalContainer")
     */
    public void render(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events,
                       @NotNull String targetId) {
        // Append modal template (single-line or multiline)
        if (multiline) {
            cmd.append(targetId, "HyperFactions/modal_input_multiline.ui");
        } else {
            cmd.append(targetId, "HyperFactions/modal_input.ui");
        }

        // Set title and label
        cmd.set(targetId + " #ModalTitle.Text", title);
        cmd.set(targetId + " #InputLabel.Text", label);

        // Set input properties
        cmd.set(targetId + " #InputField.Placeholder", placeholder);
        if (!currentValue.isEmpty()) {
            cmd.set(targetId + " #InputField.Text", currentValue);
        }
        cmd.set(targetId + " #InputField.MaxLength", String.valueOf(maxLength));

        // Bind submit button (will include input value in event data)
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            targetId + " #SubmitBtn",
            submitEventData.append("InputValue", targetId + " #InputField.Text"),
            false
        );

        // Bind cancel button
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            targetId + " #CancelBtn",
            cancelEventData,
            false
        );
    }

    /**
     * Quick helper for faction rename.
     */
    public static InputModal rename(String currentName) {
        return builder()
            .title("Rename Faction")
            .label("Enter new faction name:")
            .placeholder("My Faction")
            .currentValue(currentName)
            .maxLength(32)
            .submitEvent("RenameSubmit")
            .build();
    }

    /**
     * Quick helper for faction description.
     */
    public static InputModal description(String currentDescription) {
        return builder()
            .title("Set Description")
            .label("Enter faction description:")
            .placeholder("A great faction...")
            .currentValue(currentDescription)
            .maxLength(200)
            .multiline(true)
            .submitEvent("DescriptionSubmit")
            .build();
    }

    /**
     * Quick helper for player search/invite.
     */
    public static InputModal playerName() {
        return builder()
            .title("Invite Player")
            .label("Enter player name:")
            .placeholder("PlayerName")
            .maxLength(16)
            .submitEvent("InviteSubmit")
            .build();
    }

    // Getters
    public String getTitle() { return title; }
    public String getLabel() { return label; }
    public String getPlaceholder() { return placeholder; }
    public String getCurrentValue() { return currentValue; }
    public int getMaxLength() { return maxLength; }
    public boolean isMultiline() { return multiline; }
}
