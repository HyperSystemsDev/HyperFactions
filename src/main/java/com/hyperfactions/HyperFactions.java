package com.hyperfactions;

import com.hyperfactions.api.events.EventBus;
import com.hyperfactions.api.events.FactionDisbandEvent;
import com.hyperfactions.backup.BackupManager;
import com.hyperfactions.config.HyperFactionsConfig;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.integration.HyperPermsIntegration;
import com.hyperfactions.integration.PermissionManager;
import com.hyperfactions.manager.*;
import com.hyperfactions.protection.ProtectionChecker;
import com.hyperfactions.protection.damage.DamageProtectionHandler;
import com.hyperfactions.protection.zone.ZoneDamageProtection;
import com.hyperfactions.protection.zone.ZoneInteractionProtection;
import com.hyperfactions.storage.FactionStorage;
import com.hyperfactions.storage.PlayerStorage;
import com.hyperfactions.storage.ZoneStorage;
import com.hyperfactions.storage.json.JsonFactionStorage;
import com.hyperfactions.storage.json.JsonPlayerStorage;
import com.hyperfactions.storage.json.JsonZoneStorage;
import com.hyperfactions.territory.TerritoryNotifier;
import com.hyperfactions.update.UpdateChecker;
import com.hyperfactions.update.UpdateNotificationListener;
import com.hyperfactions.update.UpdateNotificationPreferences;
import com.hyperfactions.util.Logger;
import com.hyperfactions.worldmap.WorldMapService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Main HyperFactions core class.
 * Platform-agnostic coordinator for all faction functionality.
 */
public class HyperFactions {

    /** Plugin version from BuildInfo (auto-generated at build time) */
    public static final String VERSION = BuildInfo.VERSION;

    private final Path dataDir;
    private final java.util.logging.Logger javaLogger;

    // Storage
    private FactionStorage factionStorage;
    private PlayerStorage playerStorage;
    private ZoneStorage zoneStorage;

    // Managers
    private FactionManager factionManager;
    private ClaimManager claimManager;
    private PowerManager powerManager;
    private RelationManager relationManager;
    private CombatTagManager combatTagManager;
    private ZoneManager zoneManager;
    private TeleportManager teleportManager;
    private InviteManager inviteManager;
    private JoinRequestManager joinRequestManager;
    private ChatManager chatManager;
    private ConfirmationManager confirmationManager;

    // Protection
    private ProtectionChecker protectionChecker;
    private ZoneDamageProtection zoneDamageProtection;
    private ZoneInteractionProtection zoneInteractionProtection;
    private DamageProtectionHandler damageProtectionHandler;

    // GUI
    private GuiManager guiManager;

    // Backup
    private BackupManager backupManager;

    // Update checker
    private UpdateChecker updateChecker;
    private UpdateNotificationListener updateNotificationListener;
    private UpdateNotificationPreferences notificationPreferences;

    // Territory features
    private TerritoryNotifier territoryNotifier;
    private WorldMapService worldMapService;

    // Task management
    private final AtomicInteger taskIdCounter = new AtomicInteger(0);
    private final Map<Integer, ScheduledTask> scheduledTasks = new ConcurrentHashMap<>();
    private int autoSaveTaskId = -1;
    private int inviteCleanupTaskId = -1;

    // Admin bypass state (per-player toggle for protection bypass)
    private final Map<UUID, Boolean> adminBypassEnabled = new ConcurrentHashMap<>();

    // Platform callbacks (set by plugin)
    private Consumer<Runnable> asyncExecutor;
    private TaskSchedulerCallback taskScheduler;
    private TaskCancelCallback taskCanceller;
    private RepeatingTaskSchedulerCallback repeatingTaskScheduler;
    private java.util.function.Function<UUID, com.hypixel.hytale.server.core.universe.PlayerRef> playerLookup;

    /**
     * Functional interface for scheduling delayed tasks.
     */
    @FunctionalInterface
    public interface TaskSchedulerCallback {
        int schedule(int delayTicks, Runnable task);
    }

    /**
     * Functional interface for cancelling tasks.
     */
    @FunctionalInterface
    public interface TaskCancelCallback {
        void cancel(int taskId);
    }

    /**
     * Functional interface for scheduling repeating tasks.
     */
    @FunctionalInterface
    public interface RepeatingTaskSchedulerCallback {
        int schedule(int delayTicks, int periodTicks, Runnable task);
    }

    /**
     * Represents a scheduled task.
     */
    private record ScheduledTask(int id, Runnable task) {}

