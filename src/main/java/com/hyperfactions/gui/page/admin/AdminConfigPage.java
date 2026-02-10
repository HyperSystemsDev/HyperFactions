package com.hyperfactions.gui.page.admin;

import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.config.CoreConfig;
import com.hyperfactions.config.modules.*;
import com.hyperfactions.data.FactionPermissions;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.admin.AdminNavBarHelper;
import com.hyperfactions.gui.admin.data.AdminConfigData;
import com.hyperfactions.gui.page.admin.config.ConfigChangeTracker;
import com.hyperfactions.gui.page.admin.config.ConfigDescriptions;
import com.hyperfactions.gui.page.admin.config.ConfigTabType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin Config page - full-featured configuration editor with tabbed sections.
 */
public class AdminConfigPage extends InteractiveCustomUIPage<AdminConfigData> {

    private final PlayerRef playerRef;
    private final GuiManager guiManager;

    private ConfigTabType activeTab = ConfigTabType.GENERAL;
    private final ConfigChangeTracker changeTracker = new ConfigChangeTracker();
    private String selectedRole = "member";
    private boolean confirmReset = false;

    // Row counter for unique IDs within a tab build
    private int rowIndex;

    public AdminConfigPage(PlayerRef playerRef, GuiManager guiManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, AdminConfigData.CODEC);
        this.playerRef = playerRef;
        this.guiManager = guiManager;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {
        cmd.append("HyperFactions/admin/admin_config.ui");
        AdminNavBarHelper.setupBar(playerRef, "config", cmd, events);

        buildTabBar(cmd, events);
        buildTabContent(cmd, events);
        buildFooter(cmd, events);
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                AdminConfigData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null) return;

        if (AdminNavBarHelper.handleNavEvent(data, player, ref, store, playerRef, guiManager)) {
            return;
        }

        if (data.button == null) return;

