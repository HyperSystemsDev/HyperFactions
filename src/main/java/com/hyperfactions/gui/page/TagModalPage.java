package com.hyperfactions.gui.page;

import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.data.TagModalData;
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
import java.util.regex.Pattern;

/**
 * Modal for editing faction tag.
 * Max 5 chars, alphanumeric only, must be unique.
 */
public class TagModalPage extends InteractiveCustomUIPage<TagModalData> {

    private static final int MIN_TAG_LENGTH = 1;
    private static final int MAX_TAG_LENGTH = 5;
    private static final Pattern TAG_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final GuiManager guiManager;
    private final Faction faction;

    public TagModalPage(PlayerRef playerRef,
                        FactionManager factionManager,
                        GuiManager guiManager,
                        Faction faction) {
        super(playerRef, CustomPageLifetime.CanDismiss, TagModalData.CODEC);
        this.playerRef = playerRef;
        this.factionManager = factionManager;
        this.guiManager = guiManager;
        this.faction = faction;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        // Load the modal template
        cmd.append("HyperFactions/tag_modal.ui");

        // Show current tag
        String currentTag = faction.tag();
        if (currentTag == null || currentTag.isEmpty()) {
            cmd.set("#CurrentTag.Text", "(None)");
        } else {
            cmd.set("#CurrentTag.Text", "[" + currentTag.toUpperCase() + "]");
        }

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
                EventData.of("Button", "Save").append("@Tag", "#TagInput.Value"),
                false
        );
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                TagModalData data) {
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
            player.sendMessage(Message.raw("You don't have permission to edit the tag.").color("#FF5555"));
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
                String newTag = data.tag;

                // Empty clears tag
                if (newTag == null || newTag.trim().isEmpty()) {
                    Faction updatedFaction = faction.withTag(null);
                    factionManager.updateFaction(updatedFaction);
                    player.sendMessage(Message.raw("Faction tag cleared.").color("#AAAAAA"));
                    guiManager.openFactionSettings(player, ref, store, playerRef,
                            factionManager.getFaction(faction.id()));
                    return;
                }

                newTag = newTag.trim().toUpperCase();

                // Validate length
                if (newTag.length() < MIN_TAG_LENGTH) {
                    player.sendMessage(Message.raw("Tag must be at least " + MIN_TAG_LENGTH + " character.").color("#FF5555"));
                    sendUpdate();
                    return;
                }

                if (newTag.length() > MAX_TAG_LENGTH) {
                    player.sendMessage(Message.raw("Tag cannot exceed " + MAX_TAG_LENGTH + " characters.").color("#FF5555"));
                    sendUpdate();
                    return;
                }

                // Validate format (alphanumeric only)
                if (!TAG_PATTERN.matcher(newTag).matches()) {
                    player.sendMessage(Message.raw("Tag can only contain letters and numbers.").color("#FF5555"));
                    sendUpdate();
                    return;
                }

                // Check if same as current
                if (newTag.equalsIgnoreCase(faction.tag())) {
                    player.sendMessage(Message.raw("That's already your faction's tag.").color("#FFAA00"));
                    sendUpdate();
                    return;
                }

                // Check uniqueness
                Faction existing = factionManager.getFactionByTag(newTag);
                if (existing != null && !existing.id().equals(faction.id())) {
                    player.sendMessage(Message.raw("A faction with that tag already exists.").color("#FF5555"));
                    sendUpdate();
                    return;
                }

                // Update the faction
                Faction updatedFaction = faction.withTag(newTag);
                factionManager.updateFaction(updatedFaction);

                player.sendMessage(
                        Message.raw("Faction tag set to ").color("#AAAAAA")
                                .insert(Message.raw("[" + newTag + "]").color("#FFAA00"))
                                .insert(Message.raw("!").color("#AAAAAA"))
                );

                guiManager.openFactionSettings(player, ref, store, playerRef,
                        factionManager.getFaction(faction.id()));
            }
        }
    }
}
