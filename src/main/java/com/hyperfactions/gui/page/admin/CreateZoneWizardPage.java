package com.hyperfactions.gui.page.admin;

import com.hyperfactions.data.Zone;
import com.hyperfactions.data.ZoneType;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.admin.data.AdminZoneData;
import com.hyperfactions.manager.ZoneManager;
import com.hyperfactions.util.ChunkUtil;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Create Zone Wizard page - enter zone name and select type.
 * After creation, opens the zone map for claiming chunks.
 */
public class CreateZoneWizardPage extends InteractiveCustomUIPage<AdminZoneData> {

    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 32;

    private final PlayerRef playerRef;
    private final ZoneManager zoneManager;
    private final GuiManager guiManager;

    // Selected zone type (immutable - set via constructor for fresh page pattern)
    private final ZoneType selectedType;

    // Preserved name for page rebuilds (e.g., when switching zone types)
    private final String preservedName;

    /**
     * Default constructor - opens with SafeZone selected.
     */
    public CreateZoneWizardPage(PlayerRef playerRef,
                                ZoneManager zoneManager,
                                GuiManager guiManager) {
        this(playerRef, zoneManager, guiManager, ZoneType.SAFE, "");
    }

    /**
     * Constructor with selected zone type.
     */
    public CreateZoneWizardPage(PlayerRef playerRef,
                                ZoneManager zoneManager,
                                GuiManager guiManager,
                                ZoneType selectedType) {
        this(playerRef, zoneManager, guiManager, selectedType, "");
    }

    /**
     * Constructor with selected zone type and preserved name.
     * Used when user switches zone type to preserve the entered name.
     */
    public CreateZoneWizardPage(PlayerRef playerRef,
                                ZoneManager zoneManager,
                                GuiManager guiManager,
                                ZoneType selectedType,
                                String preservedName) {
        super(playerRef, CustomPageLifetime.CanDismiss, AdminZoneData.CODEC);
        this.playerRef = playerRef;
        this.zoneManager = zoneManager;
        this.guiManager = guiManager;
        this.selectedType = selectedType != null ? selectedType : ZoneType.SAFE;
        this.preservedName = preservedName != null ? preservedName : "";
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {
        Logger.info("[CreateZoneWizardPage] build() for type '%s', preservedName='%s'",
                selectedType.name(), preservedName);

        // Load the template
        cmd.append("HyperFactions/admin/create_zone_wizard.ui");

        // Restore preserved input value (for type selection rebuilds)
        if (!preservedName.isEmpty()) {
            cmd.set("#NameInput.Value", preservedName);
        }

        // Show current selection
        if (selectedType == ZoneType.SAFE) {
            cmd.set("#CurrentType.TextSpans", Message.raw("SafeZone").color("#2dd4bf"));
            cmd.set("#TypeDescription.Text", "Protected area - no PvP, no explosions");
        } else {
            cmd.set("#CurrentType.TextSpans", Message.raw("WarZone").color("#c084fc"));
            cmd.set("#TypeDescription.Text", "Combat area - PvP enabled, no faction protection");
        }

        // Type selection buttons - capture name when switching
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#SafeZoneBtn",
                EventData.of("Button", "SelectType")
                        .append("ZoneType", "safe")
                        .append("@Name", "#NameInput.Value"),
                false
        );

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#WarZoneBtn",
                EventData.of("Button", "SelectType")
                        .append("ZoneType", "war")
                        .append("@Name", "#NameInput.Value"),
                false
        );

