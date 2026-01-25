package com.hyperfactions.gui.page;

import com.hyperfactions.data.Faction;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.component.ColorPickerModal;
import com.hyperfactions.gui.component.InputModal;
import com.hyperfactions.gui.data.CreateFactionData;
import com.hyperfactions.integration.HyperPermsIntegration;
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

import java.util.UUID;

/**
 * Create Faction page - multi-step form for creating a new faction.
 */
public class CreateFactionPage extends InteractiveCustomUIPage<CreateFactionData> {

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final GuiManager guiManager;

    // Form state
    private String factionName = "";
    private String factionDescription = "";
    private String factionColor = "#55FFFF"; // Default cyan
    private boolean isOpen = false;
    private int currentStep = 1; // 1: Name, 2: Description, 3: Color, 4: Privacy, 5: Review

    public CreateFactionPage(PlayerRef playerRef,
                             FactionManager factionManager,
                             GuiManager guiManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, CreateFactionData.CODEC);
        this.playerRef = playerRef;
        this.factionManager = factionManager;
        this.guiManager = guiManager;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        UUID uuid = playerRef.getUuid();

        // Check permission
        if (!HyperPermsIntegration.hasPermission(uuid, "hyperfactions.create")) {
            cmd.append("HyperFactions/error_page.ui");
            cmd.set("#ErrorMessage.Text", "You don't have permission to create factions.");
            return;
        }

        // Check if already in a faction
        Faction existingFaction = factionManager.getPlayerFaction(uuid);
        if (existingFaction != null) {
            cmd.append("HyperFactions/error_page.ui");
            cmd.set("#ErrorMessage.Text", "You are already in a faction. Leave your current faction first.");
            return;
        }

        // Load the create faction template
        cmd.append("HyperFactions/create_faction.ui");

        // Set progress indicator
        cmd.set("#StepIndicator.Text", "Step " + currentStep + " of 5");

        // Render current step
        switch (currentStep) {
            case 1 -> buildNameStep(cmd, events);
            case 2 -> buildDescriptionStep(cmd, events);
            case 3 -> buildColorStep(cmd, events);
            case 4 -> buildPrivacyStep(cmd, events);
            case 5 -> buildReviewStep(cmd, events);
        }

