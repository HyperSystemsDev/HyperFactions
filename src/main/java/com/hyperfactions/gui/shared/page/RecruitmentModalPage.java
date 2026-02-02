package com.hyperfactions.gui.shared.page;

import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.shared.data.RecruitmentModalData;
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
 * Modal for changing faction recruitment status (Open/Invite Only).
 * Shows current status and allows selection with descriptions.
 */
public class RecruitmentModalPage extends InteractiveCustomUIPage<RecruitmentModalData> {

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final GuiManager guiManager;
    private final Faction faction;
    private final boolean adminMode;

    public RecruitmentModalPage(PlayerRef playerRef,
                                FactionManager factionManager,
                                GuiManager guiManager,
                                Faction faction) {
        this(playerRef, factionManager, guiManager, faction, false);
    }

    public RecruitmentModalPage(PlayerRef playerRef,
                                FactionManager factionManager,
                                GuiManager guiManager,
                                Faction faction,
                                boolean adminMode) {
        super(playerRef, CustomPageLifetime.CanDismiss, RecruitmentModalData.CODEC);
        this.playerRef = playerRef;
        this.factionManager = factionManager;
        this.guiManager = guiManager;
        this.faction = faction;
        this.adminMode = adminMode;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        // Load the modal template
        cmd.append("HyperFactions/shared/recruitment_modal.ui");

        // Show current status in the #CurrentStatus label
        cmd.set("#CurrentStatus.Text", faction.open() ? "Open" : "Invite Only");

        // Open button
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#OpenBtn",
                EventData.of("Button", "SetStatus").append("IsOpen", "true"),
                false
        );

        // Invite Only button
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#InviteOnlyBtn",
                EventData.of("Button", "SetStatus").append("IsOpen", "false"),
                false
        );

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
                                RecruitmentModalData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null || data.button == null) {
            return;
        }

        UUID uuid = playerRef.getUuid();
        FactionMember member = faction.getMember(uuid);

        // Verify officer permission (skip in admin mode)
        if (!adminMode && (member == null || member.role().getLevel() < FactionRole.OFFICER.getLevel())) {
            player.sendMessage(Message.raw("You don't have permission to change recruitment status.").color("#FF5555"));
            guiManager.openFactionSettings(player, ref, store, playerRef,
                    factionManager.getFaction(faction.id()));
            return;
        }

        switch (data.button) {
            case "Cancel" -> {
                if (adminMode) {
                    guiManager.openAdminFactionSettings(player, ref, store, playerRef, faction.id());
                } else {
                    guiManager.openFactionSettings(player, ref, store, playerRef,
                            factionManager.getFaction(faction.id()));
                }
            }

            case "SetStatus" -> {
                boolean newOpenState = "true".equals(data.isOpen);

                // Only update if changed
                if (faction.open() != newOpenState) {
                    Faction updatedFaction = faction.withOpen(newOpenState);
                    factionManager.updateFaction(updatedFaction);

                    String prefix = adminMode ? "[Admin] " : "";
                    String status = newOpenState ? "open" : "invite-only";
                    player.sendMessage(
                            Message.raw(prefix + "Faction is now ").color("#AAAAAA")
                                    .insert(Message.raw(status).color("#55FF55"))
                                    .insert(Message.raw("!").color("#AAAAAA"))
                    );
                }

                if (adminMode) {
                    guiManager.openAdminFactionSettings(player, ref, store, playerRef, faction.id());
                } else {
                    guiManager.openFactionSettings(player, ref, store, playerRef,
                            factionManager.getFaction(faction.id()));
                }
            }
        }
    }
}
