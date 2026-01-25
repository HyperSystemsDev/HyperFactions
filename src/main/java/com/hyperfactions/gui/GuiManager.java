package com.hyperfactions.gui;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.data.Faction;
import com.hyperfactions.gui.page.*;
import com.hyperfactions.gui.page.admin.*;
import com.hyperfactions.manager.*;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.nio.file.Path;
import java.util.UUID;
import java.util.function.Supplier;

import static com.hyperfactions.gui.FactionPageRegistry.Entry;

/**
 * Central manager for HyperFactions GUI pages.
 * Provides methods to open various UI screens.
 */
public class GuiManager {

    private final Supplier<HyperFactions> plugin;
    private final Supplier<FactionManager> factionManager;
    private final Supplier<ClaimManager> claimManager;
    private final Supplier<PowerManager> powerManager;
    private final Supplier<RelationManager> relationManager;
    private final Supplier<ZoneManager> zoneManager;
    private final Supplier<TeleportManager> teleportManager;
    private final Supplier<InviteManager> inviteManager;
    private final Supplier<Path> dataDir;

    public GuiManager(Supplier<HyperFactions> plugin,
                      Supplier<FactionManager> factionManager,
                      Supplier<ClaimManager> claimManager,
                      Supplier<PowerManager> powerManager,
                      Supplier<RelationManager> relationManager,
                      Supplier<ZoneManager> zoneManager,
                      Supplier<TeleportManager> teleportManager,
                      Supplier<InviteManager> inviteManager,
                      Supplier<Path> dataDir) {
        this.plugin = plugin;
        this.factionManager = factionManager;
        this.claimManager = claimManager;
        this.powerManager = powerManager;
        this.relationManager = relationManager;
        this.zoneManager = zoneManager;
        this.teleportManager = teleportManager;
        this.inviteManager = inviteManager;
        this.dataDir = dataDir;

        // Register all pages with the central registry
        registerPages();
    }

    /**
     * Registers all GUI pages with the central FactionPageRegistry.
     * This enables navigation between pages via the NavBarHelper.
     */
    private void registerPages() {
        FactionPageRegistry registry = FactionPageRegistry.getInstance();

        // Dashboard (main faction page)
        registry.registerEntry(new Entry(
                "dashboard",
                "Dashboard",
                null, // No permission required
                (player, ref, store, playerRef, faction, guiManager) ->
                        new FactionMainPage(playerRef, factionManager.get(), claimManager.get(),
                                powerManager.get(), teleportManager.get(), inviteManager.get(), guiManager),
                true, // Show in nav bar
                false, // Doesn't require faction
                0 // Order
        ));

        // Members page
        registry.registerEntry(new Entry(
                "members",
                "Members",
                null,
                (player, ref, store, playerRef, faction, guiManager) -> {
                    if (faction == null) return null;
                    return new FactionMembersPage(playerRef, factionManager.get(), guiManager, faction);
                },
                true,
                true, // Requires faction
                1
        ));

        // Browser page
        registry.registerEntry(new Entry(
                "browser",
                "Browse",
                null,
                (player, ref, store, playerRef, faction, guiManager) ->
                        new FactionBrowserPage(playerRef, factionManager.get(), powerManager.get(), guiManager),
                true,
                false,
                2
        ));

        // Map page
        registry.registerEntry(new Entry(
                "map",
                "Map",
                null,
                (player, ref, store, playerRef, faction, guiManager) ->
                        new ChunkMapPage(playerRef, factionManager.get(), claimManager.get(),
                                zoneManager.get(), guiManager),
                true,
                false,
                3
        ));

        // Relations page
        registry.registerEntry(new Entry(
                "relations",
                "Relations",
                null,
                (player, ref, store, playerRef, faction, guiManager) -> {
                    if (faction == null) return null;
                    return new FactionRelationsPage(playerRef, factionManager.get(),
                            relationManager.get(), guiManager, faction);
                },
                true,
                true,
                4
        ));

        // Admin page (requires permission) - accessed via /f admin, not in main nav bar
        registry.registerEntry(new Entry(
                "admin",
                "Admin",
                "hyperfactions.admin",
                (player, ref, store, playerRef, faction, guiManager) ->
                        new AdminMainPage(playerRef, factionManager.get(), powerManager.get(), guiManager),
                false,  // Not in main nav bar - separate admin GUI
                false,
                10
        ));

        Logger.debug("[GUI] Registered %d pages with FactionPageRegistry", registry.getEntries().size());
    }