        switch (data.button) {
            // Tab navigation
            case "SwitchTab" -> {
                if (data.tab != null) {
                    try {
                        activeTab = ConfigTabType.valueOf(data.tab);
                    } catch (IllegalArgumentException ignored) {}
                    confirmReset = false;
                    sendUpdate();
                }
            }

            // Boolean toggle
            case "ToggleBool" -> {
                if (data.key != null) {
                    boolean current = getEffectiveBool(data.key);
                    changeTracker.set(data.key, !current);
                    sendUpdate();
                }
            }

            // Increment numeric
            case "Increment" -> {
                if (data.key != null) handleIncrement(data.key);
            }

            // Decrement numeric
            case "Decrement" -> {
                if (data.key != null) handleDecrement(data.key);
            }

            // Text field updated
            case "UpdateText" -> {
                if (data.key != null && data.value != null) {
                    changeTracker.set(data.key, data.value);
                    // Don't rebuild - text is being typed
                }
            }

            // Enum/dropdown changed
            case "UpdateEnum" -> {
                if (data.key != null && data.value != null) {
                    changeTracker.set(data.key, data.value);
                    sendUpdate();
                }
            }

            // Color picker changed
            case "UpdateColor" -> {
                if (data.key != null && data.value != null) {
                    changeTracker.set(data.key, data.value);
                    // Don't rebuild for color changes - would reset the picker
                }
            }

            // Array operations
            case "AddListEntry" -> {
                if (data.key != null && data.value != null && !data.value.isEmpty()) {
                    handleAddListEntry(data.key, data.value);
                    sendUpdate();
                }
            }
            case "RemoveListEntry" -> {
                if (data.key != null && data.listItem != null) {
                    handleRemoveListEntry(data.key, data.listItem);
                    sendUpdate();
                }
            }

            // Protection tab - role selection
            case "SelectRole" -> {
                if (data.role != null) {
                    selectedRole = data.role;
                    sendUpdate();
                }
            }

            // Protection tab - toggle perm default/lock
            case "TogglePermDefault" -> {
                if (data.key != null) {
                    boolean current = getEffectivePermDefault(data.key);
                    changeTracker.set("perm.default." + data.key, !current);
                    sendUpdate();
                }
            }
            case "TogglePermLock" -> {
                if (data.key != null) {
                    boolean current = getEffectivePermLock(data.key);
                    changeTracker.set("perm.lock." + data.key, !current);
                    sendUpdate();
                }
            }

            // Footer actions
            case "Save" -> handleSave(player, ref, store, playerRef);
            case "Revert" -> {
                changeTracker.clear();
                confirmReset = false;
                sendUpdate();
                player.sendMessage(Message.raw("Changes reverted.").color("#00FFFF"));
            }
            case "ResetDefaults" -> {
                confirmReset = true;
                sendUpdate();
            }
            case "ConfirmReset" -> handleConfirmReset(player, ref, store, playerRef);
            case "CancelReset" -> {
                confirmReset = false;
                sendUpdate();
            }

            // Callout
            case "ShowHelp" -> {
                if (data.key != null) {
                    // Callout text is set during rebuild, triggered by sendUpdate
                }
            }

            // Back
            case "Back" -> guiManager.closePage(player, ref, store);
        }
    }

    // =========================================================================
    // TAB BAR
    // =========================================================================

    private void buildTabBar(UICommandBuilder cmd, UIEventBuilder events) {
        for (ConfigTabType tab : ConfigTabType.values()) {
            String btnId = "#Tab" + capitalize(tab.name().toLowerCase());
            boolean isActive = tab == activeTab;

            cmd.set(btnId + ".Style", Value.ref("HyperFactions/shared/styles.ui",
                    isActive ? "TabActiveButtonStyle" : "TabButtonStyle"));

            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    btnId,
                    EventData.of("Button", "SwitchTab").append("Tab", tab.name()),
                    false
            );
        }
    }

    private String capitalize(String s) {
        if (s.isEmpty()) return s;
        // Handle multi-word like "worldmap" -> "Worldmap"
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    // =========================================================================
    // TAB CONTENT DISPATCH
    // =========================================================================

    private void buildTabContent(UICommandBuilder cmd, UIEventBuilder events) {
        rowIndex = 0;
        switch (activeTab) {
            case GENERAL -> buildGeneralTab(cmd, events);
            case POWER -> buildPowerTab(cmd, events);
            case CLAIMS -> buildClaimsTab(cmd, events);
            case COMBAT -> buildCombatTab(cmd, events);
            case CHAT -> buildChatTab(cmd, events);
            case MODULES -> buildModulesTab(cmd, events);
            case WORLDMAP -> buildWorldMapTab(cmd, events);
            case PROTECTION -> buildProtectionTab(cmd, events);
        }
    }

    // =========================================================================
    // GENERAL TAB
    // =========================================================================

    private void buildGeneralTab(UICommandBuilder cmd, UIEventBuilder events) {
        ConfigManager cfg = ConfigManager.get();
        CoreConfig core = cfg.core();

        addSectionHeader(cmd, "Faction");
        addIntRow(cmd, events, "faction.maxMembers", "Max Members", core.getMaxMembers(), 1);
        addIntRow(cmd, events, "faction.maxNameLength", "Max Name Length", core.getMaxNameLength(), 1);
        addIntRow(cmd, events, "faction.minNameLength", "Min Name Length", core.getMinNameLength(), 1);
        addBoolRow(cmd, events, "faction.allowColors", "Allow Colors", core.isAllowColors());

        addSectionHeader(cmd, "Relations");
        addIntRow(cmd, events, "relations.maxAllies", "Max Allies", core.getMaxAllies(), 1);
        addIntRow(cmd, events, "relations.maxEnemies", "Max Enemies", core.getMaxEnemies(), 1);

        addSectionHeader(cmd, "Invites");
        addIntRow(cmd, events, "invites.inviteExpirationMinutes", "Invite Expiration (min)", core.getInviteExpirationMinutes(), 1);
        addIntRow(cmd, events, "invites.joinRequestExpirationHours", "Join Request Expiration (hrs)", core.getJoinRequestExpirationHours(), 1);

        addSectionHeader(cmd, "Teleport");
        addIntRow(cmd, events, "teleport.warmupSeconds", "Warmup (sec)", core.getWarmupSeconds(), 1);
        addIntRow(cmd, events, "teleport.cooldownSeconds", "Cooldown (sec)", core.getCooldownSeconds(), 5);
        addBoolRow(cmd, events, "teleport.cancelOnMove", "Cancel on Move", core.isCancelOnMove());
        addBoolRow(cmd, events, "teleport.cancelOnDamage", "Cancel on Damage", core.isCancelOnDamage());

        addSectionHeader(cmd, "Stuck Command");
        addIntRow(cmd, events, "stuck.warmupSeconds", "Warmup (sec)", core.getStuckWarmupSeconds(), 5);
        addIntRow(cmd, events, "stuck.cooldownSeconds", "Cooldown (sec)", core.getStuckCooldownSeconds(), 10);

        addSectionHeader(cmd, "Permissions");
        addBoolRow(cmd, events, "permissions.adminRequiresOp", "Admin Requires OP", core.isAdminRequiresOp());
        addBoolRow(cmd, events, "permissions.allowWithoutPermissionMod", "Allow Without Perm Plugin", core.isAllowWithoutPermissionMod());

        addSectionHeader(cmd, "Auto-Save");
        addBoolRow(cmd, events, "autoSave.enabled", "Enabled", core.isAutoSaveEnabled());
        addIntRow(cmd, events, "autoSave.intervalMinutes", "Interval (min)", core.getAutoSaveIntervalMinutes(), 1);

        addSectionHeader(cmd, "Updates");
        addBoolRow(cmd, events, "updates.enabled", "Update Check", core.isUpdateCheckEnabled());
        addEnumRow(cmd, events, "updates.releaseChannel", "Release Channel", core.getReleaseChannel(),
                List.of("stable", "prerelease"));

        addSectionHeader(cmd, "Messages");
        addTextRow(cmd, events, "messages.prefixText", "Prefix Text", core.getPrefixText());
        addTextRow(cmd, events, "messages.prefixColor", "Prefix Color", core.getPrefixColor());
        addTextRow(cmd, events, "messages.prefixBracketColor", "Bracket Color", core.getPrefixBracketColor());
        addTextRow(cmd, events, "messages.primaryColor", "Primary Color", core.getPrimaryColor());

        addSectionHeader(cmd, "GUI");
        addTextRow(cmd, events, "gui.title", "GUI Title", core.getGuiTitle());

        addSectionHeader(cmd, "Territory");
        addBoolRow(cmd, events, "territoryNotifications.enabled", "Territory Notifications", core.isTerritoryNotificationsEnabled());
    }

    // =========================================================================
    // POWER TAB
    // =========================================================================

    private void buildPowerTab(UICommandBuilder cmd, UIEventBuilder events) {
        CoreConfig core = ConfigManager.get().core();

        addSectionHeader(cmd, "Power Settings");
        addDoubleRow(cmd, events, "power.maxPlayerPower", "Max Player Power", core.getMaxPlayerPower(), 1.0);
        addDoubleRow(cmd, events, "power.startingPower", "Starting Power", core.getStartingPower(), 1.0);
        addDoubleRow(cmd, events, "power.powerPerClaim", "Power Per Claim", core.getPowerPerClaim(), 0.5);
        addDoubleRow(cmd, events, "power.deathPenalty", "Death Penalty", core.getDeathPenalty(), 0.5);
        addDoubleRow(cmd, events, "power.killReward", "Kill Reward", core.getKillReward(), 0.5);
        addDoubleRow(cmd, events, "power.regenPerMinute", "Regen Per Minute", core.getRegenPerMinute(), 0.05);
        addBoolRow(cmd, events, "power.regenWhenOffline", "Regen When Offline", core.isRegenWhenOffline());
    }

    // =========================================================================
    // CLAIMS TAB
    // =========================================================================

    private void buildClaimsTab(UICommandBuilder cmd, UIEventBuilder events) {
        CoreConfig core = ConfigManager.get().core();
        EconomyConfig econ = ConfigManager.get().economy();

        addSectionHeader(cmd, "Claims");
        addIntRow(cmd, events, "claims.maxClaims", "Max Claims", core.getMaxClaims(), 5);
        addBoolRow(cmd, events, "claims.onlyAdjacent", "Only Adjacent", core.isOnlyAdjacent());
        addBoolRow(cmd, events, "claims.preventDisconnect", "Prevent Disconnect", core.isPreventDisconnect());
        addBoolRow(cmd, events, "claims.decayEnabled", "Decay Enabled", core.isDecayEnabled());
        addIntRow(cmd, events, "claims.decayDaysInactive", "Decay Days Inactive", core.getDecayDaysInactive(), 1);

        addSectionHeader(cmd, "World Whitelist");
        addWorldListSection(cmd, events, "claims.worldWhitelist", core.getWorldWhitelist());

        addSectionHeader(cmd, "World Blacklist");
        addWorldListSection(cmd, events, "claims.worldBlacklist", core.getWorldBlacklist());

        addSectionHeader(cmd, "Economy");
        addBoolRow(cmd, events, "economy.enabled", "Enabled", econ.isEnabled());
        addTextRow(cmd, events, "economy.currencyName", "Currency Name", econ.getCurrencyName());
        addTextRow(cmd, events, "economy.currencyNamePlural", "Currency Plural", econ.getCurrencyNamePlural());
        addTextRow(cmd, events, "economy.currencySymbol", "Currency Symbol", econ.getCurrencySymbol());
        addDoubleRow(cmd, events, "economy.startingBalance", "Starting Balance", econ.getStartingBalance(), 10.0);
    }

    // =========================================================================
    // COMBAT TAB
    // =========================================================================

    private void buildCombatTab(UICommandBuilder cmd, UIEventBuilder events) {
        CoreConfig core = ConfigManager.get().core();

        addSectionHeader(cmd, "Combat");
        addIntRow(cmd, events, "combat.tagDurationSeconds", "Tag Duration (sec)", core.getTagDurationSeconds(), 1);
        addBoolRow(cmd, events, "combat.allyDamage", "Ally Damage", core.isAllyDamage());
        addBoolRow(cmd, events, "combat.factionDamage", "Faction Damage", core.isFactionDamage());
        addBoolRow(cmd, events, "combat.taggedLogoutPenalty", "Logout Penalty", core.isTaggedLogoutPenalty());
        addDoubleRow(cmd, events, "combat.logoutPowerLoss", "Logout Power Loss", core.getLogoutPowerLoss(), 0.5);
        addDoubleRow(cmd, events, "combat.neutralAttackPenalty", "Neutral Attack Penalty", core.getNeutralAttackPenalty(), 0.5);

        addSectionHeader(cmd, "Spawn Protection");
        addBoolRow(cmd, events, "combat.spawnProtection.enabled", "Enabled", core.isSpawnProtectionEnabled());
        addIntRow(cmd, events, "combat.spawnProtection.durationSeconds", "Duration (sec)", core.getSpawnProtectionDurationSeconds(), 1);
        addBoolRow(cmd, events, "combat.spawnProtection.breakOnAttack", "Break on Attack", core.isSpawnProtectionBreakOnAttack());
        addBoolRow(cmd, events, "combat.spawnProtection.breakOnMove", "Break on Move", core.isSpawnProtectionBreakOnMove());
    }

    // =========================================================================
    // CHAT TAB
    // =========================================================================

    private void buildChatTab(UICommandBuilder cmd, UIEventBuilder events) {
        ChatConfig chat = ConfigManager.get().chat();

        addSectionHeader(cmd, "Chat Format");
        addBoolRow(cmd, events, "chat.enabled", "Enabled", chat.isEnabled());
        addTextRow(cmd, events, "chat.format", "Format", chat.getFormat());
        addEnumRow(cmd, events, "chat.tagDisplay", "Tag Display", chat.getTagDisplay(),
                List.of("tag", "name", "none"));
        addTextRow(cmd, events, "chat.tagFormat", "Tag Format", chat.getTagFormat());
        addTextRow(cmd, events, "chat.noFactionTag", "No Faction Tag", chat.getNoFactionTag());
        addTextRow(cmd, events, "chat.noFactionTagColor", "No Faction Color", chat.getNoFactionTagColor());
        addEnumRow(cmd, events, "chat.priority", "Event Priority", chat.getPriority(),
                List.of("EARLIEST", "EARLY", "NORMAL", "LATE", "LATEST"));

        addSectionHeader(cmd, "Relation Colors");
        addTextRow(cmd, events, "chat.relationColorOwn", "Own Faction", chat.getRelationColorOwn());
        addTextRow(cmd, events, "chat.relationColorAlly", "Ally", chat.getRelationColorAlly());
        addTextRow(cmd, events, "chat.relationColorNeutral", "Neutral", chat.getRelationColorNeutral());
        addTextRow(cmd, events, "chat.relationColorEnemy", "Enemy", chat.getRelationColorEnemy());

        addSectionHeader(cmd, "Faction Chat");
        addTextRow(cmd, events, "chat.factionChatColor", "Faction Chat Color", chat.getFactionChatColor());
        addTextRow(cmd, events, "chat.factionChatPrefix", "Faction Prefix", chat.getFactionChatPrefix());
        addTextRow(cmd, events, "chat.allyChatColor", "Ally Chat Color", chat.getAllyChatColor());
        addTextRow(cmd, events, "chat.allyChatPrefix", "Ally Prefix", chat.getAllyChatPrefix());
        addTextRow(cmd, events, "chat.senderNameColor", "Sender Name Color", chat.getSenderNameColor());
        addTextRow(cmd, events, "chat.messageColor", "Message Color", chat.getMessageColor());
        addBoolRow(cmd, events, "chat.historyEnabled", "Chat History", chat.isHistoryEnabled());
        addIntRow(cmd, events, "chat.historyMaxMessages", "Max Messages", chat.getHistoryMaxMessages(), 10);
        addIntRow(cmd, events, "chat.historyRetentionDays", "Retention (days)", chat.getHistoryRetentionDays(), 1);
        addIntRow(cmd, events, "chat.historyCleanupIntervalMinutes", "Cleanup Interval (min)", chat.getHistoryCleanupIntervalMinutes(), 5);
    }

    // =========================================================================
    // MODULES TAB
    // =========================================================================

    private void buildModulesTab(UICommandBuilder cmd, UIEventBuilder events) {
        BackupConfig backup = ConfigManager.get().backup();
        AnnouncementConfig announce = ConfigManager.get().announcements();
        GravestoneConfig grave = ConfigManager.get().gravestones();

        addSectionHeader(cmd, "Backups");
        addBoolRow(cmd, events, "backup.enabled", "Enabled", backup.isEnabled());
        addIntRow(cmd, events, "backup.hourlyRetention", "Hourly Retention", backup.getHourlyRetention(), 1);
        addIntRow(cmd, events, "backup.dailyRetention", "Daily Retention", backup.getDailyRetention(), 1);
        addIntRow(cmd, events, "backup.weeklyRetention", "Weekly Retention", backup.getWeeklyRetention(), 1);
        addIntRow(cmd, events, "backup.manualRetention", "Manual Retention", backup.getManualRetention(), 1);
        addBoolRow(cmd, events, "backup.onShutdown", "Backup on Shutdown", backup.isOnShutdown());
        addIntRow(cmd, events, "backup.shutdownRetention", "Shutdown Retention", backup.getShutdownRetention(), 1);

        addSectionHeader(cmd, "Announcements");
        addBoolRow(cmd, events, "announcements.enabled", "Enabled", announce.isEnabled());
        addBoolRow(cmd, events, "announcements.factionCreated", "Faction Created", announce.isFactionCreated());
        addBoolRow(cmd, events, "announcements.factionDisbanded", "Faction Disbanded", announce.isFactionDisbanded());
        addBoolRow(cmd, events, "announcements.leadershipTransfer", "Leadership Transfer", announce.isLeadershipTransfer());
        addBoolRow(cmd, events, "announcements.overclaim", "Overclaim", announce.isOverclaim());
        addBoolRow(cmd, events, "announcements.warDeclared", "War Declared", announce.isWarDeclared());
        addBoolRow(cmd, events, "announcements.allianceFormed", "Alliance Formed", announce.isAllianceFormed());
        addBoolRow(cmd, events, "announcements.allianceBroken", "Alliance Broken", announce.isAllianceBroken());

        addSectionHeader(cmd, "Gravestones");
        addBoolRow(cmd, events, "gravestones.enabled", "Enabled", grave.isEnabled());
        addBoolRow(cmd, events, "gravestones.protectInOwnTerritory", "Protect in Own Territory", grave.isProtectInOwnTerritory());
        addBoolRow(cmd, events, "gravestones.factionMembersCanAccess", "Faction Access", grave.isFactionMembersCanAccess());
        addBoolRow(cmd, events, "gravestones.alliesCanAccess", "Allies Access", grave.isAlliesCanAccess());
        addBoolRow(cmd, events, "gravestones.protectInSafeZone", "Protect in Safe Zone", grave.isProtectInSafeZone());
        addBoolRow(cmd, events, "gravestones.protectInWarZone", "Protect in War Zone", grave.isProtectInWarZone());
        addBoolRow(cmd, events, "gravestones.protectInWilderness", "Protect in Wilderness", grave.isProtectInWilderness());
        addBoolRow(cmd, events, "gravestones.announceDeathLocation", "Announce Death Location", grave.isAnnounceDeathLocation());
    }

    // =========================================================================
    // WORLD MAP TAB
    // =========================================================================

    private void buildWorldMapTab(UICommandBuilder cmd, UIEventBuilder events) {
        WorldMapConfig wm = ConfigManager.get().worldMap();

        addSectionHeader(cmd, "World Map");
        addBoolRow(cmd, events, "worldmap.enabled", "Enabled", wm.isEnabled());
        String currentMode = changeTracker.getEffective("worldmap.refreshMode", wm.getRefreshMode().getConfigName());
        addEnumRow(cmd, events, "worldmap.refreshMode", "Refresh Mode", currentMode,
                List.of("proximity", "incremental", "debounced", "immediate", "manual"));
        addBoolRow(cmd, events, "worldmap.showFactionTags", "Show Faction Tags", wm.isShowFactionTags());
        addBoolRow(cmd, events, "worldmap.autoFallbackOnError", "Auto Fallback on Error", wm.isAutoFallbackOnError());
        addIntRow(cmd, events, "worldmap.factionWideRefreshThreshold", "Faction Refresh Threshold", wm.getFactionWideRefreshThreshold(), 10);

        // Show mode-specific settings based on current selection
        if ("proximity".equals(currentMode)) {
            addSectionHeader(cmd, "Proximity Settings");
            addIntRow(cmd, events, "worldmap.proximityChunkRadius", "Chunk Radius", wm.getProximityChunkRadius(), 4);
            addIntRow(cmd, events, "worldmap.proximityBatchIntervalTicks", "Batch Interval (ticks)", wm.getProximityBatchIntervalTicks(), 5);
            addIntRow(cmd, events, "worldmap.proximityMaxChunksPerBatch", "Max Chunks/Batch", wm.getProximityMaxChunksPerBatch(), 10);
        } else if ("incremental".equals(currentMode)) {
            addSectionHeader(cmd, "Incremental Settings");
            addIntRow(cmd, events, "worldmap.incrementalBatchIntervalTicks", "Batch Interval (ticks)", wm.getIncrementalBatchIntervalTicks(), 5);
            addIntRow(cmd, events, "worldmap.incrementalMaxChunksPerBatch", "Max Chunks/Batch", wm.getIncrementalMaxChunksPerBatch(), 10);
        } else if ("debounced".equals(currentMode)) {
            addSectionHeader(cmd, "Debounced Settings");
            addIntRow(cmd, events, "worldmap.debouncedDelaySeconds", "Delay (sec)", wm.getDebouncedDelaySeconds(), 1);
        }
    }

    // =========================================================================
    // PROTECTION TAB
    // =========================================================================

    private void buildProtectionTab(UICommandBuilder cmd, UIEventBuilder events) {
        FactionPermissionsConfig permCfg = ConfigManager.get().factionPermissions();
        DebugConfig debug = ConfigManager.get().debug();

        // Role selector buttons
        addRoleSelectorBar(cmd, events);

        addSectionHeader(cmd, selectedRole.substring(0, 1).toUpperCase() + selectedRole.substring(1) + " Permissions");
        List<String> flags = FactionPermissions.getFlagsForLevel(selectedRole);
        for (String flag : flags) {
            String suffix = flag.substring(selectedRole.length());
            String display = switch (suffix) {
                case "Break" -> "Break Blocks";
                case "Place" -> "Place Blocks";
                case "Interact" -> "Interact";
                case "DoorUse" -> "Door Use";
                case "ContainerUse" -> "Container Use";
                case "BenchUse" -> "Bench Use";
                case "ProcessingUse" -> "Processing Use";
                case "SeatUse" -> "Seat Use";
                default -> suffix;
            };
            addPermRow(cmd, events, flag, display, permCfg);
        }

        addSectionHeader(cmd, "Global Flags");
        addPermRow(cmd, events, FactionPermissions.PVP_ENABLED, "PvP Enabled", permCfg);
        addPermRow(cmd, events, FactionPermissions.OFFICERS_CAN_EDIT, "Officers Can Edit", permCfg);

        addSectionHeader(cmd, "Mob Spawning");
        addPermRow(cmd, events, FactionPermissions.MOB_SPAWNING, "Mob Spawning", permCfg);
        addPermRow(cmd, events, FactionPermissions.HOSTILE_MOB_SPAWNING, "Hostile Mobs", permCfg);
        addPermRow(cmd, events, FactionPermissions.PASSIVE_MOB_SPAWNING, "Passive Mobs", permCfg);
        addPermRow(cmd, events, FactionPermissions.NEUTRAL_MOB_SPAWNING, "Neutral Mobs", permCfg);

        addSectionHeader(cmd, "Debug Logging");
        addBoolRow(cmd, events, "debug.enabled", "Debug System", debug.isEnabled());
        addBoolRow(cmd, events, "debug.logToConsole", "Log to Console", debug.isLogToConsole());
        addBoolRow(cmd, events, "debug.power", "Power", debug.isPower());
        addBoolRow(cmd, events, "debug.claim", "Claims", debug.isClaim());
        addBoolRow(cmd, events, "debug.combat", "Combat", debug.isCombat());
        addBoolRow(cmd, events, "debug.protection", "Protection", debug.isProtection());
        addBoolRow(cmd, events, "debug.relation", "Relations", debug.isRelation());
        addBoolRow(cmd, events, "debug.territory", "Territory", debug.isTerritory());
        addBoolRow(cmd, events, "debug.worldmap", "World Map", debug.isWorldmap());
        addBoolRow(cmd, events, "debug.interaction", "Interaction", debug.isInteraction());
        addBoolRow(cmd, events, "debug.mixin", "Mixin", debug.isMixin());
        addBoolRow(cmd, events, "debug.spawning", "Spawning", debug.isSpawning());
    }

    // =========================================================================
    // ROW BUILDERS
    // =========================================================================

    private void addSectionHeader(UICommandBuilder cmd, String title) {
        int idx = rowIndex++;
        cmd.append("#TabContent", "HyperFactions/admin/config/section_header.ui");
        cmd.set("#TabContent[" + idx + "] #SectionTitle.Text", title);
    }

    private void addBoolRow(UICommandBuilder cmd, UIEventBuilder events,
                            String key, String label, boolean currentValue) {
        int idx = rowIndex++;
        boolean effective = changeTracker.getEffective(key, currentValue);
        boolean modified = changeTracker.isPending(key);

        String labelColor = modified ? "#FFAA00" : "#cccccc";
        String valueText = effective ? "ON" : "OFF";
        String valueColor = effective ? "#55FF55" : "#FF5555";

        cmd.appendInline("#TabContent",
                "Group { LayoutMode: Left; Anchor: (Height: 24); Padding: (Left: 8, Right: 8); " +
                "TextButton #BoolBtn" + idx + " { Text: \"" + escapeUi(label) + ": " + valueText + "\"; " +
                "Anchor: (Height: 22); FlexWeight: 1; " +
                "Style: (FontSize: 11, TextColor: " + labelColor + ", VerticalAlignment: Center); } }");

        // Style the button text color based on value
        cmd.set("#TabContent[" + idx + "] #BoolBtn" + idx + ".Style",
                Value.ref("HyperFactions/shared/styles.ui", "InvisibleButtonStyle"));

        // We need a label approach instead since invisible buttons don't show colored text well
        // Rewrite as a Group with Label + clickable overlay
        // Actually, let's use a simpler approach with just the invisible button
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TabContent[" + idx + "] #BoolBtn" + idx,
                EventData.of("Button", "ToggleBool").append("Key", key),
                false
        );
    }

    private void addIntRow(UICommandBuilder cmd, UIEventBuilder events,
                           String key, String label, int currentValue, int step) {
        int idx = rowIndex++;
        int effective = changeTracker.getEffective(key, currentValue);
        boolean modified = changeTracker.isPending(key);

        String labelColor = modified ? "#FFAA00" : "#cccccc";
        String valueStr = String.valueOf(effective);

        cmd.appendInline("#TabContent",
                "Group { LayoutMode: Left; Anchor: (Height: 24); Padding: (Left: 8, Right: 8); " +
                "Label { Text: \"" + escapeUi(label) + "\"; " +
                "Style: (FontSize: 11, TextColor: " + labelColor + ", VerticalAlignment: Center); FlexWeight: 1; } " +
                "TextButton #DecBtn" + idx + " { Text: \"-\"; Anchor: (Height: 20, Width: 24); } " +
                "Group { Anchor: (Width: 4); } " +
                "Label #ValLabel" + idx + " { Text: \"" + valueStr + "\"; " +
                "Style: (FontSize: 11, TextColor: #ffffff, VerticalAlignment: Center); Anchor: (Width: 50); } " +
                "Group { Anchor: (Width: 4); } " +
                "TextButton #IncBtn" + idx + " { Text: \"+\"; Anchor: (Height: 20, Width: 24); } }");

        cmd.set("#TabContent[" + idx + "] #DecBtn" + idx + ".Style",
                Value.ref("HyperFactions/shared/styles.ui", "SmallButtonStyle"));
        cmd.set("#TabContent[" + idx + "] #IncBtn" + idx + ".Style",
                Value.ref("HyperFactions/shared/styles.ui", "SmallButtonStyle"));

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TabContent[" + idx + "] #DecBtn" + idx,
                EventData.of("Button", "Decrement").append("Key", key),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TabContent[" + idx + "] #IncBtn" + idx,
                EventData.of("Button", "Increment").append("Key", key),
                false
        );
    }

    private void addDoubleRow(UICommandBuilder cmd, UIEventBuilder events,
                              String key, String label, double currentValue, double step) {
        int idx = rowIndex++;
        double effective = changeTracker.getEffective(key, currentValue);
        boolean modified = changeTracker.isPending(key);

        String labelColor = modified ? "#FFAA00" : "#cccccc";
        String valueStr = formatDouble(effective);

        cmd.appendInline("#TabContent",
                "Group { LayoutMode: Left; Anchor: (Height: 24); Padding: (Left: 8, Right: 8); " +
                "Label { Text: \"" + escapeUi(label) + "\"; " +
                "Style: (FontSize: 11, TextColor: " + labelColor + ", VerticalAlignment: Center); FlexWeight: 1; } " +
                "TextButton #DecBtn" + idx + " { Text: \"-\"; Anchor: (Height: 20, Width: 24); } " +
                "Group { Anchor: (Width: 4); } " +
                "Label #ValLabel" + idx + " { Text: \"" + valueStr + "\"; " +
                "Style: (FontSize: 11, TextColor: #ffffff, VerticalAlignment: Center); Anchor: (Width: 50); } " +
                "Group { Anchor: (Width: 4); } " +
                "TextButton #IncBtn" + idx + " { Text: \"+\"; Anchor: (Height: 20, Width: 24); } }");

        cmd.set("#TabContent[" + idx + "] #DecBtn" + idx + ".Style",
                Value.ref("HyperFactions/shared/styles.ui", "SmallButtonStyle"));
        cmd.set("#TabContent[" + idx + "] #IncBtn" + idx + ".Style",
                Value.ref("HyperFactions/shared/styles.ui", "SmallButtonStyle"));

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TabContent[" + idx + "] #DecBtn" + idx,
                EventData.of("Button", "Decrement").append("Key", key),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TabContent[" + idx + "] #IncBtn" + idx,
                EventData.of("Button", "Increment").append("Key", key),
                false
        );
    }

    private void addTextRow(UICommandBuilder cmd, UIEventBuilder events,
                            String key, String label, String currentValue) {
        int idx = rowIndex++;
        String effective = changeTracker.getEffective(key, currentValue);
        boolean modified = changeTracker.isPending(key);

        String labelColor = modified ? "#FFAA00" : "#cccccc";

        cmd.appendInline("#TabContent",
                "Group { LayoutMode: Left; Anchor: (Height: 26); Padding: (Left: 8, Right: 8); " +
                "Label { Text: \"" + escapeUi(label) + "\"; " +
                "Style: (FontSize: 11, TextColor: " + labelColor + ", VerticalAlignment: Center); Anchor: (Width: 180); } " +
                "Group { Background: (Color: #0d1520); Padding: (Left: 4, Right: 4); FlexWeight: 1; " +
                "TextField #TF" + idx + " { " +
                "Anchor: (Height: 22); " +
                "Style: (FontSize: 11, TextColor: #ffffff); } } }");

        cmd.set("#TabContent[" + idx + "] #TF" + idx + ".Value", effective);

        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#TabContent[" + idx + "] #TF" + idx,
                EventData.of("Button", "UpdateText")
                        .append("Key", key)
                        .append("@Value", "#TabContent[" + idx + "] #TF" + idx + ".Value"),
                false
        );
    }

    private void addEnumRow(UICommandBuilder cmd, UIEventBuilder events,
                            String key, String label, String currentValue,
                            List<String> options) {
        int idx = rowIndex++;
        String effective = changeTracker.getEffective(key, currentValue);
        boolean modified = changeTracker.isPending(key);

        String labelColor = modified ? "#FFAA00" : "#cccccc";

        cmd.appendInline("#TabContent",
                "Group { LayoutMode: Left; Anchor: (Height: 28); Padding: (Left: 8, Right: 8); " +
                "Label { Text: \"" + escapeUi(label) + "\"; " +
                "Style: (FontSize: 11, TextColor: " + labelColor + ", VerticalAlignment: Center); Anchor: (Width: 180); } " +
                "Group { Background: (Color: #0d1520); Padding: (Left: 6, Right: 6); FlexWeight: 1; " +
                "DropdownBox #DD" + idx + " { Anchor: (Height: 24); } } }");

        // Set dropdown entries
        List<DropdownEntryInfo> entries = options.stream()
                .map(o -> new DropdownEntryInfo(
                        LocalizableString.fromString(o), o))
                .collect(Collectors.toList());
        cmd.set("#TabContent[" + idx + "] #DD" + idx + ".Entries", entries);
        cmd.set("#TabContent[" + idx + "] #DD" + idx + ".Value", effective);

        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#TabContent[" + idx + "] #DD" + idx,
                EventData.of("Button", "UpdateEnum")
                        .append("Key", key)
                        .append("@Value", "#TabContent[" + idx + "] #DD" + idx + ".Value"),
                false
        );
    }

    private void addPermRow(UICommandBuilder cmd, UIEventBuilder events,
                            String flag, String display, FactionPermissionsConfig permCfg) {
        int idx = rowIndex++;
        boolean defaultVal = getEffectivePermDefault(flag);
        boolean locked = getEffectivePermLock(flag);

        String defaultColor = defaultVal ? "#55FF55" : "#FF5555";
        String defaultText = defaultVal ? "ALLOW" : "DENY";
        String lockColor = locked ? "#FF5555" : "#555555";
        String lockText = locked ? "LOCKED" : "unlocked";

        cmd.appendInline("#TabContent",
                "Group { LayoutMode: Left; Anchor: (Height: 24); Padding: (Left: 8, Right: 8); " +
                "Label { Text: \"" + escapeUi(display) + "\"; " +
                "Style: (FontSize: 11, TextColor: #cccccc, VerticalAlignment: Center); FlexWeight: 1; } " +
                "TextButton #DefBtn" + idx + " { Text: \"" + defaultText + "\"; " +
                "Anchor: (Height: 20, Width: 65); } " +
                "Group { Anchor: (Width: 6); } " +
                "TextButton #LockBtn" + idx + " { Text: \"" + lockText + "\"; " +
                "Anchor: (Height: 20, Width: 80); } }");

        cmd.set("#TabContent[" + idx + "] #DefBtn" + idx + ".Style",
                Value.ref("HyperFactions/shared/styles.ui",
                        defaultVal ? "GreenButtonStyle" : "RedButtonStyle"));
        cmd.set("#TabContent[" + idx + "] #LockBtn" + idx + ".Style",
                Value.ref("HyperFactions/shared/styles.ui",
                        locked ? "RedButtonStyle" : "ButtonStyle"));

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TabContent[" + idx + "] #DefBtn" + idx,
                EventData.of("Button", "TogglePermDefault").append("Key", flag),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TabContent[" + idx + "] #LockBtn" + idx,
                EventData.of("Button", "TogglePermLock").append("Key", flag),
                false
        );
    }

    private void addRoleSelectorBar(UICommandBuilder cmd, UIEventBuilder events) {
        int idx = rowIndex++;

        StringBuilder sb = new StringBuilder();
        sb.append("Group { LayoutMode: Left; Anchor: (Height: 30); Padding: (Left: 8, Right: 8, Bottom: 4); ");
        for (String role : FactionPermissions.ALL_LEVELS) {
            String display = role.substring(0, 1).toUpperCase() + role.substring(1);
            sb.append("TextButton #Role_").append(role).append(idx).append(" { Text: \"").append(display)
              .append("\"; Anchor: (Height: 26, Width: 90); } ");
            sb.append("Group { Anchor: (Width: 4); } ");
        }
        sb.append("}");

        cmd.appendInline("#TabContent", sb.toString());

        for (String role : FactionPermissions.ALL_LEVELS) {
            String btnId = "#TabContent[" + idx + "] #Role_" + role + idx;
            boolean isActive = role.equals(selectedRole);

            cmd.set(btnId + ".Style", Value.ref("HyperFactions/shared/styles.ui",
                    isActive ? "CyanButtonStyle" : "ButtonStyle"));

            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    btnId,
                    EventData.of("Button", "SelectRole").append("Role", role),
                    false
            );
        }
    }

    private void addWorldListSection(UICommandBuilder cmd, UIEventBuilder events,
                                     String key, List<String> currentList) {
        @SuppressWarnings("unchecked")
        List<String> effective = changeTracker.getEffective(key, currentList);

        // Display current entries
        for (int i = 0; i < effective.size(); i++) {
            String world = effective.get(i);
            int idx = rowIndex++;

            cmd.append("#TabContent", "HyperFactions/admin/config/list_entry.ui");
            cmd.set("#TabContent[" + idx + "] #EntryName.Text", world);

            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#TabContent[" + idx + "] #RemoveBtn",
                    EventData.of("Button", "RemoveListEntry")
                            .append("Key", key)
                            .append("ListItem", world),
                    false
            );
        }

        // Add entry row with dropdown + Add button
        int addIdx = rowIndex++;
        List<String> available = getAvailableWorlds(effective);

        if (!available.isEmpty()) {
            cmd.appendInline("#TabContent",
                    "Group { LayoutMode: Left; Anchor: (Height: 28); Padding: (Left: 8, Right: 8); " +
                    "Group { Background: (Color: #0d1520); Padding: (Left: 6, Right: 6); FlexWeight: 1; " +
                    "DropdownBox #AddDD" + addIdx + " { Anchor: (Height: 24); } } " +
                    "Group { Anchor: (Width: 6); } " +
                    "TextButton #AddBtn" + addIdx + " { Text: \"Add\"; Anchor: (Height: 24, Width: 50); } }");

            List<DropdownEntryInfo> entries = available.stream()
                    .map(w -> new DropdownEntryInfo(
                            LocalizableString.fromString(w), w))
                    .collect(Collectors.toList());
            cmd.set("#TabContent[" + addIdx + "] #AddDD" + addIdx + ".Entries", entries);
            if (!available.isEmpty()) {
                cmd.set("#TabContent[" + addIdx + "] #AddDD" + addIdx + ".Value", available.getFirst());
            }

            cmd.set("#TabContent[" + addIdx + "] #AddBtn" + addIdx + ".Style",
                    Value.ref("HyperFactions/shared/styles.ui", "GreenButtonStyle"));

            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#TabContent[" + addIdx + "] #AddBtn" + addIdx,
                    EventData.of("Button", "AddListEntry")
                            .append("Key", key)
                            .append("@Value", "#TabContent[" + addIdx + "] #AddDD" + addIdx + ".Value"),
                    false
            );
        } else {
            cmd.appendInline("#TabContent",
                    "Group { Anchor: (Height: 24); Padding: (Left: 8); " +
                    "Label { Text: \"(no more worlds available)\"; " +
                    "Style: (FontSize: 10, TextColor: #555555, VerticalAlignment: Center); FlexWeight: 1; } }");
        }
    }

    // =========================================================================
    // FOOTER
    // =========================================================================

    private void buildFooter(UICommandBuilder cmd, UIEventBuilder events) {
        if (changeTracker.hasPendingChanges()) {
            cmd.set("#DirtyIndicator.Text", "Unsaved changes (" + changeTracker.getPendingChanges().size() + ")");
        } else {
            cmd.set("#DirtyIndicator.Text", "");
        }

        if (confirmReset) {
            cmd.set("#ResetBtn.Text", "Confirm?");
            cmd.set("#ResetBtn.Style", Value.ref("HyperFactions/shared/styles.ui", "FlatRedButtonStyle"));

            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#ResetBtn",
                    EventData.of("Button", "ConfirmReset"),
                    false
            );
        } else {
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#ResetBtn",
                    EventData.of("Button", "ResetDefaults"),
                    false
            );
        }

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#SaveBtn",
                EventData.of("Button", "Save"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#RevertBtn",
                EventData.of("Button", "Revert"),
                false
        );
    }

    // =========================================================================
    // INCREMENT / DECREMENT
    // =========================================================================

    private void handleIncrement(String key) {
        Object current = getCurrentConfigValue(key);
        if (current instanceof Integer i) {
            int step = getIntStep(key);
            changeTracker.set(key, changeTracker.getEffective(key, i) + step);
        } else if (current instanceof Double d) {
            double step = getDoubleStep(key);
            double val = changeTracker.getEffective(key, d) + step;
            changeTracker.set(key, Math.round(val * 100.0) / 100.0);
        }
        sendUpdate();
    }

    private void handleDecrement(String key) {
        Object current = getCurrentConfigValue(key);
        if (current instanceof Integer i) {
            int step = getIntStep(key);
            int val = changeTracker.getEffective(key, i) - step;
            changeTracker.set(key, val);
        } else if (current instanceof Double d) {
            double step = getDoubleStep(key);
            double val = changeTracker.getEffective(key, d) - step;
            changeTracker.set(key, Math.round(val * 100.0) / 100.0);
        }
        sendUpdate();
    }

    private int getIntStep(String key) {
        return switch (key) {
            case "claims.maxClaims" -> 5;
            case "teleport.cooldownSeconds", "stuck.cooldownSeconds" -> 5;
            case "stuck.warmupSeconds" -> 5;
            case "chat.historyMaxMessages" -> 10;
            case "chat.historyCleanupIntervalMinutes" -> 5;
            case "worldmap.factionWideRefreshThreshold" -> 10;
            case "worldmap.proximityChunkRadius" -> 4;
            case "worldmap.proximityBatchIntervalTicks", "worldmap.incrementalBatchIntervalTicks" -> 5;
            case "worldmap.proximityMaxChunksPerBatch", "worldmap.incrementalMaxChunksPerBatch" -> 10;
            default -> 1;
        };
    }

    private double getDoubleStep(String key) {
        return switch (key) {
            case "power.maxPlayerPower", "power.startingPower" -> 1.0;
            case "power.powerPerClaim", "power.deathPenalty", "power.killReward",
                 "combat.logoutPowerLoss", "combat.neutralAttackPenalty" -> 0.5;
            case "power.regenPerMinute" -> 0.05;
            case "economy.startingBalance" -> 10.0;
            default -> 1.0;
        };
    }

    // =========================================================================
    // LIST OPERATIONS
    // =========================================================================

    private void handleAddListEntry(String key, String value) {
        List<String> currentList = getCurrentListValue(key);
        @SuppressWarnings("unchecked")
        List<String> effective = new ArrayList<>(changeTracker.getEffective(key, currentList));
        if (!effective.contains(value)) {
            effective.add(value);
            changeTracker.set(key, effective);
        }
    }

    private void handleRemoveListEntry(String key, String item) {
        List<String> currentList = getCurrentListValue(key);
        @SuppressWarnings("unchecked")
        List<String> effective = new ArrayList<>(changeTracker.getEffective(key, currentList));
        effective.remove(item);
        changeTracker.set(key, effective);
    }

    @SuppressWarnings("unchecked")
    private List<String> getCurrentListValue(String key) {
        CoreConfig core = ConfigManager.get().core();
        return switch (key) {
            case "claims.worldWhitelist" -> core.getWorldWhitelist();
            case "claims.worldBlacklist" -> core.getWorldBlacklist();
            default -> List.of();
        };
    }

    private List<String> getAvailableWorlds(List<String> exclude) {
        try {
            Universe universe = Universe.get();
            return universe.getWorlds().values().stream()
                    .map(w -> w.getName())
                    .filter(name -> !exclude.contains(name))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }

    // =========================================================================
    // SAVE / RESET
    // =========================================================================

    private void handleSave(Player player, Ref<EntityStore> ref,
                            Store<EntityStore> store, PlayerRef playerRef) {
        if (!changeTracker.hasPendingChanges()) {
            player.sendMessage(Message.raw("No changes to save.").color("#888888"));
            return;
        }

        List<String> errors = new ArrayList<>();
        applyPendingChanges(errors);

        if (!errors.isEmpty()) {
            player.sendMessage(Message.raw("Validation errors:").color("#FF5555"));
            for (String error : errors) {
                player.sendMessage(Message.raw("  - " + error).color("#FFAA00"));
            }
            return;
        }

        ConfigManager.get().saveAll();
        changeTracker.clear();
        confirmReset = false;
        sendUpdate();
        player.sendMessage(Message.raw("Configuration saved successfully.").color("#55FF55"));
    }

    private void handleConfirmReset(Player player, Ref<EntityStore> ref,
                                    Store<EntityStore> store, PlayerRef playerRef) {
        ConfigManager.get().reloadAll();
        changeTracker.clear();
        confirmReset = false;
        sendUpdate();
        player.sendMessage(Message.raw("Configuration reset to defaults. Use /f reload to apply.").color("#FFAA00"));
    }

    @SuppressWarnings("unchecked")
    private void applyPendingChanges(List<String> errors) {
        ConfigManager cfg = ConfigManager.get();
        CoreConfig core = cfg.core();
        ChatConfig chat = cfg.chat();
        BackupConfig backup = cfg.backup();
        EconomyConfig econ = cfg.economy();
        AnnouncementConfig announce = cfg.announcements();
        GravestoneConfig grave = cfg.gravestones();
        WorldMapConfig wm = cfg.worldMap();
        DebugConfig debug = cfg.debug();
        FactionPermissionsConfig perms = cfg.factionPermissions();

        for (var entry : changeTracker.getPendingChanges().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            try {
                // Permission defaults/locks
                if (key.startsWith("perm.default.")) {
                    String flag = key.substring("perm.default.".length());
                    perms.setDefault(flag, (Boolean) value);
                    continue;
                }
                if (key.startsWith("perm.lock.")) {
                    String flag = key.substring("perm.lock.".length());
                    perms.setLock(flag, (Boolean) value);
                    continue;
                }

                switch (key) {
                    // CoreConfig: Faction
                    case "faction.maxMembers" -> core.setMaxMembers(toInt(value));
                    case "faction.maxNameLength" -> core.setMaxNameLength(toInt(value));
                    case "faction.minNameLength" -> core.setMinNameLength(toInt(value));
                    case "faction.allowColors" -> core.setAllowColors((Boolean) value);

                    // CoreConfig: Power
                    case "power.maxPlayerPower" -> core.setMaxPlayerPower(toDouble(value));
                    case "power.startingPower" -> core.setStartingPower(toDouble(value));
                    case "power.powerPerClaim" -> core.setPowerPerClaim(toDouble(value));
                    case "power.deathPenalty" -> core.setDeathPenalty(toDouble(value));
                    case "power.killReward" -> core.setKillReward(toDouble(value));
                    case "power.regenPerMinute" -> core.setRegenPerMinute(toDouble(value));
                    case "power.regenWhenOffline" -> core.setRegenWhenOffline((Boolean) value);

                    // CoreConfig: Claims
                    case "claims.maxClaims" -> core.setMaxClaims(toInt(value));
                    case "claims.onlyAdjacent" -> core.setOnlyAdjacent((Boolean) value);
                    case "claims.preventDisconnect" -> core.setPreventDisconnect((Boolean) value);
                    case "claims.decayEnabled" -> core.setDecayEnabled((Boolean) value);
                    case "claims.decayDaysInactive" -> core.setDecayDaysInactive(toInt(value));
                    case "claims.worldWhitelist" -> core.setWorldWhitelist((List<String>) value);
                    case "claims.worldBlacklist" -> core.setWorldBlacklist((List<String>) value);

                    // CoreConfig: Combat
                    case "combat.tagDurationSeconds" -> core.setTagDurationSeconds(toInt(value));
                    case "combat.allyDamage" -> core.setAllyDamage((Boolean) value);
                    case "combat.factionDamage" -> core.setFactionDamage((Boolean) value);
                    case "combat.taggedLogoutPenalty" -> core.setTaggedLogoutPenalty((Boolean) value);
                    case "combat.logoutPowerLoss" -> core.setLogoutPowerLoss(toDouble(value));
                    case "combat.neutralAttackPenalty" -> core.setNeutralAttackPenalty(toDouble(value));
                    case "combat.spawnProtection.enabled" -> core.setSpawnProtectionEnabled((Boolean) value);
                    case "combat.spawnProtection.durationSeconds" -> core.setSpawnProtectionDurationSeconds(toInt(value));
                    case "combat.spawnProtection.breakOnAttack" -> core.setSpawnProtectionBreakOnAttack((Boolean) value);
                    case "combat.spawnProtection.breakOnMove" -> core.setSpawnProtectionBreakOnMove((Boolean) value);

                    // CoreConfig: Relations
                    case "relations.maxAllies" -> core.setMaxAllies(toInt(value));
                    case "relations.maxEnemies" -> core.setMaxEnemies(toInt(value));

                    // CoreConfig: Invites
                    case "invites.inviteExpirationMinutes" -> core.setInviteExpirationMinutes(toInt(value));
                    case "invites.joinRequestExpirationHours" -> core.setJoinRequestExpirationHours(toInt(value));

                    // CoreConfig: Stuck
                    case "stuck.warmupSeconds" -> core.setStuckWarmupSeconds(toInt(value));
                    case "stuck.cooldownSeconds" -> core.setStuckCooldownSeconds(toInt(value));

                    // CoreConfig: Teleport
                    case "teleport.warmupSeconds" -> core.setWarmupSeconds(toInt(value));
                    case "teleport.cooldownSeconds" -> core.setCooldownSeconds(toInt(value));
                    case "teleport.cancelOnMove" -> core.setCancelOnMove((Boolean) value);
                    case "teleport.cancelOnDamage" -> core.setCancelOnDamage((Boolean) value);

                    // CoreConfig: Updates
                    case "updates.enabled" -> core.setUpdateCheckEnabled((Boolean) value);
                    case "updates.releaseChannel" -> core.setReleaseChannel((String) value);

                    // CoreConfig: Auto-save
                    case "autoSave.enabled" -> core.setAutoSaveEnabled((Boolean) value);
                    case "autoSave.intervalMinutes" -> core.setAutoSaveIntervalMinutes(toInt(value));

                    // CoreConfig: Messages
                    case "messages.prefixText" -> core.setPrefixText((String) value);
                    case "messages.prefixColor" -> core.setPrefixColor((String) value);
                    case "messages.prefixBracketColor" -> core.setPrefixBracketColor((String) value);
                    case "messages.primaryColor" -> core.setPrimaryColor((String) value);

                    // CoreConfig: GUI
                    case "gui.title" -> core.setGuiTitle((String) value);

                    // CoreConfig: Territory
                    case "territoryNotifications.enabled" -> core.setTerritoryNotificationsEnabled((Boolean) value);

                    // CoreConfig: Permissions
                    case "permissions.adminRequiresOp" -> core.setAdminRequiresOp((Boolean) value);
                    case "permissions.allowWithoutPermissionMod" -> core.setAllowWithoutPermissionMod((Boolean) value);

                    // ChatConfig
                    case "chat.enabled" -> chat.setEnabled((Boolean) value);
                    case "chat.format" -> chat.setFormat((String) value);
                    case "chat.tagDisplay" -> chat.setTagDisplay((String) value);
                    case "chat.tagFormat" -> chat.setTagFormat((String) value);
                    case "chat.noFactionTag" -> chat.setNoFactionTag((String) value);
                    case "chat.noFactionTagColor" -> chat.setNoFactionTagColor((String) value);
                    case "chat.priority" -> chat.setPriority((String) value);
                    case "chat.relationColorOwn" -> chat.setRelationColorOwn((String) value);
                    case "chat.relationColorAlly" -> chat.setRelationColorAlly((String) value);
                    case "chat.relationColorNeutral" -> chat.setRelationColorNeutral((String) value);
                    case "chat.relationColorEnemy" -> chat.setRelationColorEnemy((String) value);
                    case "chat.factionChatColor" -> chat.setFactionChatColor((String) value);
                    case "chat.factionChatPrefix" -> chat.setFactionChatPrefix((String) value);
                    case "chat.allyChatColor" -> chat.setAllyChatColor((String) value);
                    case "chat.allyChatPrefix" -> chat.setAllyChatPrefix((String) value);
                    case "chat.senderNameColor" -> chat.setSenderNameColor((String) value);
                    case "chat.messageColor" -> chat.setMessageColor((String) value);
                    case "chat.historyEnabled" -> chat.setHistoryEnabled((Boolean) value);
                    case "chat.historyMaxMessages" -> chat.setHistoryMaxMessages(toInt(value));
                    case "chat.historyRetentionDays" -> chat.setHistoryRetentionDays(toInt(value));
                    case "chat.historyCleanupIntervalMinutes" -> chat.setHistoryCleanupIntervalMinutes(toInt(value));

                    // BackupConfig
                    case "backup.enabled" -> backup.setEnabled((Boolean) value);
                    case "backup.hourlyRetention" -> backup.setHourlyRetention(toInt(value));
                    case "backup.dailyRetention" -> backup.setDailyRetention(toInt(value));
                    case "backup.weeklyRetention" -> backup.setWeeklyRetention(toInt(value));
                    case "backup.manualRetention" -> backup.setManualRetention(toInt(value));
                    case "backup.onShutdown" -> backup.setOnShutdown((Boolean) value);
                    case "backup.shutdownRetention" -> backup.setShutdownRetention(toInt(value));

                    // EconomyConfig
                    case "economy.enabled" -> econ.setEnabled((Boolean) value);
                    case "economy.currencyName" -> econ.setCurrencyName((String) value);
                    case "economy.currencyNamePlural" -> econ.setCurrencyNamePlural((String) value);
                    case "economy.currencySymbol" -> econ.setCurrencySymbol((String) value);
                    case "economy.startingBalance" -> econ.setStartingBalance(toDouble(value));

                    // AnnouncementConfig
                    case "announcements.enabled" -> announce.setEnabled((Boolean) value);
                    case "announcements.factionCreated" -> announce.setFactionCreated((Boolean) value);
                    case "announcements.factionDisbanded" -> announce.setFactionDisbanded((Boolean) value);
                    case "announcements.leadershipTransfer" -> announce.setLeadershipTransfer((Boolean) value);
                    case "announcements.overclaim" -> announce.setOverclaim((Boolean) value);
                    case "announcements.warDeclared" -> announce.setWarDeclared((Boolean) value);
                    case "announcements.allianceFormed" -> announce.setAllianceFormed((Boolean) value);
                    case "announcements.allianceBroken" -> announce.setAllianceBroken((Boolean) value);

                    // GravestoneConfig
                    case "gravestones.enabled" -> grave.setEnabled((Boolean) value);
                    case "gravestones.protectInOwnTerritory" -> grave.setProtectInOwnTerritory((Boolean) value);
                    case "gravestones.factionMembersCanAccess" -> grave.setFactionMembersCanAccess((Boolean) value);
                    case "gravestones.alliesCanAccess" -> grave.setAlliesCanAccess((Boolean) value);
                    case "gravestones.protectInSafeZone" -> grave.setProtectInSafeZone((Boolean) value);
                    case "gravestones.protectInWarZone" -> grave.setProtectInWarZone((Boolean) value);
                    case "gravestones.protectInWilderness" -> grave.setProtectInWilderness((Boolean) value);
                    case "gravestones.announceDeathLocation" -> grave.setAnnounceDeathLocation((Boolean) value);

                    // WorldMapConfig
                    case "worldmap.enabled" -> wm.setEnabled((Boolean) value);
                    case "worldmap.refreshMode" -> wm.setRefreshMode(WorldMapConfig.RefreshMode.fromString((String) value));
                    case "worldmap.showFactionTags" -> wm.setShowFactionTags((Boolean) value);
                    case "worldmap.autoFallbackOnError" -> wm.setAutoFallbackOnError((Boolean) value);
                    case "worldmap.factionWideRefreshThreshold" -> wm.setFactionWideRefreshThreshold(toInt(value));
                    case "worldmap.proximityChunkRadius" -> wm.setProximityChunkRadius(toInt(value));
                    case "worldmap.proximityBatchIntervalTicks" -> wm.setProximityBatchIntervalTicks(toInt(value));
                    case "worldmap.proximityMaxChunksPerBatch" -> wm.setProximityMaxChunksPerBatch(toInt(value));
                    case "worldmap.incrementalBatchIntervalTicks" -> wm.setIncrementalBatchIntervalTicks(toInt(value));
                    case "worldmap.incrementalMaxChunksPerBatch" -> wm.setIncrementalMaxChunksPerBatch(toInt(value));
                    case "worldmap.debouncedDelaySeconds" -> wm.setDebouncedDelaySeconds(toInt(value));

                    // DebugConfig
                    case "debug.enabled" -> { debug.setEnabled((Boolean) value); }
                    case "debug.logToConsole" -> debug.setLogToConsole((Boolean) value);
                    case "debug.power" -> debug.setPower((Boolean) value);
                    case "debug.claim" -> debug.setClaim((Boolean) value);
                    case "debug.combat" -> debug.setCombat((Boolean) value);
                    case "debug.protection" -> debug.setProtection((Boolean) value);
                    case "debug.relation" -> debug.setRelation((Boolean) value);
                    case "debug.territory" -> debug.setTerritory((Boolean) value);
                    case "debug.worldmap" -> debug.setWorldmap((Boolean) value);
                    case "debug.interaction" -> debug.setInteraction((Boolean) value);
                    case "debug.mixin" -> debug.setMixin((Boolean) value);
                    case "debug.spawning" -> debug.setSpawning((Boolean) value);

                    default -> errors.add("Unknown config key: " + key);
                }
            } catch (ClassCastException | NumberFormatException e) {
                errors.add(key + ": invalid value type");
            }
        }
    }

    // =========================================================================
    // VALUE HELPERS
    // =========================================================================

    private boolean getEffectiveBool(String key) {
        Object current = getCurrentConfigValue(key);
        if (current instanceof Boolean b) {
            return changeTracker.getEffective(key, b);
        }
        return false;
    }

    private boolean getEffectivePermDefault(String flag) {
        FactionPermissionsConfig perms = ConfigManager.get().factionPermissions();
        boolean current = perms.getDefaults().getOrDefault(flag, false);
        return changeTracker.getEffective("perm.default." + flag, current);
    }

    private boolean getEffectivePermLock(String flag) {
        FactionPermissionsConfig perms = ConfigManager.get().factionPermissions();
        boolean current = perms.getLocks().getOrDefault(flag, false);
        return changeTracker.getEffective("perm.lock." + flag, current);
    }

    private Object getCurrentConfigValue(String key) {
        ConfigManager cfg = ConfigManager.get();
        CoreConfig core = cfg.core();

        return switch (key) {
            // Faction
            case "faction.maxMembers" -> core.getMaxMembers();
            case "faction.maxNameLength" -> core.getMaxNameLength();
            case "faction.minNameLength" -> core.getMinNameLength();
            case "faction.allowColors" -> core.isAllowColors();
            // Power
            case "power.maxPlayerPower" -> core.getMaxPlayerPower();
            case "power.startingPower" -> core.getStartingPower();
            case "power.powerPerClaim" -> core.getPowerPerClaim();
            case "power.deathPenalty" -> core.getDeathPenalty();
            case "power.killReward" -> core.getKillReward();
            case "power.regenPerMinute" -> core.getRegenPerMinute();
            case "power.regenWhenOffline" -> core.isRegenWhenOffline();
            // Claims
            case "claims.maxClaims" -> core.getMaxClaims();
            case "claims.onlyAdjacent" -> core.isOnlyAdjacent();
            case "claims.preventDisconnect" -> core.isPreventDisconnect();
            case "claims.decayEnabled" -> core.isDecayEnabled();
            case "claims.decayDaysInactive" -> core.getDecayDaysInactive();
            // Combat
            case "combat.tagDurationSeconds" -> core.getTagDurationSeconds();
            case "combat.allyDamage" -> core.isAllyDamage();
            case "combat.factionDamage" -> core.isFactionDamage();
            case "combat.taggedLogoutPenalty" -> core.isTaggedLogoutPenalty();
            case "combat.logoutPowerLoss" -> core.getLogoutPowerLoss();
            case "combat.neutralAttackPenalty" -> core.getNeutralAttackPenalty();
            case "combat.spawnProtection.enabled" -> core.isSpawnProtectionEnabled();
            case "combat.spawnProtection.durationSeconds" -> core.getSpawnProtectionDurationSeconds();
            case "combat.spawnProtection.breakOnAttack" -> core.isSpawnProtectionBreakOnAttack();
            case "combat.spawnProtection.breakOnMove" -> core.isSpawnProtectionBreakOnMove();
            // Relations
            case "relations.maxAllies" -> core.getMaxAllies();
            case "relations.maxEnemies" -> core.getMaxEnemies();
            // Invites
            case "invites.inviteExpirationMinutes" -> core.getInviteExpirationMinutes();
            case "invites.joinRequestExpirationHours" -> core.getJoinRequestExpirationHours();
            // Stuck
            case "stuck.warmupSeconds" -> core.getStuckWarmupSeconds();
            case "stuck.cooldownSeconds" -> core.getStuckCooldownSeconds();
            // Teleport
            case "teleport.warmupSeconds" -> core.getWarmupSeconds();
            case "teleport.cooldownSeconds" -> core.getCooldownSeconds();
            case "teleport.cancelOnMove" -> core.isCancelOnMove();
            case "teleport.cancelOnDamage" -> core.isCancelOnDamage();
            // Updates
            case "updates.enabled" -> core.isUpdateCheckEnabled();
            case "updates.releaseChannel" -> core.getReleaseChannel();
            // Auto-save
            case "autoSave.enabled" -> core.isAutoSaveEnabled();
            case "autoSave.intervalMinutes" -> core.getAutoSaveIntervalMinutes();
            // Messages
            case "messages.prefixText" -> core.getPrefixText();
            case "messages.prefixColor" -> core.getPrefixColor();
            case "messages.prefixBracketColor" -> core.getPrefixBracketColor();
            case "messages.primaryColor" -> core.getPrimaryColor();
            // GUI
            case "gui.title" -> core.getGuiTitle();
            // Territory
            case "territoryNotifications.enabled" -> core.isTerritoryNotificationsEnabled();
            // Permissions
            case "permissions.adminRequiresOp" -> core.isAdminRequiresOp();
            case "permissions.allowWithoutPermissionMod" -> core.isAllowWithoutPermissionMod();
            // Chat
            case "chat.enabled" -> cfg.chat().isEnabled();
            case "chat.historyEnabled" -> cfg.chat().isHistoryEnabled();
            case "chat.historyMaxMessages" -> cfg.chat().getHistoryMaxMessages();
            case "chat.historyRetentionDays" -> cfg.chat().getHistoryRetentionDays();
            case "chat.historyCleanupIntervalMinutes" -> cfg.chat().getHistoryCleanupIntervalMinutes();
            // Backup
            case "backup.enabled" -> cfg.backup().isEnabled();
            case "backup.hourlyRetention" -> cfg.backup().getHourlyRetention();
            case "backup.dailyRetention" -> cfg.backup().getDailyRetention();
            case "backup.weeklyRetention" -> cfg.backup().getWeeklyRetention();
            case "backup.manualRetention" -> cfg.backup().getManualRetention();
            case "backup.onShutdown" -> cfg.backup().isOnShutdown();
            case "backup.shutdownRetention" -> cfg.backup().getShutdownRetention();
            // Economy
            case "economy.enabled" -> cfg.economy().isEnabled();
            case "economy.startingBalance" -> cfg.economy().getStartingBalance();
            // Announcements
            case "announcements.enabled" -> cfg.announcements().isEnabled();
            case "announcements.factionCreated" -> cfg.announcements().isFactionCreated();
            case "announcements.factionDisbanded" -> cfg.announcements().isFactionDisbanded();
            case "announcements.leadershipTransfer" -> cfg.announcements().isLeadershipTransfer();
            case "announcements.overclaim" -> cfg.announcements().isOverclaim();
            case "announcements.warDeclared" -> cfg.announcements().isWarDeclared();
            case "announcements.allianceFormed" -> cfg.announcements().isAllianceFormed();
            case "announcements.allianceBroken" -> cfg.announcements().isAllianceBroken();
            // Gravestones
            case "gravestones.enabled" -> cfg.gravestones().isEnabled();
            case "gravestones.protectInOwnTerritory" -> cfg.gravestones().isProtectInOwnTerritory();
            case "gravestones.factionMembersCanAccess" -> cfg.gravestones().isFactionMembersCanAccess();
            case "gravestones.alliesCanAccess" -> cfg.gravestones().isAlliesCanAccess();
            case "gravestones.protectInSafeZone" -> cfg.gravestones().isProtectInSafeZone();
            case "gravestones.protectInWarZone" -> cfg.gravestones().isProtectInWarZone();
            case "gravestones.protectInWilderness" -> cfg.gravestones().isProtectInWilderness();
            case "gravestones.announceDeathLocation" -> cfg.gravestones().isAnnounceDeathLocation();
            // Worldmap
            case "worldmap.enabled" -> cfg.worldMap().isEnabled();
            case "worldmap.showFactionTags" -> cfg.worldMap().isShowFactionTags();
            case "worldmap.autoFallbackOnError" -> cfg.worldMap().isAutoFallbackOnError();
            case "worldmap.factionWideRefreshThreshold" -> cfg.worldMap().getFactionWideRefreshThreshold();
            case "worldmap.proximityChunkRadius" -> cfg.worldMap().getProximityChunkRadius();
            case "worldmap.proximityBatchIntervalTicks" -> cfg.worldMap().getProximityBatchIntervalTicks();
            case "worldmap.proximityMaxChunksPerBatch" -> cfg.worldMap().getProximityMaxChunksPerBatch();
            case "worldmap.incrementalBatchIntervalTicks" -> cfg.worldMap().getIncrementalBatchIntervalTicks();
            case "worldmap.incrementalMaxChunksPerBatch" -> cfg.worldMap().getIncrementalMaxChunksPerBatch();
            case "worldmap.debouncedDelaySeconds" -> cfg.worldMap().getDebouncedDelaySeconds();
            // Debug
            case "debug.enabled" -> cfg.debug().isEnabled();
            case "debug.logToConsole" -> cfg.debug().isLogToConsole();
            case "debug.power" -> cfg.debug().isPower();
            case "debug.claim" -> cfg.debug().isClaim();
            case "debug.combat" -> cfg.debug().isCombat();
            case "debug.protection" -> cfg.debug().isProtection();
            case "debug.relation" -> cfg.debug().isRelation();
            case "debug.territory" -> cfg.debug().isTerritory();
            case "debug.worldmap" -> cfg.debug().isWorldmap();
            case "debug.interaction" -> cfg.debug().isInteraction();
            case "debug.mixin" -> cfg.debug().isMixin();
            case "debug.spawning" -> cfg.debug().isSpawning();
            default -> null;
        };
    }

    // =========================================================================
    // UTILITY
    // =========================================================================

    private static String escapeUi(String text) {
        // Escape characters that could cause .ui parsing issues
        return text.replace("\"", "'").replace("$", "");
    }

    private static String formatDouble(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((int) value);
        }
        return String.format("%.2f", value);
    }

    private static int toInt(Object value) {
        if (value instanceof Integer i) return i;
        if (value instanceof Double d) return (int) Math.round(d);
        if (value instanceof String s) return Integer.parseInt(s);
        return 0;
    }

    private static double toDouble(Object value) {
        if (value instanceof Double d) return d;
        if (value instanceof Integer i) return i;
        if (value instanceof String s) return Double.parseDouble(s);
        return 0.0;
    }
}
