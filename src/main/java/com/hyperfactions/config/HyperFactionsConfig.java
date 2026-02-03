package com.hyperfactions.config;

import com.hyperfactions.data.FactionPermissions;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;

/**
 * Legacy configuration singleton for HyperFactions.
 * <p>
 * <b>DEPRECATED:</b> This class is maintained for backward compatibility.
 * New code should use {@link ConfigManager} directly:
 * <pre>
 * // Instead of:
 * HyperFactionsConfig.get().getMaxMembers();
 *
 * // Use:
 * ConfigManager.get().getMaxMembers();
 * // Or for more control:
 * ConfigManager.get().core().getMaxMembers();
 * ConfigManager.get().backup().isEnabled();
 * ConfigManager.get().chat().getFormat();
 * </pre>
 *
 * @deprecated Use {@link ConfigManager} instead. This class delegates all calls
 *             to ConfigManager and will be removed in a future version.
 */
@Deprecated(since = "0.4.0", forRemoval = true)
public class HyperFactionsConfig {

    private static HyperFactionsConfig instance;

    private HyperFactionsConfig() {}

    /**
     * Gets the singleton config instance.
     *
     * @return the config instance
     * @deprecated Use {@link ConfigManager#get()} instead
     */
    @Deprecated
    public static HyperFactionsConfig get() {
        if (instance == null) {
            instance = new HyperFactionsConfig();
        }
        return instance;
    }

    /**
     * Loads the configuration from file.
     *
     * @param dataDir the plugin data directory
     * @deprecated Use {@link ConfigManager#get()}.loadAll(dataDir) instead
     */
    @Deprecated
    public void load(@NotNull Path dataDir) {
        ConfigManager.get().loadAll(dataDir);
    }

    /**
     * Saves the configuration to file.
     *
     * @param dataDir the plugin data directory
     * @deprecated Use {@link ConfigManager#get()}.saveAll() instead
     */
    @Deprecated
    public void save(@NotNull Path dataDir) {
        ConfigManager.get().saveAll();
    }

    /**
     * Reloads the configuration.
     *
     * @param dataDir the plugin data directory
     * @deprecated Use {@link ConfigManager#get()}.reloadAll() instead
     */
    @Deprecated
    public void reload(@NotNull Path dataDir) {
        ConfigManager.get().reloadAll();
    }

    // === All getters delegate to ConfigManager ===

    // Faction settings
    public int getMaxMembers() { return ConfigManager.get().getMaxMembers(); }
    public int getMaxNameLength() { return ConfigManager.get().getMaxNameLength(); }
    public int getMinNameLength() { return ConfigManager.get().getMinNameLength(); }
    public boolean isAllowColors() { return ConfigManager.get().isAllowColors(); }

    // Power settings
    public double getMaxPlayerPower() { return ConfigManager.get().getMaxPlayerPower(); }
    public double getStartingPower() { return ConfigManager.get().getStartingPower(); }
    public double getPowerPerClaim() { return ConfigManager.get().getPowerPerClaim(); }
    public double getDeathPenalty() { return ConfigManager.get().getDeathPenalty(); }
    public double getRegenPerMinute() { return ConfigManager.get().getRegenPerMinute(); }
    public boolean isRegenWhenOffline() { return ConfigManager.get().isRegenWhenOffline(); }

    // Claim settings
    public int getMaxClaims() { return ConfigManager.get().getMaxClaims(); }
    public boolean isOnlyAdjacent() { return ConfigManager.get().isOnlyAdjacent(); }
    public boolean isDecayEnabled() { return ConfigManager.get().isDecayEnabled(); }
    public int getDecayDaysInactive() { return ConfigManager.get().getDecayDaysInactive(); }
    public List<String> getWorldWhitelist() { return ConfigManager.get().getWorldWhitelist(); }
    public List<String> getWorldBlacklist() { return ConfigManager.get().getWorldBlacklist(); }

