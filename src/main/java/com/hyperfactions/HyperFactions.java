package com.hyperfactions;

import com.hyperfactions.config.HyperFactionsConfig;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.integration.HyperPermsIntegration;
import com.hyperfactions.manager.*;
import com.hyperfactions.protection.ProtectionChecker;
import com.hyperfactions.storage.FactionStorage;
import com.hyperfactions.storage.PlayerStorage;
import com.hyperfactions.storage.ZoneStorage;
import com.hyperfactions.storage.json.JsonFactionStorage;
import com.hyperfactions.storage.json.JsonPlayerStorage;
import com.hyperfactions.storage.json.JsonZoneStorage;
import com.hyperfactions.update.UpdateChecker;
import com.hyperfactions.util.Logger;
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

    public static final String VERSION = "1.0.0";

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

    // Protection
    private ProtectionChecker protectionChecker;

    // GUI
    private GuiManager guiManager;

    // Update checker
    private UpdateChecker updateChecker;

    // Task management
    private final AtomicInteger taskIdCounter = new AtomicInteger(0);
    private final Map<Integer, ScheduledTask> scheduledTasks = new ConcurrentHashMap<>();

    // Platform callbacks (set by plugin)
    private Consumer<Runnable> asyncExecutor;
    private TaskSchedulerCallback taskScheduler;
    private TaskCancelCallback taskCanceller;

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
        Logger.info("HyperFactions v%s starting...", VERSION);

        // Load configuration
        HyperFactionsConfig.get().load(dataDir);

        // Initialize HyperPerms integration
        HyperPermsIntegration.init();

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
        inviteManager = new InviteManager();

        // Load data
        factionManager.loadAll().join();
        powerManager.loadAll().join();
        zoneManager.loadAll().join();

        // Build claim index after loading factions
        claimManager.buildIndex();

        // Initialize protection checker
        protectionChecker = new ProtectionChecker(
            factionManager, claimManager, zoneManager, relationManager, combatTagManager
        );

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
            () -> dataDir
        );

        // Setup combat tag callbacks
        combatTagManager.setOnCombatLogout(playerUuid -> {
            // Apply death penalty on combat logout
            powerManager.applyDeathPenalty(playerUuid);
            Logger.info("Player %s combat logged - death penalty applied", playerUuid);
        });

        // Initialize update checker if enabled
        if (HyperFactionsConfig.get().isUpdateCheckEnabled()) {
            updateChecker = new UpdateChecker(dataDir, VERSION, HyperFactionsConfig.get().getUpdateCheckUrl());
            updateChecker.checkForUpdates();
        }

        Logger.info("HyperFactions enabled");
    }

    /**
     * Disables HyperFactions.
     */
    public void disable() {
        Logger.info("HyperFactions disabling...");

        // Save all data
        if (factionManager != null) {
            factionManager.saveAll().join();
        }
        if (powerManager != null) {
            powerManager.saveAll().join();
        }
        if (zoneManager != null) {
            zoneManager.saveAll().join();
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

        // Cancel all scheduled tasks
        for (int taskId : scheduledTasks.keySet()) {
            cancelTask(taskId);
        }

        Logger.info("HyperFactions disabled");
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
    public ProtectionChecker getProtectionChecker() {
        return protectionChecker;
    }

    @NotNull
    public GuiManager getGuiManager() {
        return guiManager;
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
}