        // Navigation buttons
        if (currentStep > 1) {
            cmd.set("#BackBtn.Visible", "true");
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#BackBtn",
                    EventData.of("Button", "Back"),
                    false
            );
        } else {
            cmd.set("#BackBtn.Visible", "false");
        }

        // Cancel button (always visible)
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#CancelBtn",
                EventData.of("Button", "Cancel"),
                false
        );
    }

    private void buildNameStep(UICommandBuilder cmd, UIEventBuilder events) {
        cmd.set("#StepTitle.Text", "Faction Name");
        cmd.set("#StepDescription.Text", "Choose a unique name for your faction (max 32 characters)");

        // Append input field
        cmd.append("#StepContent", "HyperFactions/input_field.ui");
        cmd.set("#StepContent #InputField.Placeholder", "My Faction");
        cmd.set("#StepContent #InputField.MaxLength", "32");
        if (!factionName.isEmpty()) {
            cmd.set("#StepContent #InputField.Text", factionName);
        }

        // Next button
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#NextBtn",
                EventData.of("Button", "NextFromName")
                        .append("Name", "#StepContent #InputField.Text"),
                false
        );
    }

    private void buildDescriptionStep(UICommandBuilder cmd, UIEventBuilder events) {
        cmd.set("#StepTitle.Text", "Faction Description");
        cmd.set("#StepDescription.Text", "Describe your faction (optional, max 200 characters)");

        // Append textarea
        cmd.append("#StepContent", "HyperFactions/textarea_field.ui");
        cmd.set("#StepContent #TextArea.Placeholder", "A great faction...");
        cmd.set("#StepContent #TextArea.MaxLength", "200");
        if (!factionDescription.isEmpty()) {
            cmd.set("#StepContent #TextArea.Text", factionDescription);
        }

        // Next button
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#NextBtn",
                EventData.of("Button", "NextFromDescription")
                        .append("Description", "#StepContent #TextArea.Text"),
                false
        );
    }

    private void buildColorStep(UICommandBuilder cmd, UIEventBuilder events) {
        cmd.set("#StepTitle.Text", "Faction Color");
        cmd.set("#StepDescription.Text", "Choose a color for your faction name");

        // Show color picker
        ColorPickerModal colorPicker = ColorPickerModal.factionColor(factionColor);
        colorPicker.render(cmd, events, "#StepContent");
    }

    private void buildPrivacyStep(UICommandBuilder cmd, UIEventBuilder events) {
        cmd.set("#StepTitle.Text", "Privacy Settings");
        cmd.set("#StepDescription.Text", "Who can join your faction?");

        // Append privacy options
        cmd.append("#StepContent", "HyperFactions/privacy_options.ui");

        // Open option
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#StepContent #OpenOption",
                EventData.of("Button", "SelectPrivacy")
                        .append("IsOpen", "true"),
                false
        );

        // Invite-only option
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#StepContent #InviteOnlyOption",
                EventData.of("Button", "SelectPrivacy")
                        .append("IsOpen", "false"),
                false
        );

        // Highlight current selection
        if (isOpen) {
            cmd.set("#StepContent #OpenOption.BorderColor", "#55FF55");
        } else {
            cmd.set("#StepContent #InviteOnlyOption.BorderColor", "#55FF55");
        }
    }

    private void buildReviewStep(UICommandBuilder cmd, UIEventBuilder events) {
        cmd.set("#StepTitle.Text", "Review & Create");
        cmd.set("#StepDescription.Text", "Review your faction settings");

        // Append review summary
        cmd.append("#StepContent", "HyperFactions/faction_review.ui");
        cmd.set("#StepContent #NameValue.Text", factionName);
        cmd.set("#StepContent #DescriptionValue.Text", factionDescription.isEmpty() ? "(None)" : factionDescription);
        cmd.set("#StepContent #ColorPreview.BackgroundColor", factionColor);
        cmd.set("#StepContent #PrivacyValue.Text", isOpen ? "Open (anyone can join)" : "Invite-only");

        // Create button (replaces Next button)
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#NextBtn",
                EventData.of("Button", "Create"),
                false
        );
        cmd.set("#NextBtn.Text", "Create Faction");
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                CreateFactionData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null || data.button == null) {
            return;
        }

        UUID uuid = playerRef.getUuid();

        switch (data.button) {
            case "NextFromName" -> {
                if (data.name == null || data.name.trim().isEmpty()) {
                    player.sendMessage(Message.raw("Please enter a faction name.").color("#FF5555"));
                    return;
                }
                factionName = data.name.trim();
                currentStep = 2;
                guiManager.openCreateFaction(player, ref, store, playerRef);
            }

            case "NextFromDescription" -> {
                factionDescription = data.description != null ? data.description.trim() : "";
                currentStep = 3;
                guiManager.openCreateFaction(player, ref, store, playerRef);
            }

            case "FactionColorSelect" -> {
                if (data.color != null) {
                    factionColor = data.color;
                }
                currentStep = 4;
                guiManager.openCreateFaction(player, ref, store, playerRef);
            }

            case "SelectPrivacy" -> {
                isOpen = "true".equals(data.isOpen);
                currentStep = 5;
                guiManager.openCreateFaction(player, ref, store, playerRef);
            }

            case "Back" -> {
                if (currentStep > 1) {
                    currentStep--;
                    guiManager.openCreateFaction(player, ref, store, playerRef);
                }
            }

            case "Cancel" -> {
                guiManager.closePage(player, ref, store);
                player.sendMessage(Message.raw("Faction creation cancelled.").color("#FFAA00"));
            }

            case "Create" -> {
                // Validate all fields
                if (factionName.isEmpty()) {
                    player.sendMessage(Message.raw("Faction name is required.").color("#FF5555"));
                    currentStep = 1;
                    guiManager.openCreateFaction(player, ref, store, playerRef);
                    return;
                }

                // Create the faction
                FactionManager.FactionResult result = factionManager.createFaction(
                        factionName, uuid, playerRef.getUsername()
                );

                if (result == FactionManager.FactionResult.SUCCESS) {
                    Faction faction = factionManager.getPlayerFaction(uuid);
                    if (faction != null) {
                        // Apply settings
                        Faction updatedFaction = faction;
                        if (!factionDescription.isEmpty()) {
                            updatedFaction = updatedFaction.withDescription(factionDescription);
                        }
                        if (!factionColor.equals("#55FFFF")) {
                            updatedFaction = updatedFaction.withColor(factionColor);
                        }
                        if (isOpen) {
                            updatedFaction = updatedFaction.withOpen(true);
                        }
                        // Save updated faction
                        factionManager.updateFaction(updatedFaction);

                        guiManager.closePage(player, ref, store);
                        player.sendMessage(
                                Message.raw("Faction '").color("#AAAAAA")
                                        .insert(Message.raw(factionName).color(factionColor))
                                        .insert(Message.raw("' created successfully!").color("#55FF55"))
                        );

                        // Open faction dashboard
                        guiManager.openFactionMain(player, ref, store, playerRef);
                    }
                } else {
                    player.sendMessage(Message.raw("Failed to create faction: " + result).color("#FF5555"));
                }
            }
        }
    }
}
