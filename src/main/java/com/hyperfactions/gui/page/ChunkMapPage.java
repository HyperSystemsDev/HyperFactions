package com.hyperfactions.gui.page;

import com.hyperfactions.data.*;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.data.ChunkMapData;
import com.hyperfactions.manager.*;
import com.hyperfactions.util.ChunkUtil;
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

import java.util.UUID;

/**
 * Chunk Map page - displays a 15x15 grid of territory claims centered on the player.
 */
public class ChunkMapPage extends InteractiveCustomUIPage<ChunkMapData> {

    private static final int MAP_SIZE = 15; // 15x15 grid

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final ClaimManager claimManager;
    private final ZoneManager zoneManager;
    private final GuiManager guiManager;

    private int offsetX = 0;
    private int offsetZ = 0;

    public ChunkMapPage(PlayerRef playerRef,
                        FactionManager factionManager,
                        ClaimManager claimManager,
                        ZoneManager zoneManager,
                        GuiManager guiManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, ChunkMapData.CODEC);
        this.playerRef = playerRef;
        this.factionManager = factionManager;
        this.claimManager = claimManager;
        this.zoneManager = zoneManager;
        this.guiManager = guiManager;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        UUID viewerUuid = playerRef.getUuid();
        Faction viewerFaction = factionManager.getPlayerFaction(viewerUuid);

        // Get player's current position
        Player player = store.getComponent(ref, Player.getComponentType());
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        World world = player != null ? player.getWorld() : null;
        String worldName = world != null ? world.getName() : "world";

        int playerChunkX = 0;
        int playerChunkZ = 0;
        if (transform != null) {
            var position = transform.getPosition();
            playerChunkX = ChunkUtil.blockToChunk((int) position.x);
            playerChunkZ = ChunkUtil.blockToChunk((int) position.z);
        }

        // Center of the map with offset
        int centerX = playerChunkX + offsetX;
        int centerZ = playerChunkZ + offsetZ;
        int halfSize = MAP_SIZE / 2;

        // Load the main template
        cmd.append("HyperFactions/chunk_map.ui");

        // Set title
        cmd.set("#Title #TitleText.Text", "Territory Map");

        // Current position info
        cmd.set("#PositionInfo.Text", String.format("Position: %d, %d", playerChunkX, playerChunkZ));