    // Combat settings
    public int getTagDurationSeconds() { return ConfigManager.get().getTagDurationSeconds(); }
    public boolean isAllyDamage() { return ConfigManager.get().isAllyDamage(); }
    public boolean isFactionDamage() { return ConfigManager.get().isFactionDamage(); }
    public boolean isTaggedLogoutPenalty() { return ConfigManager.get().isTaggedLogoutPenalty(); }
    public double getLogoutPowerLoss() { return ConfigManager.get().getLogoutPowerLoss(); }

    // Spawn Protection settings
    public boolean isSpawnProtectionEnabled() { return ConfigManager.get().isSpawnProtectionEnabled(); }
    public int getSpawnProtectionDurationSeconds() { return ConfigManager.get().getSpawnProtectionDurationSeconds(); }
    public boolean isSpawnProtectionBreakOnAttack() { return ConfigManager.get().isSpawnProtectionBreakOnAttack(); }
    public boolean isSpawnProtectionBreakOnMove() { return ConfigManager.get().isSpawnProtectionBreakOnMove(); }

    // Relation settings
    public int getMaxAllies() { return ConfigManager.get().getMaxAllies(); }
    public int getMaxEnemies() { return ConfigManager.get().getMaxEnemies(); }

    // Invite/Request settings
    public int getInviteExpirationMinutes() { return ConfigManager.get().getInviteExpirationMinutes(); }
    public int getJoinRequestExpirationHours() { return ConfigManager.get().getJoinRequestExpirationHours(); }
    public long getInviteExpirationMs() { return ConfigManager.get().getInviteExpirationMs(); }
    public long getJoinRequestExpirationMs() { return ConfigManager.get().getJoinRequestExpirationMs(); }

    // Stuck settings
    public int getStuckWarmupSeconds() { return ConfigManager.get().getStuckWarmupSeconds(); }
    public int getStuckCooldownSeconds() { return ConfigManager.get().getStuckCooldownSeconds(); }

    // Teleport settings
    public int getWarmupSeconds() { return ConfigManager.get().getWarmupSeconds(); }
    public int getCooldownSeconds() { return ConfigManager.get().getCooldownSeconds(); }
    public boolean isCancelOnMove() { return ConfigManager.get().isCancelOnMove(); }
    public boolean isCancelOnDamage() { return ConfigManager.get().isCancelOnDamage(); }

    // Update settings
    public boolean isUpdateCheckEnabled() { return ConfigManager.get().isUpdateCheckEnabled(); }
    public String getUpdateCheckUrl() { return ConfigManager.get().getUpdateCheckUrl(); }
    public String getReleaseChannel() { return ConfigManager.get().getReleaseChannel(); }
    public boolean isPreReleaseChannel() { return ConfigManager.get().isPreReleaseChannel(); }

    // Auto-save settings
    public boolean isAutoSaveEnabled() { return ConfigManager.get().isAutoSaveEnabled(); }
    public int getAutoSaveIntervalMinutes() { return ConfigManager.get().getAutoSaveIntervalMinutes(); }

    // Backup settings
    public boolean isBackupEnabled() { return ConfigManager.get().isBackupEnabled(); }
    public int getBackupHourlyRetention() { return ConfigManager.get().getBackupHourlyRetention(); }
    public int getBackupDailyRetention() { return ConfigManager.get().getBackupDailyRetention(); }
    public int getBackupWeeklyRetention() { return ConfigManager.get().getBackupWeeklyRetention(); }
    public int getBackupManualRetention() { return ConfigManager.get().getBackupManualRetention(); }
    public boolean isBackupOnShutdown() { return ConfigManager.get().isBackupOnShutdown(); }

    // Economy settings
    public boolean isEconomyEnabled() { return ConfigManager.get().isEconomyEnabled(); }
    public String getEconomyCurrencyName() { return ConfigManager.get().getEconomyCurrencyName(); }
    public String getEconomyCurrencyNamePlural() { return ConfigManager.get().getEconomyCurrencyNamePlural(); }
    public String getEconomyCurrencySymbol() { return ConfigManager.get().getEconomyCurrencySymbol(); }
    public double getEconomyStartingBalance() { return ConfigManager.get().getEconomyStartingBalance(); }

    // Message settings
    public String getPrefix() { return ConfigManager.get().getPrefix(); }
    public String getPrimaryColor() { return ConfigManager.get().getPrimaryColor(); }