    /**
     * Creates a new HyperFactions instance.
     *
     * @param dataDir    the plugin data directory
     * @param javaLogger the Java logger
     */
    public HyperFactions(@NotNull Path dataDir, @NotNull java.util.logging.Logger javaLogger) {
        this.dataDir = dataDir;
        this.javaLogger = javaLogger;
    }

    /**
     * Enables HyperFactions.
     */
    public void enable() {
        // Initialize logger
        Logger.init(javaLogger);
        Logger.info("HyperFactions v%s starting... (build: %d)", VERSION, BuildInfo.BUILD_TIMESTAMP);

        // Load configuration
        HyperFactionsConfig.get().load(dataDir);

        // Initialize HyperPerms integration (legacy, for backward compatibility)
        HyperPermsIntegration.init();

        // Initialize unified permission manager (new chain-based system)
        PermissionManager.get().init();

        // Preload Gson classes to avoid ClassNotFoundException on Timer threads
        // The Hytale PluginClassLoader doesn't properly propagate to Timer threads,
        // so we need to load all Gson inner classes on the main thread at startup.
        preloadGsonClasses();

        // Initialize storage
        factionStorage = new JsonFactionStorage(dataDir);
        playerStorage = new JsonPlayerStorage(dataDir);
        zoneStorage = new JsonZoneStorage(dataDir);

        factionStorage.init().join();
        playerStorage.init().join();
        zoneStorage.init().join();

        // Initialize managers (order matters!)
        factionManager = new FactionManager(factionStorage);
        powerManager = new PowerManager(playerStorage, factionManager);
        claimManager = new ClaimManager(factionManager, powerManager);
        relationManager = new RelationManager(factionManager);
        combatTagManager = new CombatTagManager();
        zoneManager = new ZoneManager(zoneStorage, claimManager);
        teleportManager = new TeleportManager(factionManager);
        inviteManager = new InviteManager(dataDir);
        joinRequestManager = new JoinRequestManager(dataDir);

        // Initialize invite/request managers (loads persisted data)
        inviteManager.init();
        joinRequestManager.init();

        // Initialize confirmation manager (for text-mode command confirmations)
        confirmationManager = new ConfirmationManager();

        // Initialize backup manager
        backupManager = new BackupManager(dataDir, this);
        backupManager.init();

        // Load data
        factionManager.loadAll().join();
        powerManager.loadAll().join();
        zoneManager.loadAll().join();

        // Build claim index after loading factions
        claimManager.buildIndex();

        // Initialize protection checker (with plugin reference for admin bypass toggle)
        protectionChecker = new ProtectionChecker(
            () -> this, factionManager, claimManager, zoneManager, relationManager, combatTagManager
        );

        // Initialize zone damage protection
        zoneDamageProtection = new ZoneDamageProtection(this);

        // Initialize zone interaction protection
        zoneInteractionProtection = new ZoneInteractionProtection(this);

        // Initialize damage protection handler (coordinates all protection systems)
        // Note: denialMessageProvider will be set by plugin after ProtectionListener is created
        damageProtectionHandler = null; // Initialized later by plugin

        // Initialize GUI manager
        guiManager = new GuiManager(
            () -> this,
            () -> factionManager,
            () -> claimManager,
            () -> powerManager,
            () -> relationManager,
            () -> zoneManager,
            () -> teleportManager,
            () -> inviteManager,
            () -> joinRequestManager,
            () -> dataDir
        );

        // Initialize chat manager (uses deferred playerLookup)
        chatManager = new ChatManager(factionManager, relationManager,
            uuid -> playerLookup != null ? playerLookup.apply(uuid) : null);

        // Setup combat tag callbacks
        combatTagManager.setOnCombatLogout(playerUuid -> {
            // Apply combat logout penalty (configurable, default same as death penalty)
            double penalty = HyperFactionsConfig.get().getLogoutPowerLoss();
            powerManager.applyCombatLogoutPenalty(playerUuid, penalty);
            Logger.info("Player %s combat logged - %.1f power penalty applied", playerUuid, penalty);
        });

        // Register faction disband event listener to clean up all associated data
        EventBus.register(FactionDisbandEvent.class, this::handleFactionDisband);

        // Initialize territory notifier (for entry/exit notifications)
        territoryNotifier = new TerritoryNotifier(
            factionManager, claimManager, zoneManager, relationManager
        );

        // Initialize world map service (for claim markers on map)
        worldMapService = new WorldMapService(
            factionManager, claimManager, zoneManager, relationManager
        );

        // Wire up claim change callback to refresh world maps
        claimManager.setOnClaimChangeCallback(worldMapService::refreshAllWorldMaps);

        // Wire up zone change callback to refresh world maps
        zoneManager.setOnZoneChangeCallback(worldMapService::refreshAllWorldMaps);

        // Initialize update checker if enabled
        if (HyperFactionsConfig.get().isUpdateCheckEnabled()) {
            updateChecker = new UpdateChecker(dataDir, VERSION, HyperFactionsConfig.get().getUpdateCheckUrl(),
                    HyperFactionsConfig.get().isPreReleaseChannel());
            updateChecker.checkForUpdates();

            // Initialize notification preferences
            notificationPreferences = new UpdateNotificationPreferences(dataDir);
            notificationPreferences.load();

            // Initialize update notification listener
            updateNotificationListener = new UpdateNotificationListener(this);
        }

        // Start periodic tasks (auto-save, invite cleanup)
        // Note: These are started after platform sets callbacks via setRepeatingTaskScheduler()
        // The platform should call startPeriodicTasks() after setting up callbacks

        Logger.info("HyperFactions enabled");
    }