        // Build the 15x15 grid
        for (int gridZ = 0; gridZ < MAP_SIZE; gridZ++) {
            for (int gridX = 0; gridX < MAP_SIZE; gridX++) {
                int chunkX = centerX - halfSize + gridX;
                int chunkZ = centerZ - halfSize + gridZ;

                String cellId = "#Cell_" + gridX + "_" + gridZ;

                // Determine cell color based on claim/zone status
                String color = getCellColor(worldName, chunkX, chunkZ, viewerFaction, chunkX == playerChunkX && chunkZ == playerChunkZ);
                String label = getCellLabel(worldName, chunkX, chunkZ, viewerFaction);

                // Set cell properties
                cmd.set(cellId + ".Background.Color", color);
                cmd.set(cellId + " #Label.Text", label);

                // Add click binding for claiming/info
                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        cellId,
                        EventData.of("Button", "CellClick")
                                .append("ChunkX", String.valueOf(chunkX))
                                .append("ChunkZ", String.valueOf(chunkZ)),
                        false
                );
            }
        }

        // Legend
        cmd.set("#LegendYou.Background.Color", "#FFFFFF");
        cmd.set("#LegendOwn.Background.Color", "#00FF00");
        cmd.set("#LegendAlly.Background.Color", "#00AAFF");
        cmd.set("#LegendEnemy.Background.Color", "#FF0000");
        cmd.set("#LegendOther.Background.Color", "#FFAA00");
        cmd.set("#LegendWilderness.Background.Color", "#444444");
        cmd.set("#LegendSafeZone.Background.Color", "#00FFFF");
        cmd.set("#LegendWarZone.Background.Color", "#FF00FF");

        // Navigation buttons
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#NavUp",
                EventData.of("Button", "NavUp")
                        .append("OffsetX", String.valueOf(offsetX))
                        .append("OffsetZ", String.valueOf(offsetZ - 5)),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#NavDown",
                EventData.of("Button", "NavDown")
                        .append("OffsetX", String.valueOf(offsetX))
                        .append("OffsetZ", String.valueOf(offsetZ + 5)),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#NavLeft",
                EventData.of("Button", "NavLeft")
                        .append("OffsetX", String.valueOf(offsetX - 5))
                        .append("OffsetZ", String.valueOf(offsetZ)),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#NavRight",
                EventData.of("Button", "NavRight")
                        .append("OffsetX", String.valueOf(offsetX + 5))
                        .append("OffsetZ", String.valueOf(offsetZ)),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#NavCenter",
                EventData.of("Button", "NavCenter")
                        .append("OffsetX", "0")
                        .append("OffsetZ", "0"),
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

    private String getCellColor(String worldName, int chunkX, int chunkZ, Faction viewerFaction, boolean isPlayerChunk) {
        // Check player position first
        if (isPlayerChunk) {
            return "#FFFFFF"; // White for player position
        }

        // Check zones
        Zone zone = zoneManager.getZone(worldName, chunkX, chunkZ);
        if (zone != null) {
            return switch (zone.type()) {
                case SAFE -> "#00FFFF"; // Cyan for safe zone
                case WAR -> "#FF00FF";  // Magenta for war zone
            };
        }

        // Check claims
        UUID ownerId = claimManager.getClaimOwner(worldName, chunkX, chunkZ);
        if (ownerId == null) {
            return "#444444"; // Dark gray for wilderness
        }

        Faction ownerFaction = factionManager.getFaction(ownerId);
        if (ownerFaction == null) {
            return "#444444";
        }

        if (viewerFaction != null) {
            if (ownerFaction.id().equals(viewerFaction.id())) {
                return "#00FF00"; // Green for own faction
            }

            RelationType relation = viewerFaction.getRelationType(ownerFaction.id());
            return switch (relation) {
                case ALLY -> "#00AAFF";   // Blue for allies
                case ENEMY -> "#FF0000";  // Red for enemies
                case NEUTRAL -> "#FFAA00"; // Orange for others
            };
        }

        return "#FFAA00"; // Orange for other factions
    }

    private String getCellLabel(String worldName, int chunkX, int chunkZ, Faction viewerFaction) {
        // Check zones
        Zone zone = zoneManager.getZone(worldName, chunkX, chunkZ);
        if (zone != null) {
            return zone.type() == ZoneType.SAFE ? "S" : "W";
        }

        // Check claims
        UUID ownerId = claimManager.getClaimOwner(worldName, chunkX, chunkZ);
        if (ownerId == null) {
            return "";
        }

        Faction ownerFaction = factionManager.getFaction(ownerId);
        if (ownerFaction == null) {
            return "?";
        }

        // Get first letter of faction name
        String name = ownerFaction.name();
        return name.length() > 0 ? name.substring(0, 1).toUpperCase() : "?";
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                ChunkMapData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null || data.button == null) {
            return;
        }

        switch (data.button) {
            case "Back" -> guiManager.openFactionMain(player, ref, store, playerRef);

            case "NavUp", "NavDown", "NavLeft", "NavRight", "NavCenter" -> {
                offsetX = data.offsetX;
                offsetZ = data.offsetZ;
                guiManager.openChunkMap(player, ref, store, playerRef);
            }

            case "CellClick" -> {
                World world = player.getWorld();
                String worldName = world != null ? world.getName() : "world";

                // Show chunk info
                Zone zone = zoneManager.getZone(worldName, data.chunkX, data.chunkZ);
                if (zone != null) {
                    player.sendMessage(Message.raw(String.format(
                            "Chunk (%d, %d): %s Zone - %s",
                            data.chunkX, data.chunkZ,
                            zone.type().name(), zone.name()
                    )).color("#00FFFF"));
                    return;
                }

                UUID ownerId = claimManager.getClaimOwner(worldName, data.chunkX, data.chunkZ);
                if (ownerId == null) {
                    player.sendMessage(Message.raw(String.format(
                            "Chunk (%d, %d): Wilderness (unclaimed)",
                            data.chunkX, data.chunkZ
                    )).color("#888888"));
                } else {
                    Faction owner = factionManager.getFaction(ownerId);
                    String ownerName = owner != null ? owner.name() : "Unknown";
                    player.sendMessage(Message.raw(String.format(
                            "Chunk (%d, %d): Claimed by %s",
                            data.chunkX, data.chunkZ, ownerName
                    )).color("#FFAA00"));
                }
            }
        }
    }
}
