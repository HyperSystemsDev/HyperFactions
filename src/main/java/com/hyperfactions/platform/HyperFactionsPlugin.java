package com.hyperfactions.platform;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.api.HyperFactionsAPI;
import com.hyperfactions.chat.PublicChatListener;
import com.hyperfactions.command.FactionCommand;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.listener.PlayerListener;
import com.hyperfactions.protection.ProtectionListener;
import com.hyperfactions.protection.damage.DamageProtectionHandler;
import com.hyperfactions.protection.ecs.BlockPlaceProtectionSystem;
import com.hyperfactions.protection.interactions.HyperFactionsPlaceFluidInteraction;
import com.hyperfactions.protection.interactions.HyperFactionsRefillContainerInteraction;
import com.hyperfactions.protection.ecs.BlockBreakProtectionSystem;
import com.hyperfactions.protection.ecs.BlockUseProtectionSystem;
import com.hyperfactions.protection.ecs.ItemPickupProtectionSystem;
import com.hyperfactions.protection.ecs.ItemDropProtectionSystem;
import com.hyperfactions.protection.ecs.PlayerDeathSystem;
import com.hyperfactions.protection.ecs.PlayerRespawnSystem;
import com.hyperfactions.protection.ecs.PvPProtectionSystem;
import com.hyperfactions.protection.ecs.HarvestPickupProtectionSystem;
import com.hyperfactions.territory.TerritoryTickingSystem;
import com.hyperfactions.util.Logger;
import com.hyperfactions.worldmap.HyperFactionsWorldMapProvider;
import com.hyperfactions.integration.PermissionRegistrar;
import com.hyperfactions.integration.orbis.OrbisMixinsIntegration;
import com.hyperfactions.integration.orbis.OrbisGuardIntegration;
import com.hypixel.hytale.component.system.ISystem;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.server.core.event.events.BootEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.world.events.AddWorldEvent;
import com.hypixel.hytale.server.core.universe.world.events.RemoveWorldEvent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.worldmap.provider.IWorldMapProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * Main Hytale plugin class for HyperFactions.
 */
public class HyperFactionsPlugin extends JavaPlugin {

    private static HyperFactionsPlugin instance;

    /**
     * Gets the plugin instance.
     */
    public static HyperFactionsPlugin getInstance() {
        return instance;
    }

    private HyperFactions hyperFactions;
    private PlayerListener playerListener;
    private ProtectionListener protectionListener;
    private PublicChatListener publicChatListener;
    private TerritoryTickingSystem territoryTickingSystem;

    // Task scheduling
    private final AtomicInteger taskIdCounter = new AtomicInteger(0);
    private final Map<Integer, Object> scheduledTasks = new ConcurrentHashMap<>();

    // Player tracking
    private final Map<UUID, PlayerRef> trackedPlayers = new ConcurrentHashMap<>();

    // Periodic task executor
    private ScheduledExecutorService tickExecutor;
    private ScheduledFuture<?> powerRegenTask;
    private ScheduledFuture<?> combatTagTask;
    private ScheduledFuture<?> claimDecayTask;

    /**
     * Creates a new HyperFactionsPlugin instance.
     *
     * @param init the plugin initialization data
     */
    public HyperFactionsPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        instance = this;

        // Register interaction codec replacements (must be in setup, before assets load)
        registerInteractionCodecs();

        // Initialize HyperFactions core
        hyperFactions = new HyperFactions(getDataDirectory(), java.util.logging.Logger.getLogger("HyperFactions"));

        // Set API instance
        HyperFactionsAPI.setInstance(hyperFactions);