        // Create button (creates zone with no chunks)
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#CreateBtn",
                EventData.of("Button", "Create")
                        .append("ZoneType", selectedType == ZoneType.SAFE ? "safe" : "war")
                        .append("@Name", "#NameInput.Value"),
                false
        );

        // Create & Claim button (creates zone and claims current chunk)
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#CreateClaimBtn",
                EventData.of("Button", "CreateClaim")
                        .append("ZoneType", selectedType == ZoneType.SAFE ? "safe" : "war")
                        .append("@Name", "#NameInput.Value"),
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
                                AdminZoneData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        World world = player != null ? player.getWorld() : null;
        String worldName = world != null ? world.getName() : "world";

        if (player == null || playerRef == null || data.button == null) {
            sendUpdate();
            return;
        }

        switch (data.button) {
            case "Back" -> guiManager.openAdminZone(player, ref, store, playerRef);

            case "SelectType" -> {
                // Open fresh page with new type selection, preserving the entered name
                ZoneType newType = "war".equals(data.zoneType) ? ZoneType.WAR : ZoneType.SAFE;
                String name = data.inputName != null ? data.inputName : preservedName;
                guiManager.openCreateZoneWizard(player, ref, store, playerRef, newType, name);
            }

            case "Create" -> handleCreate(player, ref, store, playerRef, data, worldName, false);

            case "CreateClaim" -> handleCreate(player, ref, store, playerRef, data, worldName, true);

            default -> sendUpdate();
        }
    }

    private void handleCreate(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                              PlayerRef playerRef, AdminZoneData data, String worldName,
                              boolean claimCurrentChunk) {
        String name = data.inputName != null ? data.inputName.trim() : "";

        // Validate zone name
        if (name.isEmpty()) {
            player.sendMessage(Message.raw("Please enter a zone name.").color("#FF5555"));
            sendUpdate();
            return;
        }

        if (name.length() < MIN_NAME_LENGTH) {
            player.sendMessage(Message.raw("Zone name must be at least " + MIN_NAME_LENGTH + " characters.").color("#FF5555"));
            sendUpdate();
            return;
        }

        if (name.length() > MAX_NAME_LENGTH) {
            player.sendMessage(Message.raw("Zone name cannot exceed " + MAX_NAME_LENGTH + " characters.").color("#FF5555"));
            sendUpdate();
            return;
        }

        // Check if name is already taken
        if (zoneManager.getZoneByName(name) != null) {
            player.sendMessage(Message.raw("A zone with this name already exists.").color("#FF5555"));
            sendUpdate();
            return;
        }

        // Get zone type from data
        ZoneType type = "war".equals(data.zoneType) ? ZoneType.WAR : ZoneType.SAFE;

        // Create the zone
        ZoneManager.ZoneResult result = zoneManager.createZone(name, type, worldName, playerRef.getUuid());

        switch (result) {
            case SUCCESS -> {
                Zone newZone = zoneManager.getZoneByName(name);
                if (newZone != null) {
                    String typeColor = type == ZoneType.SAFE ? "#2dd4bf" : "#c084fc";
                    player.sendMessage(
                            Message.raw("Created ").color("#55FF55")
                                    .insert(Message.raw(type.getDisplayName()).color(typeColor))
                                    .insert(Message.raw(" '" + name + "'!").color("#55FF55"))
                    );

                    // Optionally claim the player's current chunk
                    if (claimCurrentChunk) {
                        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
                        if (transform != null) {
                            var position = transform.getPosition();
                            int chunkX = ChunkUtil.blockToChunk((int) position.x);
                            int chunkZ = ChunkUtil.blockToChunk((int) position.z);

                            ZoneManager.ZoneResult claimResult = zoneManager.claimChunk(
                                    newZone.id(), worldName, chunkX, chunkZ);

                            if (claimResult == ZoneManager.ZoneResult.SUCCESS) {
                                player.sendMessage(Message.raw("Claimed chunk (" + chunkX + ", " + chunkZ + ") for this zone.").color("#44cc44"));
                                // Refresh zone after claim
                                newZone = zoneManager.getZoneById(newZone.id());
                            } else {
                                player.sendMessage(Message.raw("Could not claim current chunk: " + claimResult).color("#FFAA00"));
                            }
                        }
                    } else {
                        player.sendMessage(Message.raw("Click on the map to claim chunks for this zone.").color("#888888"));
                    }

                    // Open the zone map for claiming/viewing
                    if (newZone != null) {
                        guiManager.openAdminZoneMap(player, ref, store, playerRef, newZone);
                    } else {
                        guiManager.openAdminZone(player, ref, store, playerRef);
                    }
                } else {
                    player.sendMessage(Message.raw("Zone created but could not open map.").color("#FFAA00"));
                    guiManager.openAdminZone(player, ref, store, playerRef);
                }
            }

            case NAME_TAKEN -> {
                player.sendMessage(Message.raw("A zone with this name already exists.").color("#FF5555"));
                sendUpdate();
            }

            case INVALID_NAME -> {
                player.sendMessage(Message.raw("Invalid zone name.").color("#FF5555"));
                sendUpdate();
            }

            default -> {
                player.sendMessage(Message.raw("Could not create zone: " + result).color("#FF5555"));
                sendUpdate();
            }
        }
    }
}
