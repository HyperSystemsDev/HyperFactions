package com.hyperfactions.gui.page;

import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.data.RenameModalData;
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
 * Modal for renaming a faction.
 * Validates name uniqueness before saving.
 */
public class RenameModalPage extends InteractiveCustomUIPage<RenameModalData> {

    private static final int MIN_NAME_LENGTH = 3;
    private static final int MAX_NAME_LENGTH = 32;

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final GuiManager guiManager;
    private final Faction faction;

    public RenameModalPage(PlayerRef playerRef,
                           FactionManager factionManager,
                           GuiManager guiManager,
                           Faction faction) {
        super(playerRef, CustomPageLifetime.CanDismiss, RenameModalData.CODEC);
        this.playerRef = playerRef;
        this.factionManager = factionManager;
        this.guiManager = guiManager;
        this.faction = faction;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        // Load the modal template
        cmd.append("HyperFactions/rename_modal.ui");

        // Show current name
        cmd.set("#CurrentName.Text", faction.name());

        // Cancel button
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#CancelBtn",
                EventData.of("Button", "Cancel"),
                false
        );

        // Save button - captures text input value
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#SaveBtn",
                EventData.of("Button", "Save").append("@Name", "#NameInput.Value"),
                false
        );
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                RenameModalData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null || data.button == null) {
            return;
        }

        UUID uuid = playerRef.getUuid();
        FactionMember member = faction.getMember(uuid);

        // Verify officer permission
        if (member == null || member.role().getLevel() < FactionRole.OFFICER.getLevel()) {
            player.sendMessage(Message.raw("You don't have permission to rename the faction.").color("#FF5555"));
            guiManager.openFactionSettings(player, ref, store, playerRef,
                    factionManager.getFaction(faction.id()));
            return;
        }

        switch (data.button) {
            case "Cancel" -> {
                guiManager.openFactionSettings(player, ref, store, playerRef,
                        factionManager.getFaction(faction.id()));
            }

            case "Save" -> {
                String newName = data.name;

                // Validation
                if (newName == null || newName.trim().isEmpty()) {
                    player.sendMessage(Message.raw("Please enter a faction name.").color("#FF5555"));
                    sendUpdate();
                    return;
                }

                newName = newName.trim();

                if (newName.length() < MIN_NAME_LENGTH) {
                    player.sendMessage(Message.raw("Faction name must be at least " + MIN_NAME_LENGTH + " characters.").color("#FF5555"));
                    sendUpdate();
                    return;
                }

                if (newName.length() > MAX_NAME_LENGTH) {
                    player.sendMessage(Message.raw("Faction name cannot exceed " + MAX_NAME_LENGTH + " characters.").color("#FF5555"));
                    sendUpdate();
                    return;
                }

                // Check if name is the same
                if (newName.equalsIgnoreCase(faction.name())) {
                    player.sendMessage(Message.raw("That's already your faction's name.").color("#FFAA00"));
                    sendUpdate();
                    return;
                }

                // Check uniqueness
                Faction existing = factionManager.getFactionByName(newName);
                if (existing != null) {
                    player.sendMessage(Message.raw("A faction with that name already exists.").color("#FF5555"));
                    sendUpdate();
                    return;
                }

                // Update the faction
                String oldName = faction.name();
                Faction updatedFaction = faction.withName(newName);
                factionManager.updateFaction(updatedFaction);

                player.sendMessage(
                        Message.raw("Faction renamed from ").color("#AAAAAA")
                                .insert(Message.raw(oldName).color("#888888"))
                                .insert(Message.raw(" to ").color("#AAAAAA"))
                                .insert(Message.raw(newName).color("#00FFFF"))
                                .insert(Message.raw("!").color("#AAAAAA"))
                );

                guiManager.openFactionSettings(player, ref, store, playerRef,
                        factionManager.getFaction(faction.id()));
            }
        }
    }
}