    // GUI settings
    public String getGuiTitle() { return ConfigManager.get().getGuiTitle(); }

    // Territory notification settings
    public boolean isTerritoryNotificationsEnabled() { return ConfigManager.get().isTerritoryNotificationsEnabled(); }

    // World map marker settings
    public boolean isWorldMapMarkersEnabled() { return ConfigManager.get().isWorldMapMarkersEnabled(); }

    // Debug settings
    public boolean isDebugEnabledByDefault() { return ConfigManager.get().isDebugEnabledByDefault(); }
    public boolean isDebugLogToConsole() { return ConfigManager.get().isDebugLogToConsole(); }
    public boolean isDebugPower() { return ConfigManager.get().isDebugPower(); }
    public boolean isDebugClaim() { return ConfigManager.get().isDebugClaim(); }
    public boolean isDebugCombat() { return ConfigManager.get().isDebugCombat(); }
    public boolean isDebugProtection() { return ConfigManager.get().isDebugProtection(); }
    public boolean isDebugRelation() { return ConfigManager.get().isDebugRelation(); }
    public boolean isDebugTerritory() { return ConfigManager.get().isDebugTerritory(); }

    // Chat formatting settings
    public boolean isChatFormattingEnabled() { return ConfigManager.get().isChatFormattingEnabled(); }
    public String getChatFormat() { return ConfigManager.get().getChatFormat(); }
    public String getChatTagDisplay() { return ConfigManager.get().getChatTagDisplay(); }
    public String getChatTagFormat() { return ConfigManager.get().getChatTagFormat(); }
    public String getChatRelationColorOwn() { return ConfigManager.get().getChatRelationColorOwn(); }
    public String getChatRelationColorAlly() { return ConfigManager.get().getChatRelationColorAlly(); }
    public String getChatRelationColorNeutral() { return ConfigManager.get().getChatRelationColorNeutral(); }
    public String getChatRelationColorEnemy() { return ConfigManager.get().getChatRelationColorEnemy(); }
    public String getChatNoFactionTag() { return ConfigManager.get().getChatNoFactionTag(); }
    public String getChatEventPriority() { return ConfigManager.get().getChatEventPriority(); }

    // Permission settings
    public boolean isAdminRequiresOp() { return ConfigManager.get().isAdminRequiresOp(); }
    public String getPermissionFallbackBehavior() { return ConfigManager.get().getPermissionFallbackBehavior(); }

    // Faction permission methods
    @NotNull
    public FactionPermissions getDefaultFactionPermissions() {
        return ConfigManager.get().getDefaultFactionPermissions();
    }

    @NotNull
    public FactionPermissions getEffectiveFactionPermissions(@NotNull FactionPermissions factionPerms) {
        return ConfigManager.get().getEffectiveFactionPermissions(factionPerms);
    }

    public boolean isPermissionLocked(@NotNull String permissionName) {
        return ConfigManager.get().isPermissionLocked(permissionName);
    }

    // Debug setters
    public void setDebugPower(boolean enabled) { ConfigManager.get().setDebugPower(enabled); }
    public void setDebugClaim(boolean enabled) { ConfigManager.get().setDebugClaim(enabled); }
    public void setDebugCombat(boolean enabled) { ConfigManager.get().setDebugCombat(enabled); }
    public void setDebugProtection(boolean enabled) { ConfigManager.get().setDebugProtection(enabled); }
    public void setDebugRelation(boolean enabled) { ConfigManager.get().setDebugRelation(enabled); }
    public void setDebugTerritory(boolean enabled) { ConfigManager.get().setDebugTerritory(enabled); }

    public void applyDebugSettings() { ConfigManager.get().applyDebugSettings(); }
    public void enableAllDebug() { ConfigManager.get().enableAllDebug(); }
    public void disableAllDebug() { ConfigManager.get().disableAllDebug(); }

    // Utility methods
    public boolean isWorldAllowed(@NotNull String worldName) {
        return ConfigManager.get().isWorldAllowed(worldName);
    }

    public int calculateMaxClaims(double totalPower) {
        return ConfigManager.get().calculateMaxClaims(totalPower);
    }
}
