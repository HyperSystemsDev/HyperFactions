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
 * Chunk Map page - displays territory information.
 * Note: Due to UI limitations, this shows a text-based summary rather than a graphical grid.
 */
public class ChunkMapPage extends InteractiveCustomUIPage<ChunkMapData> {

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final ClaimManager claimManager;
    private final ZoneManager zoneManager;
    private final GuiManager guiManager;

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

        // Load the main template
        cmd.append("HyperFactions/chunk_map.ui");

        // Current position info
        cmd.set("#PositionInfo.Text", String.format("Your Position: Chunk (%d, %d)", playerChunkX, playerChunkZ));

        // Current chunk info
        Zone zone = zoneManager.getZone(worldName, playerChunkX, playerChunkZ);
        UUID ownerId = claimManager.getClaimOwner(worldName, playerChunkX, playerChunkZ);

        String chunkStatus;
        if (zone != null) {
            chunkStatus = zone.type() == ZoneType.SAFE ? "Safe Zone" : "War Zone";
        } else if (ownerId == null) {
            chunkStatus = "Wilderness (Unclaimed)";
        } else {
            Faction owner = factionManager.getFaction(ownerId);
            if (owner != null) {
                if (viewerFaction != null && owner.id().equals(viewerFaction.id())) {
                    chunkStatus = "Your Faction's Territory";
                } else {
                    chunkStatus = "Claimed by " + owner.name();
                }
            } else {
                chunkStatus = "Claimed (Unknown)";
            }
        }
        cmd.set("#ChunkStatus.Text", chunkStatus);

        // Faction stats
        if (viewerFaction != null) {
            cmd.set("#FactionClaims.Text", "Your Claims: " + viewerFaction.claims().size());
        } else {
            cmd.set("#FactionClaims.Text", "Join a faction to claim territory");
        }

        cmd.set("#MapHint.Text", "Use /f map in chat for a visual territory map");

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
                                ChunkMapData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null || data.button == null) {
            return;
        }

        if ("Back".equals(data.button)) {
            guiManager.openFactionMain(player, ref, store, playerRef);
        }
    }
}