    /**
     * Opens the Main Menu page - central navigation hub.
     * Displayed when player types /f alone.
     *
     * @param player    The Player entity
     * @param ref       The entity reference
     * @param store     The entity store
     * @param playerRef The PlayerRef component
     */
    public void openMainMenu(Player player, Ref<EntityStore> ref,
                             Store<EntityStore> store, PlayerRef playerRef) {
        Logger.debug("[GUI] Opening MainMenuPage for %s", playerRef.getUsername());
        try {
            PageManager pageManager = player.getPageManager();
            MainMenuPage page = new MainMenuPage(
                playerRef,
                factionManager.get(),
                this
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] MainMenuPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open MainMenuPage: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Opens the Faction Main page (dashboard) for a player.
     *
     * @param player    The Player entity
     * @param ref       The entity reference
     * @param store     The entity store
     * @param playerRef The PlayerRef component
     */
    public void openFactionMain(Player player, Ref<EntityStore> ref,
                                Store<EntityStore> store, PlayerRef playerRef) {
        Logger.debug("[GUI] Opening FactionMainPage for %s", playerRef.getUsername());
        try {
            PageManager pageManager = player.getPageManager();
            FactionMainPage page = new FactionMainPage(
                playerRef,
                factionManager.get(),
                claimManager.get(),
                powerManager.get(),
                teleportManager.get(),
                inviteManager.get(),
                this
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] FactionMainPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open FactionMainPage: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Opens the Faction Members page.
     *
     * @param player    The Player entity
     * @param ref       The entity reference
     * @param store     The entity store
     * @param playerRef The PlayerRef component
     * @param faction   The faction to show members for
     */
    public void openFactionMembers(Player player, Ref<EntityStore> ref,
                                   Store<EntityStore> store, PlayerRef playerRef,
                                   Faction faction) {
        Logger.debug("[GUI] Opening FactionMembersPage for %s", playerRef.getUsername());
        try {
            PageManager pageManager = player.getPageManager();
            FactionMembersPage page = new FactionMembersPage(
                playerRef,
                factionManager.get(),
                this,
                faction
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] FactionMembersPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open FactionMembersPage: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Opens the Chunk Map page (territory view).
     *
     * @param player    The Player entity
     * @param ref       The entity reference
     * @param store     The entity store
     * @param playerRef The PlayerRef component
     */
    public void openChunkMap(Player player, Ref<EntityStore> ref,
                             Store<EntityStore> store, PlayerRef playerRef) {
        Logger.debug("[GUI] Opening ChunkMapPage for %s", playerRef.getUsername());
        try {
            PageManager pageManager = player.getPageManager();
            ChunkMapPage page = new ChunkMapPage(
                playerRef,
                factionManager.get(),
                claimManager.get(),
                zoneManager.get(),
                this
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] ChunkMapPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open ChunkMapPage: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Opens the Faction Relations page.
     *
     * @param player    The Player entity
     * @param ref       The entity reference
     * @param store     The entity store
     * @param playerRef The PlayerRef component
     * @param faction   The faction to show relations for
     */
    public void openFactionRelations(Player player, Ref<EntityStore> ref,
                                     Store<EntityStore> store, PlayerRef playerRef,
                                     Faction faction) {
        Logger.debug("[GUI] Opening FactionRelationsPage for %s", playerRef.getUsername());
        try {
            PageManager pageManager = player.getPageManager();
            FactionRelationsPage page = new FactionRelationsPage(
                playerRef,
                factionManager.get(),
                relationManager.get(),
                this,
                faction
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] FactionRelationsPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open FactionRelationsPage: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Opens the Create Faction page (multi-step form).
     *
     * @param player    The Player entity
     * @param ref       The entity reference
     * @param store     The entity store
     * @param playerRef The PlayerRef component
     */
    public void openCreateFaction(Player player, Ref<EntityStore> ref,
                                  Store<EntityStore> store, PlayerRef playerRef) {
        Logger.debug("[GUI] Opening CreateFactionPage for %s", playerRef.getUsername());
        try {
            PageManager pageManager = player.getPageManager();
            CreateFactionPage page = new CreateFactionPage(
                playerRef,
                factionManager.get(),
                this
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] CreateFactionPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open CreateFactionPage: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Opens the Faction Browser page (list all factions).
     *
     * @param player    The Player entity
     * @param ref       The entity reference
     * @param store     The entity store
     * @param playerRef The PlayerRef component
     */
    public void openFactionBrowser(Player player, Ref<EntityStore> ref,
                                   Store<EntityStore> store, PlayerRef playerRef) {
        Logger.debug("[GUI] Opening FactionBrowserPage for %s", playerRef.getUsername());
        try {
            PageManager pageManager = player.getPageManager();
            FactionBrowserPage page = new FactionBrowserPage(
                playerRef,
                factionManager.get(),
                powerManager.get(),
                this
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] FactionBrowserPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open FactionBrowserPage: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Opens the Faction Settings page.
     * Requires officer or leader role.
     *
     * @param player    The Player entity
     * @param ref       The entity reference
     * @param store     The entity store
     * @param playerRef The PlayerRef component
     * @param faction   The faction to edit settings for
     */
    public void openFactionSettings(Player player, Ref<EntityStore> ref,
                                    Store<EntityStore> store, PlayerRef playerRef,
                                    Faction faction) {
        Logger.debug("[GUI] Opening FactionSettingsPage for %s", playerRef.getUsername());
        try {
            PageManager pageManager = player.getPageManager();
            FactionSettingsPage page = new FactionSettingsPage(
                playerRef,
                factionManager.get(),
                this,
                faction
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] FactionSettingsPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open FactionSettingsPage: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Opens the Admin Main page.
     * Requires hyperfactions.admin permission.
     *
     * @param player    The Player entity
     * @param ref       The entity reference
     * @param store     The entity store
     * @param playerRef The PlayerRef component
     */
    public void openAdminMain(Player player, Ref<EntityStore> ref,
                              Store<EntityStore> store, PlayerRef playerRef) {
        Logger.debug("[GUI] Opening AdminMainPage for %s", playerRef.getUsername());
        try {
            PageManager pageManager = player.getPageManager();
            AdminMainPage page = new AdminMainPage(
                playerRef,
                factionManager.get(),
                powerManager.get(),
                this
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] AdminMainPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open AdminMainPage: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Opens the Admin Zone page.
     * Requires hyperfactions.admin.zones permission.
     *
     * @param player    The Player entity
     * @param ref       The entity reference
     * @param store     The entity store
     * @param playerRef The PlayerRef component
     */
    public void openAdminZone(Player player, Ref<EntityStore> ref,
                              Store<EntityStore> store, PlayerRef playerRef) {
        Logger.debug("[GUI] Opening AdminZonePage for %s", playerRef.getUsername());
        try {
            PageManager pageManager = player.getPageManager();
            AdminZonePage page = new AdminZonePage(
                playerRef,
                zoneManager.get(),
                this
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] AdminZonePage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open AdminZonePage: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Opens the Logs Viewer page.
     *
     * @param player    The Player entity
     * @param ref       The entity reference
     * @param store     The entity store
     * @param playerRef The PlayerRef component
     * @param faction   The faction to view logs for
     */
    public void openLogsViewer(Player player, Ref<EntityStore> ref,
                               Store<EntityStore> store, PlayerRef playerRef,
                               Faction faction) {
        Logger.debug("[GUI] Opening LogsViewerPage for %s", playerRef.getUsername());
        try {
            PageManager pageManager = player.getPageManager();
            LogsViewerPage page = new LogsViewerPage(
                playerRef,
                factionManager.get(),
                this,
                faction
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] LogsViewerPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open LogsViewerPage: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Opens the Player Info page.
     *
     * @param player          The Player entity
     * @param ref             The entity reference
     * @param store           The entity store
     * @param playerRef       The PlayerRef component
     * @param targetUuid      The UUID of the player to view
     * @param targetName      The name of the player to view
     */
    public void openPlayerInfo(Player player, Ref<EntityStore> ref,
                               Store<EntityStore> store, PlayerRef playerRef,
                               UUID targetUuid, String targetName) {
        Logger.debug("[GUI] Opening PlayerInfoPage for %s (viewing %s)", playerRef.getUsername(), targetName);
        try {
            PageManager pageManager = player.getPageManager();
            PlayerInfoPage page = new PlayerInfoPage(
                playerRef,
                targetUuid,
                targetName,
                factionManager.get(),
                powerManager.get(),
                this
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] PlayerInfoPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open PlayerInfoPage: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Closes the current page.
     *
     * @param player The Player entity
     * @param ref    The entity reference
     * @param store  The entity store
     */
    public void closePage(Player player, Ref<EntityStore> ref, Store<EntityStore> store) {
        player.getPageManager().setPage(ref, store,
                com.hypixel.hytale.protocol.packets.interface_.Page.None);
    }

    // === Getters ===

    public Supplier<HyperFactions> getPlugin() {
        return plugin;
    }

    public Supplier<FactionManager> getFactionManager() {
        return factionManager;
    }

    public Supplier<ClaimManager> getClaimManager() {
        return claimManager;
    }

    public Supplier<PowerManager> getPowerManager() {
        return powerManager;
    }

    public Supplier<RelationManager> getRelationManager() {
        return relationManager;
    }

    public Supplier<ZoneManager> getZoneManager() {
        return zoneManager;
    }

    public Supplier<TeleportManager> getTeleportManager() {
        return teleportManager;
    }

    public Supplier<InviteManager> getInviteManager() {
        return inviteManager;
    }

    public Supplier<Path> getDataDir() {
        return dataDir;
    }
}
