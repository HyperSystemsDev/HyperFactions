package com.hyperfactions.worldmap;

import com.hyperfactions.config.HyperFactionsConfig;
import com.hyperfactions.manager.ClaimManager;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.RelationManager;
import com.hyperfactions.manager.ZoneManager;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapManager;
import com.hypixel.hytale.server.core.universe.world.worldmap.provider.IWorldMapProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages world map generator registration.
 * Handles registering the HyperFactionsWorldMap with worlds as players join them.
 * This provides colored chunk overlays showing claimed territory on the world map.
 *
 * The HyperFactionsWorldMap generates chunk images from scratch with claim
 * colors baked in during pixel generation, ensuring reliable overlay display.
 */
public class WorldMapService {

    private final FactionManager factionManager;
    private final ClaimManager claimManager;
    private final ZoneManager zoneManager;

    /** Tracks which worlds have had the generator registered */
    private final Set<String> registeredWorlds = ConcurrentHashMap.newKeySet();

    public WorldMapService(
            @NotNull FactionManager factionManager,
            @NotNull ClaimManager claimManager,
            @NotNull ZoneManager zoneManager,
            @NotNull RelationManager relationManager) {
        this.factionManager = factionManager;
        this.claimManager = claimManager;
        this.zoneManager = zoneManager;
        // relationManager parameter kept for API compatibility but not used
    }

    /**
     * Registers the HyperFactions world map provider with a world if not already registered.
     * Should be called when a player enters a world.
     *
     * This sets our custom world map provider on the WorldConfig, which returns
     * our HyperFactionsWorldMap generator that renders claim overlays.
     *
     * @param world the world to register with
     */
    public void registerProviderIfNeeded(@NotNull World world) {
        if (!HyperFactionsConfig.get().isWorldMapMarkersEnabled()) {
            return;
        }

        String worldName = world.getName();

        // Check if already registered
        if (registeredWorlds.contains(worldName)) {
            return;
        }

        try {
            // Set our world map provider on the WorldConfig (like Hyfaction does)
            // This is the correct way to override the world map generator
            world.getWorldConfig().setWorldMapProvider(new HyperFactionsWorldMapProvider());

            registeredWorlds.add(worldName);
            Logger.debugTerritory("Registered world map provider for world: %s", worldName);

        } catch (Exception e) {
            Logger.warn("Failed to register world map provider for world %s: %s", worldName, e.getMessage());
        }
    }

    /**
     * Forces a refresh of the world map for all players.
     * Call this when claims change to update the overlays.
     *
     * @param world the world to refresh
     */
    public void refreshWorldMap(@NotNull World world) {
        if (!HyperFactionsConfig.get().isWorldMapMarkersEnabled()) {
            return;
        }

        try {
            WorldMapManager worldMapManager = world.getWorldMapManager();
            // Clear cached images on server to force regeneration with new claim data
            worldMapManager.clearImages();

            // Clear each player's world map tracker to force them to re-request tiles
            // This sends ClearWorldMap packet to clients
            for (com.hypixel.hytale.server.core.entity.entities.Player player : world.getPlayers()) {
                try {
                    player.getWorldMapTracker().clear();
                } catch (Exception e) {
                    Logger.warn("Failed to clear world map tracker for player: %s", e.getMessage());
                }
            }

            Logger.debugTerritory("Cleared world map images for world: %s (%d players)",
                    world.getName(), world.getPlayers().size());
        } catch (Exception e) {
            Logger.warn("Failed to refresh world map for world %s: %s", world.getName(), e.getMessage());
        }
    }

    /**
     * Forces a refresh of the world map for all registered worlds.
     * Call this when faction data changes (color, claims, etc.).
     */
    public void refreshAllWorldMaps() {
        if (!HyperFactionsConfig.get().isWorldMapMarkersEnabled()) {
            return;
        }

        try {
            int refreshed = 0;
            for (World world : com.hypixel.hytale.server.core.universe.Universe.get().getWorlds().values()) {
                if (registeredWorlds.contains(world.getName())) {
                    refreshWorldMap(world);
                    refreshed++;
                }
            }
            Logger.debugTerritory("Refreshed world maps for %d/%d worlds", refreshed, registeredWorlds.size());
        } catch (Exception e) {
            Logger.warn("Failed to refresh all world maps: %s", e.getMessage());
        }
    }

    /**
     * Unregisters the overlay from a world.
     * Note: This restores the original generator if one was wrapped.
     *
     * @param worldName the world name
     */
    public void unregisterProvider(@NotNull String worldName) {
        registeredWorlds.remove(worldName);
    }

    /**
     * Checks if the overlay is registered for a world.
     *
     * @param worldName the world name
     * @return true if registered
     */
    public boolean isRegistered(@NotNull String worldName) {
        return registeredWorlds.contains(worldName);
    }

    /**
     * Clears all registration state.
     * Called on plugin shutdown.
     */
    public void shutdown() {
        registeredWorlds.clear();
    }
}
