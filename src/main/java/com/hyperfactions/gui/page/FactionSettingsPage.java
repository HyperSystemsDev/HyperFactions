package com.hyperfactions.gui.page;

import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.component.ColorPickerModal;
import com.hyperfactions.gui.component.ConfirmationModal;
import com.hyperfactions.gui.component.InputModal;
import com.hyperfactions.gui.data.FactionSettingsData;
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
 * Faction Settings page - edit faction name, description, color, and privacy.
 * Requires officer or leader role.
 */
public class FactionSettingsPage extends InteractiveCustomUIPage<FactionSettingsData> {

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final GuiManager guiManager;
    private final Faction faction;

    public FactionSettingsPage(PlayerRef playerRef,
                               FactionManager factionManager,
                               GuiManager guiManager,
                               Faction faction) {
        super(playerRef, CustomPageLifetime.CanDismiss, FactionSettingsData.CODEC);
        this.playerRef = playerRef;
        this.factionManager = factionManager;
        this.guiManager = guiManager;
        this.faction = faction;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        UUID uuid = playerRef.getUuid();
        FactionMember member = faction.getMember(uuid);

        // Permission check - officer or leader only
        if (member == null || member.role().getLevel() < FactionRole.OFFICER.getLevel()) {
            cmd.append("HyperFactions/error_page.ui");
            cmd.set("#ErrorMessage.Text", "Only officers and leaders can change faction settings.");
            return;
        }

        // Load the settings template
        cmd.append("HyperFactions/faction_settings.ui");

        // Set page title
        cmd.set("#PageTitle.Text", "Faction Settings");

        // === General Section ===
        cmd.append("#GeneralSection", "HyperFactions/settings_section.ui");
        cmd.set("#GeneralSection #SectionTitle.Text", "General");

        // Name setting
        cmd.append("#GeneralSection #SectionContent", "HyperFactions/setting_item.ui");
        cmd.set("#GeneralSection #SectionContent #SettingName.Text", "Name");
        cmd.set("#GeneralSection #SectionContent #SettingValue.Text", faction.name());
        cmd.set("#GeneralSection #SectionContent #EditBtn.Visible", "true");
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#GeneralSection #SectionContent #EditBtn",
                EventData.of("Button", "EditName"),
                false
        );