    /**
     * Starts periodic tasks (auto-save, invite cleanup, scheduled backups).
     * Should be called by the platform after setting up task scheduler callbacks.
     */
    public void startPeriodicTasks() {
        startAutoSaveTask();
        startInviteCleanupTask();
        // Start scheduled backups now that the task scheduler is available
        if (backupManager != null) {
            backupManager.startScheduledBackups();
        }
    }

    /**
     * Disables HyperFactions.
     */
    public void disable() {
        Logger.info("HyperFactions disabling...");

        // Cancel periodic tasks first
        if (autoSaveTaskId > 0) {
            cancelTask(autoSaveTaskId);
            autoSaveTaskId = -1;
        }
        if (inviteCleanupTaskId > 0) {
            cancelTask(inviteCleanupTaskId);
            inviteCleanupTaskId = -1;
        }

        // Save all data
        saveAllData();

        // Shutdown backup manager (creates shutdown backup if configured)
        if (backupManager != null) {
            backupManager.shutdown();
        }

        // Shutdown invite/request managers (saves persisted data)
        if (inviteManager != null) {
            inviteManager.shutdown();
        }
        if (joinRequestManager != null) {
            joinRequestManager.shutdown();
        }

        // Shutdown storage
        if (factionStorage != null) {
            factionStorage.shutdown().join();
        }
        if (playerStorage != null) {
            playerStorage.shutdown().join();
        }
        if (zoneStorage != null) {
            zoneStorage.shutdown().join();
        }

        // Shutdown update notification listener
        if (updateNotificationListener != null) {
            updateNotificationListener.shutdown();
        }

        // Shutdown territory services
        if (territoryNotifier != null) {
            territoryNotifier.shutdown();
        }
        if (worldMapService != null) {
            worldMapService.shutdown();
        }

        // Cancel remaining scheduled tasks
        for (int taskId : scheduledTasks.keySet()) {
            cancelTask(taskId);
        }

        Logger.info("HyperFactions disabled");
    }

    /**
     * Preloads Gson classes to avoid ClassNotFoundException on Timer threads.
     * The Hytale PluginClassLoader doesn't properly propagate class visibility
     * to Timer threads, so we load all needed Gson inner classes at startup.
     */
    private void preloadGsonClasses() {
        try {
            // Create a test object and serialize/deserialize it
            // This forces all Gson internal classes to be loaded
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder()
                    .setPrettyPrinting()
                    .create();

            // Create a JsonObject with various types to trigger class loading
            com.google.gson.JsonObject testObj = new com.google.gson.JsonObject();
            testObj.addProperty("string", "test");
            testObj.addProperty("number", 42);
            testObj.addProperty("boolean", true);

            com.google.gson.JsonArray testArray = new com.google.gson.JsonArray();
            testArray.add("item1");
            testArray.add("item2");
            testObj.add("array", testArray);

            // Serialize to JSON string (triggers LinkedTreeMap$EntrySet and related classes)
            String json = gson.toJson(testObj);

            // Also iterate over entries (triggers entrySet inner classes)
            for (var entry : testObj.entrySet()) {
                // Force class loading
                @SuppressWarnings("unused")
                String key = entry.getKey();
            }

            // Parse back (triggers other internal classes)
            gson.fromJson(json, com.google.gson.JsonObject.class);

            Logger.debug("Gson classes preloaded successfully");
        } catch (Exception e) {
            Logger.warn("Failed to preload Gson classes: %s", e.getMessage());
        }
    }

    /**
     * Reloads the configuration.
     */
    public void reloadConfig() {
        HyperFactionsConfig.get().reload(dataDir);
        Logger.info("Configuration reloaded");
    }

    // === Platform callbacks ===

