package com.hyperfactions.gui.page;

import com.hyperfactions.data.*;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.NavBarHelper;
import com.hyperfactions.gui.data.ChunkMapData;
import com.hyperfactions.manager.*;
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

import java.util.UUID;

/**
 * Interactive Chunk Map page - displays a 9x9 territory grid.
 * Left-click to claim, right-click to unclaim.
 */
public class ChunkMapPage extends InteractiveCustomUIPage<ChunkMapData> {

    private static final String PAGE_ID = "map";
    private static final int GRID_RADIUS_X = 14; // 29 columns (-14 to +14)
    private static final int GRID_RADIUS_Z = 8;  // 17 rows (-8 to +8)
    private static final int CELL_SIZE = 16;     // pixels per cell

    // Color constants - clean flat design
    private static final String COLOR_OWN = "#4ade80";        // Bright green - your territory
    private static final String COLOR_ALLY = "#60a5fa";       // Bright blue - ally territory
    private static final String COLOR_ENEMY = "#f87171";      // Bright red - enemy territory
    private static final String COLOR_OTHER = "#fbbf24";      // Yellow/gold - neutral faction
    private static final String COLOR_WILDERNESS = "#1e293b"; // Dark slate - unclaimed (darker for contrast)
    private static final String COLOR_SAFEZONE = "#2dd4bf";   // Teal - safe zone
    private static final String COLOR_WARZONE = "#c084fc";    // Light purple - war zone
    private static final String COLOR_PLAYER_POS = "#ffffff"; // White - player position stands out

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final ClaimManager claimManager;
    private final RelationManager relationManager;
    private final ZoneManager zoneManager;
    private final GuiManager guiManager;

    public ChunkMapPage(PlayerRef playerRef,
                        FactionManager factionManager,
                        ClaimManager claimManager,
                        RelationManager relationManager,
                        ZoneManager zoneManager,
                        GuiManager guiManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, ChunkMapData.CODEC);
        this.playerRef = playerRef;
        this.factionManager = factionManager;
        this.claimManager = claimManager;
        this.relationManager = relationManager;
        this.zoneManager = zoneManager;
        this.guiManager = guiManager;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {
        Logger.info("[ChunkMapPage] build() started for %s", playerRef.getUsername());

        UUID viewerUuid = playerRef.getUuid();
        Faction viewerFaction = factionManager.getPlayerFaction(viewerUuid);
        Logger.info("[ChunkMapPage] viewerFaction: %s", viewerFaction != null ? viewerFaction.name() : "null");

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
        Logger.info("[ChunkMapPage] Player at chunk (%d, %d) in world '%s'", playerChunkX, playerChunkZ, worldName);

        // Load the main template
        Logger.info("[ChunkMapPage] Loading chunk_map.ui template");
        cmd.append("HyperFactions/chunk_map.ui");

        // Setup navigation bar
        Logger.info("[ChunkMapPage] Setting up nav bar");
        NavBarHelper.setupBar(playerRef, viewerFaction != null, PAGE_ID, cmd, events);
        Logger.info("[ChunkMapPage] Nav bar setup complete");

        // Current position info
        cmd.set("#PositionInfo.Text", String.format("Your Position: Chunk (%d, %d)", playerChunkX, playerChunkZ));

        // Build the 9x9 chunk grid
        Logger.info("[ChunkMapPage] Building chunk grid");
        buildChunkGrid(cmd, events, worldName, playerChunkX, playerChunkZ, viewerFaction);
        Logger.info("[ChunkMapPage] Chunk grid build complete");

        // Set Home button binding (only if player is in a faction and is officer+)
        if (viewerFaction != null) {
            var member = viewerFaction.getMember(viewerUuid);
            if (member != null && member.isOfficerOrHigher()) {
                Logger.info("[ChunkMapPage] Binding SetHome button for officer");
                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        "#SetHomeBtn",
                        EventData.of("Button", "SetHome"),
                        false
                );
            }

            // Claim stats
            PowerManager.FactionPowerStats stats = guiManager.getPowerManager().get().getFactionPowerStats(viewerFaction.id());
            cmd.set("#ClaimStats.Text", String.format("Claims: %d/%d", viewerFaction.getClaimCount(), stats.maxClaims()));
        } else {
            cmd.set("#ClaimStats.Text", "Join a faction to claim");
        }