        // Description setting
        cmd.append("#GeneralSection #SectionContent", "HyperFactions/setting_item.ui");
        cmd.set("#GeneralSection #SectionContent #SettingName.Text", "Description");
        String desc = faction.description() != null ? faction.description() : "(None)";
        cmd.set("#GeneralSection #SectionContent #SettingValue.Text", desc);
        cmd.set("#GeneralSection #SectionContent #EditBtn.Visible", "true");
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#GeneralSection #SectionContent #EditBtn",
                EventData.of("Button", "EditDescription"),
                false
        );

        // === Appearance Section ===
        cmd.append("#AppearanceSection", "HyperFactions/settings_section.ui");
        cmd.set("#AppearanceSection #SectionTitle.Text", "Appearance");

        // Color setting
        cmd.append("#AppearanceSection #SectionContent", "HyperFactions/setting_item.ui");
        cmd.set("#AppearanceSection #SectionContent #SettingName.Text", "Color");
        cmd.set("#AppearanceSection #SectionContent #ColorPreview.BackgroundColor", faction.color());
        cmd.set("#AppearanceSection #SectionContent #SettingValue.Text", faction.color());
        cmd.set("#AppearanceSection #SectionContent #EditBtn.Visible", "true");
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#AppearanceSection #SectionContent #EditBtn",
                EventData.of("Button", "EditColor"),
                false
        );

        // === Privacy Section ===
        cmd.append("#PrivacySection", "HyperFactions/settings_section.ui");
        cmd.set("#PrivacySection #SectionTitle.Text", "Privacy");

        // Open/Closed setting
        cmd.append("#PrivacySection #SectionContent", "HyperFactions/setting_toggle.ui");
        cmd.set("#PrivacySection #SectionContent #SettingName.Text", "Open Faction");
        cmd.set("#PrivacySection #SectionContent #SettingDescription.Text",
                "Allow anyone to join without an invite");
        cmd.set("#PrivacySection #SectionContent #Toggle.Value", faction.open() ? "true" : "false");
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#PrivacySection #SectionContent #Toggle",
                EventData.of("Button", "ToggleOpen")
                        .append("IsOpen", faction.open() ? "false" : "true"),
                false
        );

        // Back button
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#BackBtn",
                EventData.of("Button", "Back"),
                false
        );
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                FactionSettingsData data) {
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
            return;
        }

        switch (data.button) {
            case "EditName" -> {
                // Show input modal (would need to integrate with page reload)
                InputModal nameModal = InputModal.rename(faction.name());
                player.sendMessage(
                        Message.raw("Use ").color("#AAAAAA")
                                .insert(Message.raw("/f rename <name>").color("#55FF55"))
                                .insert(Message.raw(" to rename your faction.").color("#AAAAAA"))
                );
                guiManager.closePage(player, ref, store);
            }

            case "RenameSubmit" -> {
                if (data.name != null && !data.name.trim().isEmpty()) {
                    Faction updatedFaction = faction.withName(data.name.trim());
                    factionManager.updateFaction(updatedFaction);
                    player.sendMessage(
                            Message.raw("Faction renamed to '").color("#AAAAAA")
                                    .insert(Message.raw(data.name.trim()).color(faction.color()))
                                    .insert(Message.raw("'!").color("#55FF55"))
                    );
                    guiManager.openFactionSettings(player, ref, store, playerRef,
                            factionManager.getFaction(faction.id()));
                }
            }

            case "EditDescription" -> {
                player.sendMessage(
                        Message.raw("Use ").color("#AAAAAA")
                                .insert(Message.raw("/f desc <text>").color("#55FF55"))
                                .insert(Message.raw(" to change your faction description.").color("#AAAAAA"))
                );
                guiManager.closePage(player, ref, store);
            }

            case "DescriptionSubmit" -> {
                String newDesc = data.description != null ? data.description.trim() : null;
                Faction updatedFaction = faction.withDescription(newDesc);
                factionManager.updateFaction(updatedFaction);
                player.sendMessage(Message.raw("Faction description updated!").color("#55FF55"));
                guiManager.openFactionSettings(player, ref, store, playerRef,
                        factionManager.getFaction(faction.id()));
            }

            case "EditColor" -> {
                player.sendMessage(
                        Message.raw("Use ").color("#AAAAAA")
                                .insert(Message.raw("/f color <code>").color("#55FF55"))
                                .insert(Message.raw(" to change your faction color.").color("#AAAAAA"))
                );
                guiManager.closePage(player, ref, store);
            }

            case "FactionColorSelect" -> {
                if (data.color != null) {
                    Faction updatedFaction = faction.withColor(data.color);
                    factionManager.updateFaction(updatedFaction);
                    player.sendMessage(
                            Message.raw("Faction color updated to ").color("#AAAAAA")
                                    .insert(Message.raw("this color").color(data.color))
                                    .insert(Message.raw("!").color("#55FF55"))
                    );
                    guiManager.openFactionSettings(player, ref, store, playerRef,
                            factionManager.getFaction(faction.id()));
                }
            }

            case "ToggleOpen" -> {
                boolean newOpenState = "true".equals(data.isOpen);
                Faction updatedFaction = faction.withOpen(newOpenState);
                factionManager.updateFaction(updatedFaction);

                String status = newOpenState ? "open" : "invite-only";
                player.sendMessage(
                        Message.raw("Faction is now ").color("#AAAAAA")
                                .insert(Message.raw(status).color("#55FF55"))
                                .insert(Message.raw("!").color("#AAAAAA"))
                );

                // Refresh page
                guiManager.openFactionSettings(player, ref, store, playerRef,
                        factionManager.getFaction(faction.id()));
            }

            case "Back" -> guiManager.openFactionMain(player, ref, store, playerRef);
        }
    }
}