    public void setAsyncExecutor(@NotNull Consumer<Runnable> executor) {
        this.asyncExecutor = executor;
    }

    public void setTaskScheduler(@NotNull TaskSchedulerCallback scheduler) {
        this.taskScheduler = scheduler;
    }

    public void setTaskCanceller(@NotNull TaskCancelCallback canceller) {
        this.taskCanceller = canceller;
    }

    public void setRepeatingTaskScheduler(@NotNull RepeatingTaskSchedulerCallback scheduler) {
        this.repeatingTaskScheduler = scheduler;
    }

    public void setPlayerLookup(@NotNull java.util.function.Function<UUID, com.hypixel.hytale.server.core.universe.PlayerRef> lookup) {
        this.playerLookup = lookup;
        // Also set up the permission manager's player lookup for OP checks
        PermissionManager.get().setPlayerLookup(lookup);
    }

    // === Task scheduling ===

    /**
     * Schedules a delayed task.
     *
     * @param delayTicks the delay in ticks
     * @param task       the task
     * @return the task ID
     */
    public int scheduleDelayedTask(int delayTicks, @NotNull Runnable task) {
        if (taskScheduler != null) {
            int id = taskIdCounter.incrementAndGet();
            int platformId = taskScheduler.schedule(delayTicks, () -> {
                scheduledTasks.remove(id);
                task.run();
            });
            scheduledTasks.put(id, new ScheduledTask(platformId, task));
            return id;
        }
        // Fallback: run immediately
        task.run();
        return -1;
    }

    /**
     * Cancels a task.
     *
     * @param taskId the task ID
     */
    public void cancelTask(int taskId) {
        ScheduledTask task = scheduledTasks.remove(taskId);
        if (task != null && taskCanceller != null) {
            taskCanceller.cancel(task.id());
        }
    }

    /**
     * Schedules a repeating task.
     *
     * @param delayTicks  initial delay in ticks
     * @param periodTicks period in ticks
     * @param task        the task
     * @return the task ID
     */
    public int scheduleRepeatingTask(int delayTicks, int periodTicks, @NotNull Runnable task) {
        if (repeatingTaskScheduler != null) {
            int id = taskIdCounter.incrementAndGet();
            int platformId = repeatingTaskScheduler.schedule(delayTicks, periodTicks, task);
            scheduledTasks.put(id, new ScheduledTask(platformId, task));
            return id;
        }
        return -1;
    }

    /**
     * Performs a save of all data.
     * Called periodically by auto-save and on shutdown.
     */
    public void saveAllData() {
        Logger.info("Auto-saving data...");
        if (factionManager != null) {
            factionManager.saveAll().join();
        }
        if (powerManager != null) {
            powerManager.saveAll().join();
        }
        if (zoneManager != null) {
            zoneManager.saveAll().join();
        }
        Logger.info("Auto-save complete");
    }

    /**
     * Starts the auto-save periodic task if enabled.
     */
    private void startAutoSaveTask() {
        HyperFactionsConfig config = HyperFactionsConfig.get();
        if (!config.isAutoSaveEnabled()) {
            Logger.info("Auto-save is disabled in config");
            return;
        }

        int intervalMinutes = config.getAutoSaveIntervalMinutes();
        if (intervalMinutes <= 0) {
            Logger.warn("Invalid auto-save interval: %d minutes, using default 5 minutes", intervalMinutes);
            intervalMinutes = 5;
        }

        int periodTicks = intervalMinutes * 60 * 20; // Convert minutes to ticks (20 ticks per second)
        autoSaveTaskId = scheduleRepeatingTask(periodTicks, periodTicks, this::saveAllData);

        if (autoSaveTaskId > 0) {
            Logger.info("Auto-save scheduled every %d minutes", intervalMinutes);
        }
    }

    /**
     * Starts the invite cleanup periodic task.
     * Also cleans up expired join requests.
     */
    private void startInviteCleanupTask() {
        // Run every 5 minutes (6000 ticks)
        int periodTicks = 5 * 60 * 20;
        inviteCleanupTaskId = scheduleRepeatingTask(periodTicks, periodTicks, () -> {
            if (inviteManager != null) {
                inviteManager.cleanupExpired();
            }
            if (joinRequestManager != null) {
                joinRequestManager.cleanupExpired();
            }
        });

        if (inviteCleanupTaskId > 0) {
            Logger.info("Invite/request cleanup task scheduled every 5 minutes");
        }
    }

    // === Getters ===

    @NotNull
    public Path getDataDir() {
        return dataDir;
    }

    @NotNull
    public FactionManager getFactionManager() {
        return factionManager;
    }

