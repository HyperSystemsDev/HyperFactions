package com.hyperfactions.gui;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.gui.faction.*;
import com.hyperfactions.gui.faction.page.*;
import com.hyperfactions.gui.shared.page.*;
import com.hyperfactions.gui.page.admin.*;
import com.hyperfactions.gui.page.newplayer.*;
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

import static com.hyperfactions.gui.faction.FactionPageRegistry.Entry;

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
    private final Supplier<JoinRequestManager> joinRequestManager;
    private final Supplier<Path> dataDir;

    public GuiManager(Supplier<HyperFactions> plugin,
                      Supplier<FactionManager> factionManager,
                      Supplier<ClaimManager> claimManager,
                      Supplier<PowerManager> powerManager,
                      Supplier<RelationManager> relationManager,
                      Supplier<ZoneManager> zoneManager,
                      Supplier<TeleportManager> teleportManager,
                      Supplier<InviteManager> inviteManager,
                      Supplier<JoinRequestManager> joinRequestManager,
                      Supplier<Path> dataDir) {
        this.plugin = plugin;
        this.factionManager = factionManager;
        this.claimManager = claimManager;
        this.powerManager = powerManager;
        this.relationManager = relationManager;
        this.zoneManager = zoneManager;
        this.teleportManager = teleportManager;
        this.inviteManager = inviteManager;
        this.joinRequestManager = joinRequestManager;
        this.dataDir = dataDir;

        // Register all pages with the central registry
        registerPages();
        registerNewPlayerPages();
    }

    /**
     * Registers all GUI pages with the central FactionPageRegistry.
     * This enables navigation between pages via the NavBarHelper.
     */
    private void registerPages() {
        FactionPageRegistry registry = FactionPageRegistry.getInstance();

        // Dashboard (main faction page)
        // If player has faction, show enhanced dashboard; otherwise show main page
        registry.registerEntry(new Entry(
                "dashboard",
                "Dashboard",
                null, // No permission required
                (player, ref, store, playerRef, faction, guiManager) -> {
                    if (faction != null) {
                        return new FactionDashboardPage(playerRef, factionManager.get(), claimManager.get(),
                                powerManager.get(), teleportManager.get(), guiManager, plugin.get(), faction);
                    }
                    return new FactionMainPage(playerRef, factionManager.get(), claimManager.get(),
                            powerManager.get(), teleportManager.get(), inviteManager.get(), guiManager);
                },
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

        // Invites page (officers+ only) - shows outgoing invites and incoming join requests
        registry.registerEntry(new Entry(
                "invites",
                "Invites",
                null,
                (player, ref, store, playerRef, faction, guiManager) -> {
                    if (faction == null) return null;
                    // Only show for officers and above
                    FactionMember member = faction.getMember(playerRef.getUuid());
                    if (member == null || member.role().getLevel() < FactionRole.OFFICER.getLevel()) {
                        return null;  // Returns null = page hidden from nav bar
                    }
                    return new FactionInvitesPage(playerRef, factionManager.get(), inviteManager.get(),
                            joinRequestManager.get(), guiManager, plugin.get(), faction);
                },
                true, // Show in nav bar (conditionally returns null for non-officers)
                true, // Requires faction
                2
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
                3
        ));

        // Map page
        registry.registerEntry(new Entry(
                "map",
                "Map",
                null,
                (player, ref, store, playerRef, faction, guiManager) ->
                        new ChunkMapPage(playerRef, factionManager.get(), claimManager.get(),
                                relationManager.get(), zoneManager.get(), guiManager),
                true,
                false,
                4
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
                5
        ));

        // Settings page (officers+)
        registry.registerEntry(new Entry(
                "settings",
                "Settings",
                null,
                (player, ref, store, playerRef, faction, guiManager) -> {
                    if (faction == null) return null;
                    return new FactionSettingsPage(playerRef, factionManager.get(), claimManager.get(), guiManager, plugin.get(), faction);
                },
                true, // Show in nav bar
                true, // Requires faction
                6
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
     * Registers all New Player GUI pages with the NewPlayerPageRegistry.
     * These pages are shown to players who are NOT in a faction.
     *
     * Nav bar order: BROWSE | CREATE | INVITES | MAP | HELP
     */
    private void registerNewPlayerPages() {
        NewPlayerPageRegistry registry = NewPlayerPageRegistry.getInstance();

        // Browse Factions (default landing page)
        registry.registerEntry(new NewPlayerPageRegistry.Entry(
                "browse",
                "Browse",
                null,
                (player, ref, store, playerRef, guiManager) ->
                        new NewPlayerBrowsePage(playerRef, factionManager.get(), powerManager.get(),
                                inviteManager.get(), guiManager),
                true,
                0
        ));

        // Create Faction
        registry.registerEntry(new NewPlayerPageRegistry.Entry(
                "create",
                "Create",
                null,
                (player, ref, store, playerRef, guiManager) ->
                        new CreateFactionStep1Page(playerRef, factionManager.get(), guiManager),
                true,
                1
        ));

        // My Invites
        registry.registerEntry(new NewPlayerPageRegistry.Entry(
                "invites",
                "Invites",
                null,
                (player, ref, store, playerRef, guiManager) ->
                        new InvitesPage(playerRef, factionManager.get(), powerManager.get(),
                                inviteManager.get(), joinRequestManager.get(), guiManager),
                true,
                2
        ));

        // Territory Map (read-only for new players)
        registry.registerEntry(new NewPlayerPageRegistry.Entry(
                "map",
                "Map",
                null,
                (player, ref, store, playerRef, guiManager) ->
                        new NewPlayerMapPage(playerRef, factionManager.get(), claimManager.get(),
                                relationManager.get(), zoneManager.get(), guiManager),
                true,
                3
        ));

        // Help Page
        registry.registerEntry(new NewPlayerPageRegistry.Entry(
                "help",
                "Help",
                null,
                (player, ref, store, playerRef, guiManager) ->
                        new HelpPage(playerRef, guiManager),
                true,
                4
        ));

        Logger.debug("[GUI] Registered %d pages with NewPlayerPageRegistry", registry.getEntries().size());
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
     * If the player has a faction, opens the enhanced FactionDashboardPage.
     * Otherwise, opens the standard FactionMainPage.
     *
     * @param player    The Player entity
     * @param ref       The entity reference
     * @param store     The entity store
     * @param playerRef The PlayerRef component
     */
    public void openFactionMain(Player player, Ref<EntityStore> ref,
                                Store<EntityStore> store, PlayerRef playerRef) {
        UUID uuid = playerRef.getUuid();
        Faction faction = factionManager.get().getPlayerFaction(uuid);

        if (faction != null) {
            // Player has a faction - open enhanced dashboard
            Logger.debug("[GUI] Opening FactionDashboardPage for %s (faction: %s)",
                    playerRef.getUsername(), faction.name());
            try {
                PageManager pageManager = player.getPageManager();
                FactionDashboardPage page = new FactionDashboardPage(
                    playerRef,
                    factionManager.get(),
                    claimManager.get(),
                    powerManager.get(),
                    teleportManager.get(),
                    this,
                    plugin.get(),
                    faction
                );
                pageManager.openCustomPage(ref, store, page);
                Logger.debug("[GUI] FactionDashboardPage opened successfully");
            } catch (Exception e) {
                Logger.severe("[GUI] Failed to open FactionDashboardPage: %s", e.getMessage());
                e.printStackTrace();
            }
        } else {
            // Player has no faction - redirect to new player browse page
            Logger.debug("[GUI] Player %s has no faction, redirecting to NewPlayerBrowsePage",
                    playerRef.getUsername());
            openNewPlayerBrowse(player, ref, store, playerRef);
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
     * Opens the Chunk Map page (interactive territory view).
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
                relationManager.get(),
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
        // Redirect to the new wizard-style create faction flow
        openCreateFactionWizard(player, ref, store, playerRef);
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
     * Opens the Faction Invites page.
     * Shows outgoing invites and incoming join requests.
     * Requires officer or leader role.
     *
     * @param player    The Player entity
     * @param ref       The entity reference
     * @param store     The entity store
     * @param playerRef The PlayerRef component
     * @param faction   The faction to manage invites for
     */
    public void openFactionInvites(Player player, Ref<EntityStore> ref,
                                   Store<EntityStore> store, PlayerRef playerRef,
                                   Faction faction) {
        Logger.debug("[GUI] Opening FactionInvitesPage for %s", playerRef.getUsername());
        try {
            PageManager pageManager = player.getPageManager();
            FactionInvitesPage page = new FactionInvitesPage(
                playerRef,
                factionManager.get(),
                inviteManager.get(),
                joinRequestManager.get(),
                this,
                plugin.get(),
                faction
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] FactionInvitesPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open FactionInvitesPage: %s", e.getMessage());
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
                claimManager.get(),
                this,
                plugin.get(),
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
     * Opens the Set Relation modal.
     *
     * @param player    The Player entity
     * @param ref       The entity reference
     * @param store     The entity store
     * @param playerRef The PlayerRef component
     * @param faction   The faction to set relations for
     */
    public void openSetRelationModal(Player player, Ref<EntityStore> ref,
                                     Store<EntityStore> store, PlayerRef playerRef,
                                     Faction faction) {
        openSetRelationModal(player, ref, store, playerRef, faction, "", 0);
    }

    /**
     * Opens the Set Relation modal with search state.
     *
     * @param player      The Player entity
     * @param ref         The entity reference
     * @param store       The entity store
     * @param playerRef   The PlayerRef component
     * @param faction     The faction to set relations for
     * @param searchQuery The current search query
     * @param page        The current page
     */
    public void openSetRelationModal(Player player, Ref<EntityStore> ref,
                                     Store<EntityStore> store, PlayerRef playerRef,
                                     Faction faction, String searchQuery, int page) {
        Logger.debug("[GUI] Opening SetRelationModalPage for %s", playerRef.getUsername());
        try {
            PageManager pageManager = player.getPageManager();
            SetRelationModalPage modalPage = new SetRelationModalPage(
                playerRef,
                factionManager.get(),
                powerManager.get(),
                relationManager.get(),
                this,
                faction,
                searchQuery,
                page
            );
            pageManager.openCustomPage(ref, store, modalPage);
            Logger.debug("[GUI] SetRelationModalPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open SetRelationModalPage: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Opens the Tag modal.
     *
     * @param player    The Player entity
     * @param ref       The entity reference
     * @param store     The entity store
     * @param playerRef The PlayerRef component
     * @param faction   The faction to edit tag for
     */
    public void openTagModal(Player player, Ref<EntityStore> ref,
                             Store<EntityStore> store, PlayerRef playerRef,
                             Faction faction) {
        Logger.debug("[GUI] Opening TagModalPage for %s", playerRef.getUsername());
        try {
            PageManager pageManager = player.getPageManager();
            TagModalPage page = new TagModalPage(
                playerRef,
                factionManager.get(),
                this,
                faction
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] TagModalPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open TagModalPage: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Opens the Description modal.
     *
     * @param player    The Player entity
     * @param ref       The entity reference
     * @param store     The entity store
     * @param playerRef The PlayerRef component
     * @param faction   The faction to edit description for
     */
    public void openDescriptionModal(Player player, Ref<EntityStore> ref,
                                     Store<EntityStore> store, PlayerRef playerRef,
                                     Faction faction) {
        Logger.debug("[GUI] Opening DescriptionModalPage for %s", playerRef.getUsername());
        try {
            PageManager pageManager = player.getPageManager();
            DescriptionModalPage page = new DescriptionModalPage(
                playerRef,
                factionManager.get(),
                this,
                faction
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] DescriptionModalPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open DescriptionModalPage: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Opens the Rename Faction modal.
     *
     * @param player    The Player entity
     * @param ref       The entity reference
     * @param store     The entity store
     * @param playerRef The PlayerRef component
     * @param faction   The faction to rename
     */
    public void openRenameModal(Player player, Ref<EntityStore> ref,
                                Store<EntityStore> store, PlayerRef playerRef,
                                Faction faction) {
        Logger.debug("[GUI] Opening RenameModalPage for %s", playerRef.getUsername());
        try {
            PageManager pageManager = player.getPageManager();
            RenameModalPage page = new RenameModalPage(
                playerRef,
                factionManager.get(),
                this,
                faction
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] RenameModalPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open RenameModalPage: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Opens the Recruitment Status modal.
     *
     * @param player    The Player entity
     * @param ref       The entity reference
     * @param store     The entity store
     * @param playerRef The PlayerRef component
     * @param faction   The faction to edit recruitment for
     */
    public void openRecruitmentModal(Player player, Ref<EntityStore> ref,
                                     Store<EntityStore> store, PlayerRef playerRef,
                                     Faction faction) {
        Logger.debug("[GUI] Opening RecruitmentModalPage for %s", playerRef.getUsername());
        try {
            PageManager pageManager = player.getPageManager();
            RecruitmentModalPage page = new RecruitmentModalPage(
                playerRef,
                factionManager.get(),
                this,
                faction
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] RecruitmentModalPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open RecruitmentModalPage: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Opens the Color Picker page.
     *
     * @param player    The Player entity
     * @param ref       The entity reference
     * @param store     The entity store
     * @param playerRef The PlayerRef component
     * @param faction   The faction to edit color for
     */
    public void openColorPicker(Player player, Ref<EntityStore> ref,
                                Store<EntityStore> store, PlayerRef playerRef,
                                Faction faction) {
        Logger.debug("[GUI] Opening ColorPickerPage for %s", playerRef.getUsername());
        try {
            PageManager pageManager = player.getPageManager();
            ColorPickerPage page = new ColorPickerPage(
                playerRef,
                factionManager.get(),
                this,
                faction
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] ColorPickerPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open ColorPickerPage: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Opens the Disband Confirmation modal.
     *
     * @param player    The Player entity
     * @param ref       The entity reference
     * @param store     The entity store
     * @param playerRef The PlayerRef component
     * @param faction   The faction to disband
     */
    public void openDisbandConfirm(Player player, Ref<EntityStore> ref,
                                   Store<EntityStore> store, PlayerRef playerRef,
                                   Faction faction) {
        Logger.debug("[GUI] Opening DisbandConfirmPage for %s", playerRef.getUsername());
        try {
            PageManager pageManager = player.getPageManager();
            DisbandConfirmPage page = new DisbandConfirmPage(
                playerRef,
                factionManager.get(),
                this,
                faction
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] DisbandConfirmPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open DisbandConfirmPage: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Opens the Leadership Transfer Confirmation modal.
     * Shows a warning and requires confirmation to transfer leadership.
     *
     * @param player     The Player entity
     * @param ref        The entity reference
     * @param store      The entity store
     * @param playerRef  The PlayerRef component
     * @param faction    The faction
     * @param targetUuid The UUID of the player receiving leadership
     * @param targetName The name of the player receiving leadership
     */
    public void openTransferConfirm(Player player, Ref<EntityStore> ref,
                                    Store<EntityStore> store, PlayerRef playerRef,
                                    Faction faction, UUID targetUuid, String targetName) {
        Logger.debug("[GUI] Opening TransferConfirmPage for %s -> %s", playerRef.getUsername(), targetName);
        try {
            PageManager pageManager = player.getPageManager();
            TransferConfirmPage page = new TransferConfirmPage(
                playerRef,
                factionManager.get(),
                this,
                faction,
                targetUuid,
                targetName
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] TransferConfirmPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open TransferConfirmPage: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Opens the Faction Dashboard page.
     * Shows stat cards, quick actions, and activity feed for faction members.
     *
     * @param player    The Player entity
     * @param ref       The entity reference
     * @param store     The entity store
     * @param playerRef The PlayerRef component
     * @param faction   The faction to show dashboard for
     */
    public void openFactionDashboard(Player player, Ref<EntityStore> ref,
                                     Store<EntityStore> store, PlayerRef playerRef,
                                     Faction faction) {
        Logger.debug("[GUI] Opening FactionDashboardPage for %s", playerRef.getUsername());
        try {
            PageManager pageManager = player.getPageManager();
            FactionDashboardPage page = new FactionDashboardPage(
                playerRef,
                factionManager.get(),
                claimManager.get(),
                powerManager.get(),
                teleportManager.get(),
                this,
                plugin.get(),
                faction
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] FactionDashboardPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open FactionDashboardPage: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Opens the Leave Faction Confirmation modal.
     *
     * @param player    The Player entity
     * @param ref       The entity reference
     * @param store     The entity store
     * @param playerRef The PlayerRef component
     * @param faction   The faction to leave
     */
    public void openLeaveConfirm(Player player, Ref<EntityStore> ref,
                                 Store<EntityStore> store, PlayerRef playerRef,
                                 Faction faction) {
        Logger.debug("[GUI] Opening LeaveConfirmPage for %s", playerRef.getUsername());
        try {
            PageManager pageManager = player.getPageManager();
            LeaveConfirmPage page = new LeaveConfirmPage(
                playerRef,
                factionManager.get(),
                this,
                faction
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] LeaveConfirmPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open LeaveConfirmPage: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Opens the Faction Modules page.
     *
     * @param player    The Player entity
     * @param ref       The entity reference
     * @param store     The entity store
     * @param playerRef The PlayerRef component
     * @param faction   The faction to show modules for
     */
    public void openFactionModules(Player player, Ref<EntityStore> ref,
                                   Store<EntityStore> store, PlayerRef playerRef,
                                   Faction faction) {
        Logger.debug("[GUI] Opening FactionModulesPage for %s", playerRef.getUsername());
        try {
            PageManager pageManager = player.getPageManager();
            FactionModulesPage page = new FactionModulesPage(
                playerRef,
                factionManager.get(),
                this,
                faction
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] FactionModulesPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open FactionModulesPage: %s", e.getMessage());
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
     * Opens the Faction Info page.
     * Displays detailed information about a specific faction.
     *
     * @param player        The Player entity
     * @param ref           The entity reference
     * @param store         The entity store
     * @param playerRef     The PlayerRef component
     * @param targetFaction The faction to view info for
     */
    public void openFactionInfo(Player player, Ref<EntityStore> ref,
                                Store<EntityStore> store, PlayerRef playerRef,
                                Faction targetFaction) {
        Logger.debug("[GUI] Opening FactionInfoPage for %s (viewing %s)",
                playerRef.getUsername(), targetFaction.name());
        try {
            PageManager pageManager = player.getPageManager();
            FactionInfoPage page = new FactionInfoPage(
                playerRef,
                targetFaction,
                factionManager.get(),
                powerManager.get(),
                relationManager.get(),
                this
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] FactionInfoPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open FactionInfoPage: %s", e.getMessage());
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

    // =========================================================================
    // NEW PLAYER GUI METHODS
    // =========================================================================

    /**
     * Opens the New Player Browse page (default landing page for players without a faction).
     *
     * @param player    The Player entity
     * @param ref       The entity reference
     * @param store     The entity store
     * @param playerRef The PlayerRef component
     */
    public void openNewPlayerBrowse(Player player, Ref<EntityStore> ref,
                                    Store<EntityStore> store, PlayerRef playerRef) {
        openNewPlayerBrowse(player, ref, store, playerRef, 0, "power");
    }

    /**
     * Opens the New Player Browse page with custom page and sort state.
     *
     * @param player    The Player entity
     * @param ref       The entity reference
     * @param store     The entity store
     * @param playerRef The PlayerRef component
     * @param page      The page number (0-indexed)
     * @param sortBy    The sort mode (power, members, name)
     */
    public void openNewPlayerBrowse(Player player, Ref<EntityStore> ref,
                                    Store<EntityStore> store, PlayerRef playerRef,
                                    int page, String sortBy) {
        Logger.debug("[GUI] Opening NewPlayerBrowsePage for %s (page=%d, sort=%s)",
                playerRef.getUsername(), page, sortBy);
        try {
            PageManager pageManager = player.getPageManager();
            NewPlayerBrowsePage browsePage = new NewPlayerBrowsePage(
                playerRef,
                factionManager.get(),
                powerManager.get(),
                inviteManager.get(),
                this,
                page,
                sortBy
            );
            pageManager.openCustomPage(ref, store, browsePage);
            Logger.debug("[GUI] NewPlayerBrowsePage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open NewPlayerBrowsePage: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Opens the Create Faction wizard (Step 1).
     *
     * @param player    The Player entity
     * @param ref       The entity reference
     * @param store     The entity store
     * @param playerRef The PlayerRef component
     */
    public void openCreateFactionWizard(Player player, Ref<EntityStore> ref,
                                        Store<EntityStore> store, PlayerRef playerRef) {
        Logger.debug("[GUI] Opening CreateFactionStep1Page for %s", playerRef.getUsername());
        try {
            PageManager pageManager = player.getPageManager();
            CreateFactionStep1Page page = new CreateFactionStep1Page(
                playerRef,
                factionManager.get(),
                this
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] CreateFactionStep1Page opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open CreateFactionStep1Page: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Opens the Create Faction wizard Step 1 with preserved state.
     * Used when rebuilding the page after color selection.
     *
     * @param player        The Player entity
     * @param ref           The entity reference
     * @param store         The entity store
     * @param playerRef     The PlayerRef component
     * @param selectedColor The currently selected color code
     * @param name          The preserved faction name input
     * @param tag           The preserved faction tag input
     */
    public void openCreateFactionStep1(Player player, Ref<EntityStore> ref,
                                       Store<EntityStore> store, PlayerRef playerRef,
                                       String selectedColor, String name, String tag) {
        Logger.debug("[GUI] Opening CreateFactionStep1Page for %s (color=%s)",
                playerRef.getUsername(), selectedColor);
        try {
            PageManager pageManager = player.getPageManager();
            CreateFactionStep1Page page = new CreateFactionStep1Page(
                playerRef,
                factionManager.get(),
                this,
                selectedColor,
                name,
                tag
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] CreateFactionStep1Page opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open CreateFactionStep1Page: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Opens the Create Faction wizard Step 2.
     *
     * @param player      The Player entity
     * @param ref         The entity reference
     * @param store       The entity store
     * @param playerRef   The PlayerRef component
     * @param name        The faction name from Step 1
     * @param color       The faction color from Step 1
     * @param tag         The faction tag from Step 1 (may be null)
     */
    public void openCreateFactionStep2(Player player, Ref<EntityStore> ref,
                                       Store<EntityStore> store, PlayerRef playerRef,
                                       String name, String color, String tag) {
        openCreateFactionStep2(player, ref, store, playerRef, name, color, tag, false);
    }

    /**
     * Opens the Create Faction wizard Step 2 with preserved state.
     * Used when rebuilding the page after recruitment selection.
     *
     * @param player          The Player entity
     * @param ref             The entity reference
     * @param store           The entity store
     * @param playerRef       The PlayerRef component
     * @param name            The faction name from Step 1
     * @param color           The faction color from Step 1
     * @param tag             The faction tag from Step 1 (may be null)
     * @param openRecruitment Whether recruitment is open (true) or invite-only (false)
     */
    public void openCreateFactionStep2(Player player, Ref<EntityStore> ref,
                                       Store<EntityStore> store, PlayerRef playerRef,
                                       String name, String color, String tag,
                                       boolean openRecruitment) {
        Logger.debug("[GUI] Opening CreateFactionStep2Page for %s (recruitment=%s)",
                playerRef.getUsername(), openRecruitment ? "open" : "closed");
        try {
            PageManager pageManager = player.getPageManager();
            CreateFactionStep2Page page = new CreateFactionStep2Page(
                playerRef,
                factionManager.get(),
                this,
                name,
                color,
                tag,
                openRecruitment
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] CreateFactionStep2Page opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open CreateFactionStep2Page: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Opens the Invites page for new players.
     *
     * @param player    The Player entity
     * @param ref       The entity reference
     * @param store     The entity store
     * @param playerRef The PlayerRef component
     */
    public void openInvitesPage(Player player, Ref<EntityStore> ref,
                                Store<EntityStore> store, PlayerRef playerRef) {
        Logger.debug("[GUI] Opening InvitesPage for %s", playerRef.getUsername());
        try {
            PageManager pageManager = player.getPageManager();
            InvitesPage page = new InvitesPage(
                playerRef,
                factionManager.get(),
                powerManager.get(),
                inviteManager.get(),
                joinRequestManager.get(),
                this
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] InvitesPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open InvitesPage: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Opens the read-only Map page for new players.
     *
     * @param player    The Player entity
     * @param ref       The entity reference
     * @param store     The entity store
     * @param playerRef The PlayerRef component
     */
    public void openNewPlayerMap(Player player, Ref<EntityStore> ref,
                                 Store<EntityStore> store, PlayerRef playerRef) {
        Logger.debug("[GUI] Opening NewPlayerMapPage for %s", playerRef.getUsername());
        try {
            PageManager pageManager = player.getPageManager();
            NewPlayerMapPage page = new NewPlayerMapPage(
                playerRef,
                factionManager.get(),
                claimManager.get(),
                relationManager.get(),
                zoneManager.get(),
                this
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] NewPlayerMapPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open NewPlayerMapPage: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Opens the Help page for new players.
     *
     * @param player    The Player entity
     * @param ref       The entity reference
     * @param store     The entity store
     * @param playerRef The PlayerRef component
     */
    public void openHelpPage(Player player, Ref<EntityStore> ref,
                             Store<EntityStore> store, PlayerRef playerRef) {
        Logger.debug("[GUI] Opening HelpPage for %s", playerRef.getUsername());
        try {
            PageManager pageManager = player.getPageManager();
            HelpPage page = new HelpPage(playerRef, this);
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] HelpPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open HelpPage: %s", e.getMessage());
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

    public JoinRequestManager getJoinRequestManager() {
        return joinRequestManager.get();
    }

    public Supplier<Path> getDataDir() {
        return dataDir;
    }
}