        getLogger().at(Level.INFO).log("HyperFactions setup complete");
    }

    @Override
    protected void start() {
        // Configure platform callbacks
        configurePlatformCallbacks();

        // Enable core
        hyperFactions.enable();

        // Initialize GravestonePlugin integration (v2 direct API — needs EventRegistry)
        hyperFactions.initGravestoneIntegration(getEventRegistry());

        // Register world map provider CODEC (must be before worlds load)
        registerWorldMapProvider();

        // Register commands
        registerCommands();

        // Register event listeners
        registerEventListeners();

        // Register teleport systems
        registerTeleportSystems();

        // Register territory tracking systems
        registerTerritorySystems();

        // Apply world map provider to already-loaded worlds
        // (AddWorldEvent may have fired before our listener was registered)
        applyWorldMapProviderToExistingWorlds();

        // Start periodic tasks
        startPeriodicTasks();

        // Initialize spawn suppression manager (must be after TagSetPlugin is ready)
        initializeSpawnSuppression();

        // Initialize OrbisGuard integrations and register pickup hooks
        initializeOrbisIntegrations();

        // Log protection coverage summary
        logProtectionCoverage();

        // Register permission nodes with LuckPerms on BootEvent (after all plugins loaded)
        getEventRegistry().registerGlobal(BootEvent.class, e -> PermissionRegistrar.registerWithLuckPerms());

        getLogger().at(Level.INFO).log("HyperFactions v%s enabled!", getManifest().getVersion());
    }

    @Override
    protected void shutdown() {
        // Stop periodic tasks
        stopPeriodicTasks();

        // Handle combat logout for all tagged players
        for (UUID playerUuid : trackedPlayers.keySet()) {
            hyperFactions.getCombatTagManager().handleDisconnect(playerUuid);
        }

        // Unregister all OrbisGuard-Mixins hooks
        OrbisMixinsIntegration.unregisterAllHooks();

        // Clean up territory ticking system
        if (territoryTickingSystem != null) {
            territoryTickingSystem.shutdown();
            territoryTickingSystem = null;
        }

        // Clear instances
        instance = null;
        HyperFactionsAPI.setInstance(null);

        // Disable core
        if (hyperFactions != null) {
            hyperFactions.disable();
        }

        // Clear tracked players
        trackedPlayers.clear();

        getLogger().at(Level.INFO).log("HyperFactions disabled");
    }

    /**
     * Configures platform-specific callbacks for HyperFactions core.
     */
    private void configurePlatformCallbacks() {
        // Async executor
        hyperFactions.setAsyncExecutor(task -> {
            java.util.concurrent.CompletableFuture.runAsync(task);
        });

        // Task scheduler (for one-shot delayed tasks)
        hyperFactions.setTaskScheduler((delayTicks, task) -> {
            int id = taskIdCounter.incrementAndGet();
            java.util.Timer timer = new java.util.Timer();
            long delayMs = delayTicks * 50L;
            timer.schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    scheduledTasks.remove(id);
                    task.run();
                }
            }, delayMs);
            scheduledTasks.put(id, timer);
            return id;
        });

        // Repeating task scheduler (for periodic tasks like auto-save)
        hyperFactions.setRepeatingTaskScheduler((delayTicks, periodTicks, task) -> {
            int id = taskIdCounter.incrementAndGet();
            java.util.Timer timer = new java.util.Timer();
            long delayMs = delayTicks * 50L;
            long periodMs = periodTicks * 50L;
            timer.scheduleAtFixedRate(new java.util.TimerTask() {
                @Override
                public void run() {
                    task.run();
                }
            }, delayMs, periodMs);
            scheduledTasks.put(id, timer);
            return id;
        });

        // Task canceller
        hyperFactions.setTaskCanceller(taskId -> {
            Object task = scheduledTasks.remove(taskId);
            if (task instanceof java.util.Timer timer) {
                timer.cancel();
            }
        });

        // Player lookup (for chat manager)
        hyperFactions.setPlayerLookup(this::getTrackedPlayer);

        // Online players supplier (for announcements)
        hyperFactions.setOnlinePlayersSupplier(() -> trackedPlayers.values());
    }

    /**
     * Registers commands with Hytale.
     */
    private void registerCommands() {
        try {
            getCommandRegistry().registerCommand(new FactionCommand(hyperFactions, this));
            getLogger().at(Level.INFO).log("Registered command: /faction (/f, /hf)");
        } catch (Exception e) {
            getLogger().at(Level.SEVERE).withCause(e).log("Failed to register commands");
        }
    }

    /**
     * Registers the world map provider CODEC with Hytale.
     * This allows our custom world map generator to be used.
     */
    private void registerWorldMapProvider() {
        try {
            IWorldMapProvider.CODEC.register(
                    HyperFactionsWorldMapProvider.ID,
                    HyperFactionsWorldMapProvider.class,
                    HyperFactionsWorldMapProvider.CODEC
            );
            getLogger().at(Level.INFO).log("Registered HyperFactions world map provider (ID: %s)",
                    HyperFactionsWorldMapProvider.ID);
        } catch (Exception e) {
            getLogger().at(Level.WARNING).withCause(e).log("Failed to register world map provider");
        }
    }

    /**
     * Registers custom interaction codec replacements for protection.
     * Replaces PlaceFluid with a version that checks zone/faction protection.
     * Must be called in setup() before assets are loaded.
     */
    private void registerInteractionCodecs() {
        try {
            var registry = this.getCodecRegistry(Interaction.CODEC);
            registry.register("PlaceFluid",
                    HyperFactionsPlaceFluidInteraction.class,
                    HyperFactionsPlaceFluidInteraction.CODEC);
            registry.register("RefillContainer",
                    HyperFactionsRefillContainerInteraction.class,
                    HyperFactionsRefillContainerInteraction.CODEC);
            getLogger().at(Level.INFO).log("Registered fluid protection codecs (place + pickup)");
        } catch (Exception e) {
            getLogger().at(Level.WARNING).log("Failed to register fluid placement codec: %s", e.getMessage());
        }
    }

    /**
     * Initializes the spawn suppression manager and applies suppression to existing worlds.
     */
    private void initializeSpawnSuppression() {
        try {
            // Initialize the manager (resolves NPC groups)
            hyperFactions.getSpawnSuppressionManager().initialize();

            // Wire up zone change callback to re-apply suppression
            hyperFactions.getZoneManager().setOnZoneChangeCallback(affectedChunks -> {
                // Refresh world maps (respects configured refresh mode)
                // Pass affected chunks for optimized refresh, or null for full refresh
                hyperFactions.getWorldMapService().triggerFactionWideRefresh(affectedChunks);
                // Re-apply spawn suppression to all worlds
                applySpawnSuppressionToAllWorlds();
            });

            // Apply to existing worlds - some may not be ready yet during startup
            List<String> failedWorlds = applySpawnSuppressionToAllWorlds();

            if (!failedWorlds.isEmpty()) {
                // Schedule a retry for worlds that weren't ready
                Logger.debug("Scheduling spawn suppression retry for %d worlds: %s",
                    failedWorlds.size(), String.join(", ", failedWorlds));

                hyperFactions.scheduleDelayedTask(60, () -> { // Retry after 60 ticks (3 seconds)
                    Logger.debug("Retrying spawn suppression for worlds that weren't ready at startup");
                    List<String> stillFailed = applySpawnSuppressionToAllWorlds();
                    if (!stillFailed.isEmpty()) {
                        Logger.warn("Some worlds still not ready for spawn suppression: %s (will be handled by AddWorldEvent)",
                            String.join(", ", stillFailed));
                    }
                });
            }

            getLogger().at(Level.INFO).log("Spawn suppression initialized");
        } catch (Exception e) {
            getLogger().at(Level.WARNING).withCause(e).log("Failed to initialize spawn suppression");
        }
    }

    /**
     * Applies spawn suppression to all loaded worlds.
     * Returns a list of world names that weren't ready and should be retried.
     */
    private List<String> applySpawnSuppressionToAllWorlds() {
        List<String> failedWorlds = new ArrayList<>();
        try {
            Map<String, World> worlds = Universe.get().getWorlds();
            for (World world : worlds.values()) {
                boolean success = hyperFactions.getSpawnSuppressionManager().applyToWorld(world);
                if (!success) {
                    failedWorlds.add(world.getName());
                }
            }
        } catch (Exception e) {
            Logger.warn("Failed to apply spawn suppression to worlds: %s", e.getMessage());
        }
        return failedWorlds;
    }

    /**
     * Applies the world map provider to any worlds that were loaded before
     * our AddWorldEvent listener was registered.
     */
    private void applyWorldMapProviderToExistingWorlds() {
        if (!ConfigManager.get().isWorldMapMarkersEnabled()) {
            Logger.debug("World map markers disabled, skipping provider setup for existing worlds");
            return;
        }

        try {
            Map<String, World> worlds = Universe.get().getWorlds();
            Logger.debug("Checking %d existing worlds for world map provider setup", worlds.size());

            for (World world : worlds.values()) {
                try {
                    // Skip temporary worlds
                    if (world.getWorldConfig().isDeleteOnRemove()) {
                        Logger.debug("Skipping temporary world: %s", world.getName());
                        continue;
                    }

                    // Set our world map generator directly on the WorldMapManager
                    // This is critical - setWorldMapProvider() only affects future loads,
                    // but setGenerator() updates the live WorldMapManager
                    world.getWorldMapManager().setGenerator(
                            com.hyperfactions.worldmap.HyperFactionsWorldMap.INSTANCE);
                    Logger.debug("Applied HyperFactions world map generator to existing world: %s", world.getName());

                    // Also register with WorldMapService to track it
                    hyperFactions.getWorldMapService().registerProviderIfNeeded(world);

                } catch (Exception e) {
                    Logger.warn("Failed to apply world map provider to world %s: %s",
                            world.getName(), e.getMessage());
                }
            }
        } catch (Exception e) {
            Logger.warn("Failed to apply world map provider to existing worlds: %s", e.getMessage());
        }
    }

    /**
     * Initializes OrbisGuard integrations and registers pickup protection hooks.
     *
     * OrbisGuard-Mixins provides hooks for F-key and auto item pickup protection.
     * OrbisGuard provides region protection to prevent claiming in protected areas.
     */
    private void initializeOrbisIntegrations() {
        // Initialize OrbisGuard API detection (for claim conflict checking)
        OrbisGuardIntegration.init();

        // Register mixin hooks unconditionally - the mixin will find them if loaded
        // This matches how OrbisGuard registers its hooks
        OrbisMixinsIntegration.registerPickupHook(
                (playerUuid, worldName, x, y, z, mode) -> {
                    // Delegate to ProtectionChecker with mode awareness
                    // mode = "manual" for F-key pickup, "auto" for walking over items
                    return hyperFactions.getProtectionChecker().canPickupItem(
                            playerUuid, worldName, x, y, z, mode);
                });

        // Register harvest/F-key pickup protection hook
        // This handles F-key pickup on rubble, crops, etc. via BlockHarvestUtils
        OrbisMixinsIntegration.registerHarvestHook(
                (playerUuid, worldName, x, y, z) -> {
                    // Check ITEM_PICKUP_MANUAL flag for F-key pickup protection
                    boolean allowed = hyperFactions.getProtectionChecker().canPickupItem(
                            playerUuid, worldName, x, 0, z, "manual");
                    if (!allowed) {
                        return "You cannot pick up items manually in this zone.";
                    }
                    return null; // Allowed
                });

        // Register spawn protection hook
        OrbisMixinsIntegration.registerSpawnHook(
                (worldName, x, y, z) -> {
                    // Delegate to ProtectionChecker - returns true if spawn should be BLOCKED
                    return hyperFactions.getProtectionChecker().shouldBlockSpawn(worldName, x, y, z);
                });

        // Note: Fluid placement protection is handled via interaction codec replacement
        // (HyperFactionsPlaceFluidInteraction), not via mixin hooks.
    }

    /**
     * Logs a summary of protection coverage at startup.
     * Informs admins which protections are active and which require additional plugins.
     */
    private void logProtectionCoverage() {
        boolean orbisGuardAvailable = OrbisGuardIntegration.isAvailable();

        getLogger().at(Level.INFO).log("=== HyperFactions Protection Coverage ===");
        getLogger().at(Level.INFO).log("ECS Events (native): Block break/place, Use, Harvest drops, Damage - ENABLED");
        getLogger().at(Level.INFO).log("Interaction Codecs: Fluid place/pickup protection - ENABLED");
        getLogger().at(Level.INFO).log("Mixin Hooks (registered): F-key pickup, Auto pickup, NPC Spawn control");
        getLogger().at(Level.INFO).log("  -> Requires Hyxin + OrbisGuard-Mixins in earlyplugins/ to activate");

        if (orbisGuardAvailable) {
            getLogger().at(Level.INFO).log("OrbisGuard API: Claim conflict detection - ENABLED");
        } else {
            getLogger().at(Level.INFO).log("OrbisGuard: Not detected (optional)");
        }

        boolean gsAvailable = hyperFactions.getProtectionChecker().getGravestoneIntegration() != null
                && hyperFactions.getProtectionChecker().getGravestoneIntegration().isAvailable();
        if (gsAvailable) {
            getLogger().at(Level.INFO).log("GravestonePlugin: v2 API — AccessChecker + events - ENABLED");
        } else {
            getLogger().at(Level.INFO).log("GravestonePlugin: Not detected (optional)");
        }

        getLogger().at(Level.INFO).log("=========================================");
    }

    /**
     * Registers event listeners with Hytale.
     */
    private void registerEventListeners() {
        // World add event - register world map provider for each world
        getEventRegistry().registerGlobal(AddWorldEvent.class, this::onWorldAdd);

        // World remove event - cleanup
        getEventRegistry().registerGlobal(RemoveWorldEvent.class, this::onWorldRemove);

        // Player connect event
        getEventRegistry().register(PlayerConnectEvent.class, this::onPlayerConnect);

        // Player disconnect event
        getEventRegistry().register(PlayerDisconnectEvent.class, this::onPlayerDisconnect);

        // Player chat event (for faction/ally chat channels)
        // Use async global handler since PlayerChatEvent is an async event
        getEventRegistry().registerAsyncGlobal(
            EventPriority.NORMAL,
            PlayerChatEvent.class,
            this::onPlayerChatAsync
        );

        // Public chat formatting (faction tags with relation colors)
        // Register at configured priority (default LATE) to run after LuckPerms
        publicChatListener = new PublicChatListener(hyperFactions);
        EventPriority chatPriority = publicChatListener.getEventPriority();
        getEventRegistry().registerAsyncGlobal(
            chatPriority,
            PlayerChatEvent.class,
            publicChatListener::onPlayerChatAsync
        );
        getLogger().at(Level.INFO).log("Registered public chat formatter at %s priority", chatPriority);

        // Create listeners
        playerListener = new PlayerListener(hyperFactions);
        protectionListener = new ProtectionListener(hyperFactions);

        // Create and set damage protection handler (coordinates all damage protection systems)
        DamageProtectionHandler damageHandler = new DamageProtectionHandler(
            hyperFactions.getZoneDamageProtection(),
            hyperFactions.getProtectionChecker(),
            hyperFactions.getCombatTagManager(),
            protectionListener::getDenialMessage
        );
        hyperFactions.setDamageProtectionHandler(damageHandler);

        // Register ECS event systems for block protection
        registerBlockProtectionSystems();

        // Register harvest pickup protection (InteractivelyPickupItemEvent - for block harvest drops)
        // Note: F-key entity pickup (PickupItemInteraction) cannot be intercepted - server API limitation
        registerHarvestPickupProtection();

        // Register update notification listener (if update checking is enabled)
        if (hyperFactions.getUpdateNotificationListener() != null) {
            hyperFactions.getUpdateNotificationListener().register(getEventRegistry());
            getLogger().at(Level.INFO).log("Registered update notification listener");
        }

        getLogger().at(Level.INFO).log("Registered event listeners");
    }

    /**
     * Registers ECS event systems for block protection.
     */
    private void registerBlockProtectionSystems() {
        try {
            // Block place protection
            getEntityStoreRegistry().registerSystem(new BlockPlaceProtectionSystem(hyperFactions, protectionListener));

            // Block break protection
            getEntityStoreRegistry().registerSystem(new BlockBreakProtectionSystem(hyperFactions, protectionListener));

            // Block use/interact protection
            getEntityStoreRegistry().registerSystem(new BlockUseProtectionSystem(hyperFactions, protectionListener));

            // Item pickup protection
            getEntityStoreRegistry().registerSystem(new ItemPickupProtectionSystem(hyperFactions, protectionListener));

            // Item drop protection
            getEntityStoreRegistry().registerSystem(new ItemDropProtectionSystem(hyperFactions));

            // PvP protection
            getEntityStoreRegistry().registerSystem(new PvPProtectionSystem(hyperFactions, protectionListener));

            // Player death and respawn systems (power loss, spawn protection)
            getEntityStoreRegistry().registerSystem(new PlayerDeathSystem(hyperFactions));
            getEntityStoreRegistry().registerSystem(new PlayerRespawnSystem(hyperFactions));

            getLogger().at(Level.INFO).log("Registered block, item, and player protection systems");
        } catch (Exception e) {
            getLogger().at(Level.WARNING).withCause(e).log("Failed to register block protection systems");
        }
    }

    /**
     * Registers the harvest pickup protection system.
     * This handles block harvest drops (farming, mining, etc.) via InteractivelyPickupItemEvent.
     *
     * NOTE: F-key entity pickup of items already on the ground (PickupItemInteraction)
     * cannot be intercepted as it does not fire any cancellable event. This is a
     * limitation of the Hytale server API.
     */
    private void registerHarvestPickupProtection() {
        try {
            getEntityStoreRegistry().registerSystem(new HarvestPickupProtectionSystem(hyperFactions, protectionListener));
            getLogger().at(Level.INFO).log("Registered harvest pickup protection system");
        } catch (Exception e) {
            getLogger().at(Level.WARNING).withCause(e).log("Failed to register harvest pickup protection system");
        }
    }

    /**
     * Registers ECS event systems for teleport management.
     */
    private void registerTeleportSystems() {
        try {
            // Cancel teleport on damage
            getEntityStoreRegistry().registerSystem(new TeleportCancelOnDamageSystem(hyperFactions, this));

            getLogger().at(Level.INFO).log("Registered teleport systems");
        } catch (Exception e) {
            getLogger().at(Level.WARNING).withCause(e).log("Failed to register teleport systems");
        }
    }

    /**
     * Registers ECS ticking systems for territory tracking.
     * The TerritoryTickingSystem reliably detects player chunk changes
     * each game tick, triggering territory notifications.
     */
    private void registerTerritorySystems() {
        try {
            // Territory chunk tracking (ticks every game tick for players)
            // Cast to ISystem as required by Hytale's registration
            territoryTickingSystem = new TerritoryTickingSystem(hyperFactions);
            getEntityStoreRegistry().registerSystem((ISystem) territoryTickingSystem);

            getLogger().at(Level.INFO).log("Registered territory ticking system");
        } catch (Exception e) {
            getLogger().at(Level.WARNING).withCause(e).log("Failed to register territory ticking system");
        }
    }

    /**
     * Handles world add event - registers world map provider.
     */
    private void onWorldAdd(AddWorldEvent event) {
        World world = event.getWorld();
        Logger.debug("AddWorldEvent received for world: %s", world.getName());
        try {
            // Skip temporary worlds
            if (world.getWorldConfig().isDeleteOnRemove()) {
                Logger.debug("Skipping world %s (temporary/delete-on-remove)", world.getName());
                return;
            }

            // Register our world map provider for this world
            boolean worldMapEnabled = ConfigManager.get().isWorldMapMarkersEnabled();
            Logger.debug("World map markers enabled: %s for world: %s", worldMapEnabled, world.getName());

            if (worldMapEnabled) {
                HyperFactionsWorldMapProvider provider = new HyperFactionsWorldMapProvider();
                world.getWorldConfig().setWorldMapProvider((IWorldMapProvider) provider);
                Logger.debug("World map provider set successfully for: %s (provider class: %s)",
                        world.getName(), provider.getClass().getName());
            }

            // Track the world in WorldMapService
            hyperFactions.getWorldMapService().registerProviderIfNeeded(world);

            // Apply spawn suppression to the new world
            hyperFactions.getSpawnSuppressionManager().applyToWorld(world);
        } catch (Exception e) {
            getLogger().at(Level.WARNING).log("Error in AddWorldEvent handler for %s: %s",
                    world.getName(), e.getMessage());
            Logger.severe("AddWorldEvent error for %s", e, world.getName());
        }
    }

    /**
     * Handles world remove event - cleanup.
     */
    private void onWorldRemove(RemoveWorldEvent event) {
        World world = event.getWorld();
        try {
            hyperFactions.getWorldMapService().unregisterProvider(world.getName());
        } catch (Exception e) {
            getLogger().at(Level.WARNING).log("Error in RemoveWorldEvent handler for %s: %s",
                    world.getName(), e.getMessage());
        }
    }

    /**
     * Starts periodic tasks (power regen, combat tag decay, auto-save, invite cleanup).
     */
    private void startPeriodicTasks() {
        tickExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "HyperFactions-Ticker");
            t.setDaemon(true);
            return t;
        });

        // Power regeneration every minute
        powerRegenTask = tickExecutor.scheduleAtFixedRate(
            () -> {
                try {
                    hyperFactions.getPowerManager().tickPowerRegen();
                } catch (Exception e) {
                    Logger.severe("Error in power regen tick", e);
                }
            },
            60, 60, TimeUnit.SECONDS
        );

        // Combat tag decay every second
        combatTagTask = tickExecutor.scheduleAtFixedRate(
            () -> {
                try {
                    hyperFactions.getCombatTagManager().tickDecay();
                } catch (Exception e) {
                    Logger.severe("Error in combat tag tick", e);
                }
            },
            1, 1, TimeUnit.SECONDS
        );

        // Claim decay for inactive factions - runs hourly
        // Uses thread-safe ConcurrentHashMap operations internally
        claimDecayTask = tickExecutor.scheduleAtFixedRate(
            () -> {
                try {
                    hyperFactions.getClaimManager().tickClaimDecay();
                } catch (Exception e) {
                    Logger.severe("Error in claim decay tick", e);
                }
            },
            1, 1, TimeUnit.HOURS  // Initial delay of 1 hour, then every hour
        );

        // Territory tracking is now handled by TerritoryTickingSystem (ECS)
        // which ticks reliably every game tick for all player entities

        // Start core periodic tasks (auto-save, invite cleanup)
        hyperFactions.startPeriodicTasks();

        getLogger().at(Level.INFO).log("Started periodic tasks (including claim decay every hour)");
    }

    /**
     * Stops periodic tasks.
     */
    private void stopPeriodicTasks() {
        if (powerRegenTask != null) {
            powerRegenTask.cancel(false);
        }
        if (combatTagTask != null) {
            combatTagTask.cancel(false);
        }
        if (claimDecayTask != null) {
            claimDecayTask.cancel(false);
        }
        if (tickExecutor != null) {
            tickExecutor.shutdown();
        }
    }

    /**
     * Handles player connect event.
     */
    private void onPlayerConnect(PlayerConnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();
        UUID uuid = playerRef.getUuid();
        String username = playerRef.getUsername();

        Logger.debug("Player connecting: %s (%s)", username, uuid);

        // Track the player
        trackedPlayers.put(uuid, playerRef);

        // Load player power
        hyperFactions.getPowerManager().playerOnline(uuid);

        // Update faction member last online
        hyperFactions.getFactionManager().updateLastOnline(uuid);

        // Pre-warm chat history cache if player is in a faction
        com.hyperfactions.data.Faction playerFaction = hyperFactions.getFactionManager().getPlayerFaction(uuid);
        if (playerFaction != null && hyperFactions.getChatHistoryManager() != null) {
            hyperFactions.getChatHistoryManager().preWarmCache(playerFaction.id());
        }

        // Initialize territory tracking and send initial notification
        // World map provider is now registered via AddWorldEvent, not here
        try {
            UUID worldUuid = playerRef.getWorldUuid();
            if (worldUuid != null) {
                com.hypixel.hytale.server.core.universe.world.World world =
                    com.hypixel.hytale.server.core.universe.Universe.get().getWorld(worldUuid);
                if (world != null) {
                    // Get spawn position for initial territory notification
                    com.hypixel.hytale.math.vector.Transform transform = playerRef.getTransform();
                    if (transform != null) {
                        com.hypixel.hytale.math.vector.Vector3d position = transform.getPosition();
                        hyperFactions.getTerritoryNotifier().onPlayerConnect(
                            playerRef, world.getName(), position.getX(), position.getZ()
                        );
                    }
                }
            }
        } catch (Exception e) {
            Logger.debugTerritory("Failed to initialize territory tracking for %s: %s", username, e.getMessage());
        }
    }

    /**
     * Handles player disconnect event.
     */
    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();
        UUID uuid = playerRef.getUuid();
        String username = playerRef.getUsername();

        Logger.debug("Player disconnecting: %s", username);

        // Handle combat logout
        boolean wasCombatTagged = hyperFactions.getCombatTagManager().handleDisconnect(uuid);
        if (wasCombatTagged) {
            Logger.info("Player %s combat logged!", username);
        }

        // Cancel pending teleport
        hyperFactions.getTeleportManager().cancelPending(uuid, hyperFactions::cancelTask);

        // Mark player offline
        hyperFactions.getPowerManager().playerOffline(uuid);

        // Update faction member last online
        hyperFactions.getFactionManager().updateLastOnline(uuid);

        // Reset chat channel
        hyperFactions.getChatManager().resetChannel(uuid);

        // Evict chat history cache if no online faction members remain
        com.hyperfactions.data.Faction dcFaction = hyperFactions.getFactionManager().getPlayerFaction(uuid);
        if (dcFaction != null && hyperFactions.getChatHistoryManager() != null) {
            boolean anyOnline = dcFaction.members().keySet().stream()
                    .filter(id -> !id.equals(uuid))
                    .anyMatch(id -> trackedPlayers.containsKey(id));
            if (!anyOnline) {
                hyperFactions.getChatHistoryManager().evictCache(dcFaction.id());
            }
        }

        // Clean up territory tracking
        hyperFactions.getTerritoryNotifier().onPlayerDisconnect(uuid);

        // Unregister from active page tracker (GUI real-time updates)
        if (hyperFactions.getActivePageTracker() != null) {
            hyperFactions.getActivePageTracker().unregister(uuid);
        }

        // Untrack the player
        trackedPlayers.remove(uuid);
    }

    /**
     * Handles player chat event for faction/ally chat channels (async handler).
     */
    private CompletableFuture<PlayerChatEvent> onPlayerChatAsync(
            CompletableFuture<PlayerChatEvent> futureEvent) {
        return futureEvent.thenApply(event -> {
            if (event.isCancelled()) return event;

            PlayerRef sender = event.getSender();
            String message = event.getContent();

            // Check if player is in faction/ally chat mode
            boolean handled = hyperFactions.getChatManager().processChatMessage(sender, message);
            if (handled) {
                // Cancel the normal chat broadcast
                event.setCancelled(true);
            }
            return event;
        });
    }

    /**
     * Gets a tracked player by UUID.
     *
     * @param uuid the player's UUID
     * @return the PlayerRef, or null if not online
     */
    public PlayerRef getTrackedPlayer(UUID uuid) {
        return trackedPlayers.get(uuid);
    }

    /**
     * Gets all tracked players.
     *
     * @return map of UUID to PlayerRef
     */
    public Map<UUID, PlayerRef> getTrackedPlayers() {
        return trackedPlayers;
    }

    /**
     * Reloads the configuration.
     */
    public void reloadConfig() {
        hyperFactions.reloadConfig();
    }

    /**
     * Gets the HyperFactions instance.
     *
     * @return the HyperFactions instance
     */
    public HyperFactions getHyperFactions() {
        return hyperFactions;
    }

    /**
     * Gets the player listener.
     *
     * @return the player listener
     */
    public PlayerListener getPlayerListener() {
        return playerListener;
    }

    /**
     * Gets the protection listener.
     *
     * @return the protection listener
     */
    public ProtectionListener getProtectionListener() {
        return protectionListener;
    }

    // === ECS Event Systems ===

    /**
     * ECS system for canceling teleports when players take damage.
     */
    private static class TeleportCancelOnDamageSystem extends EntityEventSystem<EntityStore, Damage> {
        private final HyperFactions hyperFactions;
        private final HyperFactionsPlugin plugin;

        public TeleportCancelOnDamageSystem(HyperFactions hyperFactions, HyperFactionsPlugin plugin) {
            super(Damage.class);
            this.hyperFactions = hyperFactions;
            this.plugin = plugin;
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Archetype.empty();
        }

        @Override
        public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk,
                           Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer,
                           Damage event) {
            try {
                // Only process if damage was not cancelled
                if (event.isCancelled()) return;

                // Get the player being damaged
                PlayerRef player = chunk.getComponent(entityIndex, PlayerRef.getComponentType());
                if (player == null) return;

                UUID playerUuid = player.getUuid();

                // Check if player has a pending teleport and cancel it if configured
                hyperFactions.getTeleportManager().cancelOnDamage(
                    playerUuid,
                    player::sendMessage
                );
            } catch (Exception e) {
                Logger.severe("Error processing damage event for teleport cancellation", e);
            }
        }
    }
}