    @NotNull
    public ClaimManager getClaimManager() {
        return claimManager;
    }

    @NotNull
    public PowerManager getPowerManager() {
        return powerManager;
    }

    @NotNull
    public RelationManager getRelationManager() {
        return relationManager;
    }

    @NotNull
    public CombatTagManager getCombatTagManager() {
        return combatTagManager;
    }

    @NotNull
    public ZoneManager getZoneManager() {
        return zoneManager;
    }

    @NotNull
    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    @NotNull
    public InviteManager getInviteManager() {
        return inviteManager;
    }

    @NotNull
    public JoinRequestManager getJoinRequestManager() {
        return joinRequestManager;
    }

    @NotNull
    public ChatManager getChatManager() {
        return chatManager;
    }

    public ConfirmationManager getConfirmationManager() {
        return confirmationManager;
    }

    @NotNull
    public ProtectionChecker getProtectionChecker() {
        return protectionChecker;
    }

    @NotNull
    public ZoneDamageProtection getZoneDamageProtection() {
        return zoneDamageProtection;
    }

    @NotNull
    public ZoneInteractionProtection getZoneInteractionProtection() {
        return zoneInteractionProtection;
    }

    /**
     * Gets the damage protection handler that coordinates all protection systems.
     *
     * @return the damage protection handler, or null if not yet initialized
     */
    @Nullable
    public DamageProtectionHandler getDamageProtectionHandler() {
        return damageProtectionHandler;
    }

    /**
     * Sets the damage protection handler. Called by the plugin after creating ProtectionListener.
     *
     * @param handler the damage protection handler
     */
    public void setDamageProtectionHandler(@NotNull DamageProtectionHandler handler) {
        this.damageProtectionHandler = handler;
    }

    @NotNull
    public GuiManager getGuiManager() {
        return guiManager;
    }

    /**
     * Gets the backup manager.
     *
     * @return the backup manager
     */
    @NotNull
    public BackupManager getBackupManager() {
        return backupManager;
    }

    /**
     * Gets the update checker.
     *
     * @return the update checker, or null if update checking is disabled
     */
    @Nullable
    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    /**
     * Gets the update notification listener.
     *
     * @return the update notification listener, or null if update checking is disabled
     */
    @Nullable
    public UpdateNotificationListener getUpdateNotificationListener() {
        return updateNotificationListener;
    }

    /**
     * Gets the notification preferences manager.
     *
     * @return the notification preferences, or null if update checking is disabled
     */
    @Nullable
    public UpdateNotificationPreferences getNotificationPreferences() {
        return notificationPreferences;
    }

    /**
     * Gets the data directory.
     *
     * @return the plugin data directory
     */
    @NotNull
    public Path getDataDirectory() {
        return dataDir;
    }

    /**
     * Gets the territory notifier.
     *
     * @return the territory notifier
     */
    @NotNull
    public TerritoryNotifier getTerritoryNotifier() {
        return territoryNotifier;
    }

    /**
     * Gets the world map service.
     *
     * @return the world map service
     */
    @NotNull
    public WorldMapService getWorldMapService() {
        return worldMapService;
    }

    // === Admin Bypass Toggle ===

    /**
     * Checks if admin bypass is enabled for a player.
     * When enabled, the player can bypass protection checks in claimed territory.
     *
     * @param playerUuid the player's UUID
     * @return true if admin bypass is enabled for this player
     */
    public boolean isAdminBypassEnabled(@NotNull UUID playerUuid) {
        return adminBypassEnabled.getOrDefault(playerUuid, false);
    }

    /**
     * Toggles admin bypass state for a player.
     * When toggled on, the player can bypass protection checks in claimed territory.
     *
     * @param playerUuid the player's UUID
     * @return true if bypass is now enabled, false if now disabled
     */
    public boolean toggleAdminBypass(@NotNull UUID playerUuid) {
        return adminBypassEnabled.compute(playerUuid, (k, v) -> v == null || !v);
    }

    /**
     * Handles faction disband event by cleaning up all associated data.
     * This is called by the EventBus when any faction is disbanded.
     *
     * @param event the disband event
     */
    private void handleFactionDisband(@NotNull FactionDisbandEvent event) {
        UUID factionId = event.faction().id();
        Logger.info("Cleaning up data for disbanded faction '%s' (ID: %s)",
                event.faction().name(), factionId);

        // Clean up claims
        claimManager.unclaimAll(factionId);

        // Clean up invites
        inviteManager.clearFactionInvites(factionId);

        // Clean up join requests
        joinRequestManager.clearFactionRequests(factionId);

        // Clean up relations
        relationManager.clearAllRelations(factionId);
    }
}
