package com.hyperfactions.platform;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.api.HyperFactionsAPI;
import com.hyperfactions.command.FactionCommand;
import com.hyperfactions.config.HyperFactionsConfig;
import com.hyperfactions.listener.PlayerListener;
import com.hyperfactions.listener.ProtectionListener;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Map;
import java.util.UUID;
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

    // Task scheduling
    private final AtomicInteger taskIdCounter = new AtomicInteger(0);
    private final Map<Integer, Object> scheduledTasks = new ConcurrentHashMap<>();

    // Player tracking
    private final Map<UUID, PlayerRef> trackedPlayers = new ConcurrentHashMap<>();

    // Periodic task executor
    private ScheduledExecutorService tickExecutor;
    private ScheduledFuture<?> powerRegenTask;
    private ScheduledFuture<?> combatTagTask;

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

        // Register commands
        registerCommands();

        // Register event listeners
        registerEventListeners();

        // Start periodic tasks
        startPeriodicTasks();

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
     * Registers event listeners with Hytale.
     */
    private void registerEventListeners() {
        // Player connect event
        getEventRegistry().register(PlayerConnectEvent.class, this::onPlayerConnect);

        // Player disconnect event
        getEventRegistry().register(PlayerDisconnectEvent.class, this::onPlayerDisconnect);

        // Create listeners
        playerListener = new PlayerListener(hyperFactions);
        protectionListener = new ProtectionListener(hyperFactions);

        // Register ECS event systems for block protection
        registerBlockProtectionSystems();

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

            getLogger().at(Level.INFO).log("Registered block protection systems");
        } catch (Exception e) {
            getLogger().at(Level.WARNING).withCause(e).log("Failed to register block protection systems");
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

        // Start core periodic tasks (auto-save, invite cleanup)
        hyperFactions.startPeriodicTasks();

        getLogger().at(Level.INFO).log("Started periodic tasks");
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

        // Untrack the player
        trackedPlayers.remove(uuid);
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

    // === ECS Event Systems for Block Protection ===

    /**
     * ECS system for handling block place protection.
     */
    private static class BlockPlaceProtectionSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {
        private final HyperFactions hyperFactions;
        private final ProtectionListener protectionListener;

        public BlockPlaceProtectionSystem(HyperFactions hyperFactions, ProtectionListener protectionListener) {
            super(PlaceBlockEvent.class);
            this.hyperFactions = hyperFactions;
            this.protectionListener = protectionListener;
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Archetype.empty();
        }

        @Override
        public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk,
                           Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer,
                           PlaceBlockEvent event) {
            try {
                PlayerRef player = chunk.getComponent(entityIndex, PlayerRef.getComponentType());
                if (player == null) return;

                Vector3i pos = event.getTargetBlock();
                // Get world name from store's external data (EntityStore)
                String worldName = getWorldName(store);
                if (worldName == null) return;

                boolean blocked = protectionListener.onBlockPlace(
                    player.getUuid(),
                    worldName,
                    pos.getX(), pos.getY(), pos.getZ()
                );

                if (blocked) {
                    event.setCancelled(true);
                    player.sendMessage(Message.raw(protectionListener.getDenialMessage(
                        hyperFactions.getProtectionChecker().canInteract(
                            player.getUuid(), worldName, pos.getX(), pos.getZ(),
                            com.hyperfactions.protection.ProtectionChecker.InteractionType.BUILD
                        )
                    )).color("#FF5555"));
                }
            } catch (Exception e) {
                Logger.severe("Error processing block place event", e);
            }
        }

        private String getWorldName(Store<EntityStore> store) {
            try {
                return store.getExternalData().getWorld().getName();
            } catch (Exception e) {
                return null;
            }
        }
    }

    /**
     * ECS system for handling block break protection.
     */
    private static class BlockBreakProtectionSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {
        private final HyperFactions hyperFactions;
        private final ProtectionListener protectionListener;

        public BlockBreakProtectionSystem(HyperFactions hyperFactions, ProtectionListener protectionListener) {
            super(BreakBlockEvent.class);
            this.hyperFactions = hyperFactions;
            this.protectionListener = protectionListener;
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Archetype.empty();
        }

        @Override
        public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk,
                           Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer,
                           BreakBlockEvent event) {
            try {
                PlayerRef player = chunk.getComponent(entityIndex, PlayerRef.getComponentType());
                if (player == null) return;

                Vector3i pos = event.getTargetBlock();
                String worldName = getWorldName(store);
                if (worldName == null) return;

                boolean blocked = protectionListener.onBlockBreak(
                    player.getUuid(),
                    worldName,
                    pos.getX(), pos.getY(), pos.getZ()
                );

                if (blocked) {
                    event.setCancelled(true);
                    player.sendMessage(Message.raw(protectionListener.getDenialMessage(
                        hyperFactions.getProtectionChecker().canInteract(
                            player.getUuid(), worldName, pos.getX(), pos.getZ(),
                            com.hyperfactions.protection.ProtectionChecker.InteractionType.BUILD
                        )
                    )).color("#FF5555"));
                }
            } catch (Exception e) {
                Logger.severe("Error processing block break event", e);
            }
        }

        private String getWorldName(Store<EntityStore> store) {
            try {
                return store.getExternalData().getWorld().getName();
            } catch (Exception e) {
                return null;
            }
        }
    }

    /**
     * ECS system for handling block use/interact protection.
     */
    private static class BlockUseProtectionSystem extends EntityEventSystem<EntityStore, UseBlockEvent.Pre> {
        private final HyperFactions hyperFactions;
        private final ProtectionListener protectionListener;

        public BlockUseProtectionSystem(HyperFactions hyperFactions, ProtectionListener protectionListener) {
            super(UseBlockEvent.Pre.class);
            this.hyperFactions = hyperFactions;
            this.protectionListener = protectionListener;
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Archetype.empty();
        }

        @Override
        public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk,
                           Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer,
                           UseBlockEvent.Pre event) {
            try {
                PlayerRef player = chunk.getComponent(entityIndex, PlayerRef.getComponentType());
                if (player == null) return;

                Vector3i pos = event.getTargetBlock();
                String worldName = getWorldName(store);
                if (worldName == null) return;

                // Use INTERACT for general block use
                boolean blocked = protectionListener.onBlockInteract(
                    player.getUuid(),
                    worldName,
                    pos.getX(), pos.getY(), pos.getZ()
                );

                if (blocked) {
                    event.setCancelled(true);
                    player.sendMessage(Message.raw(protectionListener.getDenialMessage(
                        hyperFactions.getProtectionChecker().canInteract(
                            player.getUuid(), worldName, pos.getX(), pos.getZ(),
                            com.hyperfactions.protection.ProtectionChecker.InteractionType.INTERACT
                        )
                    )).color("#FF5555"));
                }
            } catch (Exception e) {
                Logger.severe("Error processing block use event", e);
            }
        }

        private String getWorldName(Store<EntityStore> store) {
            try {
                return store.getExternalData().getWorld().getName();
            } catch (Exception e) {
                return null;
            }
        }
    }
}