        Logger.info("[ChunkMapPage] build() completed successfully");
    }

    /**
     * Builds the 15x9 chunk grid dynamically.
     * Creates cells inline with color baked in, then adds button overlay for clicks.
     */
    private void buildChunkGrid(UICommandBuilder cmd, UIEventBuilder events,
                                String worldName, int centerX, int centerZ,
                                Faction viewerFaction) {
        UUID viewerFactionId = viewerFaction != null ? viewerFaction.id() : null;
        boolean isOfficer = false;
        if (viewerFaction != null) {
            var member = viewerFaction.getMember(playerRef.getUuid());
            isOfficer = member != null && member.isOfficerOrHigher();
        }
        Logger.info("[ChunkMapPage] buildChunkGrid: isOfficer=%s, viewerFactionId=%s", isOfficer, viewerFactionId);

        // Build 9 rows (z-4 to z+4)
        for (int zOffset = -GRID_RADIUS_Z; zOffset <= GRID_RADIUS_Z; zOffset++) {
            int rowIndex = zOffset + GRID_RADIUS_Z; // 0-8
            int chunkZ = centerZ + zOffset;

            // Create row container
            cmd.appendInline("#ChunkGrid", "Group { LayoutMode: Left; }");

            // Build 15 cells per row (x-7 to x+7)
            for (int xOffset = -GRID_RADIUS_X; xOffset <= GRID_RADIUS_X; xOffset++) {
                int colIndex = xOffset + GRID_RADIUS_X; // 0-14
                int chunkX = centerX + xOffset;

                // Get chunk info and color (use actual territory color, not special player color)
                ChunkInfo info = getChunkInfo(worldName, chunkX, chunkZ, viewerFactionId);
                boolean isPlayerPos = (xOffset == 0 && zOffset == 0);

                // Create flat cell - white for player position, territory color otherwise
                String cellColor = isPlayerPos ? COLOR_PLAYER_POS : info.color;
                cmd.appendInline("#ChunkGrid[" + rowIndex + "]",
                        "Group { Anchor: (Width: " + CELL_SIZE + ", Height: " + CELL_SIZE + "); " +
                        "Background: (Color: " + cellColor + "); }");

                // Add button overlay for click events
                String cellSelector = "#ChunkGrid[" + rowIndex + "][" + colIndex + "]";
                cmd.append(cellSelector, "HyperFactions/chunk_btn.ui");

                // Bind click events based on chunk ownership (only for officers)
                if (isOfficer) {
                    bindChunkEvents(events, cellSelector + " #Btn", chunkX, chunkZ, info, viewerFactionId);
                }
            }
        }
        Logger.info("[ChunkMapPage] buildChunkGrid: completed %dx%d grid", (GRID_RADIUS_X * 2 + 1), (GRID_RADIUS_Z * 2 + 1));
    }

    /**
     * Binds click events to a chunk cell based on its ownership state.
     * Events are bound to the #Cell Button element.
     */
    private void bindChunkEvents(UIEventBuilder events, String cellSelector,
                                 int chunkX, int chunkZ, ChunkInfo info, UUID viewerFactionId) {
        switch (info.type) {
            case WILDERNESS:
                // Left-click wilderness to claim
                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        cellSelector,
                        EventData.of("Button", "Claim")
                                .append("ChunkX", String.valueOf(chunkX))
                                .append("ChunkZ", String.valueOf(chunkZ)),
                        false
                );
                break;

            case OWN:
                // Right-click own territory to unclaim
                events.addEventBinding(
                        CustomUIEventBindingType.RightClicking,
                        cellSelector,
                        EventData.of("Button", "Unclaim")
                                .append("ChunkX", String.valueOf(chunkX))
                                .append("ChunkZ", String.valueOf(chunkZ)),
                        false
                );
                break;

            case ENEMY:
                // Left-click enemy territory to attempt overclaim
                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        cellSelector,
                        EventData.of("Button", "Overclaim")
                                .append("ChunkX", String.valueOf(chunkX))
                                .append("ChunkZ", String.valueOf(chunkZ)),
                        false
                );
                break;

            // ALLY, OTHER, SAFEZONE, WARZONE - no click actions
            default:
                break;
        }
    }

    /**
     * Gets information about a chunk's ownership and display color.
     */
    private ChunkInfo getChunkInfo(String worldName, int chunkX, int chunkZ, UUID viewerFactionId) {
        // Check for zone first
        Zone zone = zoneManager.getZone(worldName, chunkX, chunkZ);
        if (zone != null) {
            if (zone.type() == ZoneType.SAFE) {
                return new ChunkInfo(ChunkType.SAFEZONE, COLOR_SAFEZONE, null);
            } else {
                return new ChunkInfo(ChunkType.WARZONE, COLOR_WARZONE, null);
            }
        }

        // Check claim ownership
        UUID ownerId = claimManager.getClaimOwner(worldName, chunkX, chunkZ);
        if (ownerId == null) {
            return new ChunkInfo(ChunkType.WILDERNESS, COLOR_WILDERNESS, null);
        }

        // It's claimed - determine relation
        if (viewerFactionId != null && ownerId.equals(viewerFactionId)) {
            return new ChunkInfo(ChunkType.OWN, COLOR_OWN, ownerId);
        }

        if (viewerFactionId != null) {
            RelationType relation = relationManager.getRelation(viewerFactionId, ownerId);
            return switch (relation) {
                case ALLY -> new ChunkInfo(ChunkType.ALLY, COLOR_ALLY, ownerId);
                case ENEMY -> new ChunkInfo(ChunkType.ENEMY, COLOR_ENEMY, ownerId);
                case NEUTRAL -> new ChunkInfo(ChunkType.OTHER, COLOR_OTHER, ownerId);
            };
        }

        // Viewer has no faction, show as other
        return new ChunkInfo(ChunkType.OTHER, COLOR_OTHER, ownerId);
    }

    /**
     * Chunk type for event binding decisions.
     */
    private enum ChunkType {
        WILDERNESS, OWN, ALLY, ENEMY, OTHER, SAFEZONE, WARZONE
    }

    /**
     * Information about a chunk for display and interaction.
     */
    private record ChunkInfo(ChunkType type, String color, UUID ownerId) {}

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                ChunkMapData data) {
        super.handleDataEvent(ref, store, data);
        Logger.info("[ChunkMapPage] handleDataEvent: button=%s, chunkX=%d, chunkZ=%d", data.button, data.chunkX, data.chunkZ);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null || data.button == null) {
            Logger.info("[ChunkMapPage] handleDataEvent: early return (player=%s, playerRef=%s, button=%s)",
                    player != null, playerRef != null, data.button);
            sendUpdate();
            return;
        }

        Faction viewerFaction = factionManager.getPlayerFaction(playerRef.getUuid());
        World world = player.getWorld();
        String worldName = world != null ? world.getName() : "world";

        // Handle navigation
        if (NavBarHelper.handleNavEvent(data, player, ref, store, playerRef, viewerFaction, guiManager)) {
            Logger.info("[ChunkMapPage] handleDataEvent: handled by NavBarHelper");
            return;
        }

        switch (data.button) {
            case "Claim" -> handleClaim(player, playerRef, worldName, data.chunkX, data.chunkZ, ref, store);
            case "Unclaim" -> handleUnclaim(player, playerRef, worldName, data.chunkX, data.chunkZ, ref, store);
            case "Overclaim" -> handleOverclaim(player, playerRef, worldName, data.chunkX, data.chunkZ, ref, store);
            case "SetHome" -> handleSetHome(player, playerRef, ref, store);
            default -> {
                Logger.info("[ChunkMapPage] handleDataEvent: unknown button '%s'", data.button);
                sendUpdate();
            }
        }
    }

    private void handleClaim(Player player, PlayerRef playerRef, String worldName,
                             int chunkX, int chunkZ, Ref<EntityStore> ref, Store<EntityStore> store) {
        Logger.info("[ChunkMapPage] handleClaim: chunk (%d, %d) in %s", chunkX, chunkZ, worldName);
        ClaimManager.ClaimResult result = claimManager.claim(playerRef.getUuid(), worldName, chunkX, chunkZ);
        Logger.info("[ChunkMapPage] handleClaim: result=%s", result);

        Message message = switch (result) {
            case SUCCESS -> Message.raw("Claimed chunk at (" + chunkX + ", " + chunkZ + ")!").color("#44cc44");
            case NOT_IN_FACTION -> Message.raw("You must be in a faction to claim territory.").color("#ff5555");
            case NOT_OFFICER -> Message.raw("Only officers and leaders can claim territory.").color("#ff5555");
            case ALREADY_CLAIMED_SELF -> Message.raw("You already own this chunk.").color("#ffaa00");
            case ALREADY_CLAIMED_OTHER -> Message.raw("This chunk is already claimed by another faction.").color("#ff5555");
            case NOT_ADJACENT -> Message.raw("You can only claim chunks adjacent to your territory.").color("#ff5555");
            case MAX_CLAIMS_REACHED -> Message.raw("You have reached your maximum claim limit.").color("#ff5555");
            case WORLD_NOT_ALLOWED -> Message.raw("Claiming is not allowed in this world.").color("#ff5555");
            default -> Message.raw("Failed to claim chunk.").color("#ff5555");
        };

        player.sendMessage(message);

        // Refresh the map
        guiManager.openChunkMap(player, ref, store, playerRef);
    }

    private void handleUnclaim(Player player, PlayerRef playerRef, String worldName,
                               int chunkX, int chunkZ, Ref<EntityStore> ref, Store<EntityStore> store) {
        Logger.info("[ChunkMapPage] handleUnclaim: chunk (%d, %d) in %s", chunkX, chunkZ, worldName);
        ClaimManager.ClaimResult result = claimManager.unclaim(playerRef.getUuid(), worldName, chunkX, chunkZ);
        Logger.info("[ChunkMapPage] handleUnclaim: result=%s", result);

        Message message = switch (result) {
            case SUCCESS -> Message.raw("Unclaimed chunk at (" + chunkX + ", " + chunkZ + ").").color("#44cc44");
            case NOT_IN_FACTION -> Message.raw("You must be in a faction.").color("#ff5555");
            case NOT_OFFICER -> Message.raw("Only officers and leaders can unclaim territory.").color("#ff5555");
            case CHUNK_NOT_CLAIMED -> Message.raw("This chunk is not claimed.").color("#ffaa00");
            case NOT_YOUR_CLAIM -> Message.raw("This chunk belongs to another faction.").color("#ff5555");
            case CANNOT_UNCLAIM_HOME -> Message.raw("Cannot unclaim the chunk containing your faction home.").color("#ff5555");
            default -> Message.raw("Failed to unclaim chunk.").color("#ff5555");
        };

        player.sendMessage(message);

        // Refresh the map
        guiManager.openChunkMap(player, ref, store, playerRef);
    }

    private void handleOverclaim(Player player, PlayerRef playerRef, String worldName,
                                 int chunkX, int chunkZ, Ref<EntityStore> ref, Store<EntityStore> store) {
        Logger.info("[ChunkMapPage] handleOverclaim: chunk (%d, %d) in %s", chunkX, chunkZ, worldName);
        ClaimManager.ClaimResult result = claimManager.overclaim(playerRef.getUuid(), worldName, chunkX, chunkZ);
        Logger.info("[ChunkMapPage] handleOverclaim: result=%s", result);

        Message message = switch (result) {
            case SUCCESS -> Message.raw("Overclaimed enemy chunk at (" + chunkX + ", " + chunkZ + ")!").color("#44cc44");
            case NOT_IN_FACTION -> Message.raw("You must be in a faction.").color("#ff5555");
            case NOT_OFFICER -> Message.raw("Only officers and leaders can overclaim territory.").color("#ff5555");
            case ALREADY_CLAIMED_SELF -> Message.raw("You already own this chunk.").color("#ffaa00");
            case ALREADY_CLAIMED_ALLY -> Message.raw("You cannot overclaim allied territory.").color("#ff5555");
            case TARGET_HAS_POWER -> Message.raw("This faction has enough power to defend their territory.").color("#ff5555");
            case MAX_CLAIMS_REACHED -> Message.raw("You have reached your maximum claim limit.").color("#ff5555");
            default -> Message.raw("Failed to overclaim chunk.").color("#ff5555");
        };

        player.sendMessage(message);

        // Refresh the map
        guiManager.openChunkMap(player, ref, store, playerRef);
    }

    private void handleSetHome(Player player, PlayerRef playerRef,
                               Ref<EntityStore> ref, Store<EntityStore> store) {
        Logger.info("[ChunkMapPage] handleSetHome started");
        UUID viewerUuid = playerRef.getUuid();
        Faction faction = factionManager.getPlayerFaction(viewerUuid);

        if (faction == null) {
            player.sendMessage(Message.raw("You must be in a faction to set home.").color("#ff5555"));
            sendUpdate();
            return;
        }

        var member = faction.getMember(viewerUuid);
        if (member == null || !member.isOfficerOrHigher()) {
            player.sendMessage(Message.raw("Only officers and leaders can set the faction home.").color("#ff5555"));
            sendUpdate();
            return;
        }

        // Get current position
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        World world = player.getWorld();
        if (transform == null || world == null) {
            player.sendMessage(Message.raw("Failed to get your position.").color("#ff5555"));
            sendUpdate();
            return;
        }

        var pos = transform.getPosition();
        String worldName = world.getName();
        int chunkX = ChunkUtil.blockToChunk((int) pos.x);
        int chunkZ = ChunkUtil.blockToChunk((int) pos.z);

        // Check if standing in own territory
        UUID claimOwner = claimManager.getClaimOwner(worldName, chunkX, chunkZ);
        if (claimOwner == null || !claimOwner.equals(faction.id())) {
            player.sendMessage(Message.raw("You must be standing in your faction's territory to set home.").color("#ff5555"));
            sendUpdate();
            return;
        }

        // Set the home
        Faction.FactionHome home = Faction.FactionHome.create(worldName, pos.x, pos.y, pos.z, 0f, 0f, viewerUuid);
        FactionManager.FactionResult result = factionManager.setHome(faction.id(), home, viewerUuid);
        Logger.info("[ChunkMapPage] handleSetHome: result=%s", result);

        if (result == FactionManager.FactionResult.SUCCESS) {
            player.sendMessage(Message.raw("Faction home set at your current location!").color("#44cc44"));
        } else {
            player.sendMessage(Message.raw("Failed to set faction home.").color("#ff5555"));
        }

        // Refresh the map
        guiManager.openChunkMap(player, ref, store, playerRef);
    }
}
