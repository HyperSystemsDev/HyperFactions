package com.hyperfactions.integration;

import com.hyperfactions.config.HyperFactionsConfig;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Unified permission manager with chain-of-responsibility pattern.
 * Tries providers in order: VaultUnlocked -> HyperPerms -> LuckPerms.
 *
 * Fallback behavior when no provider can answer:
 * - Admin permissions (hyperfactions.admin.*): Require OP
 * - Normal permissions: Allow by default (configurable)
 */
public class PermissionManager {

    private static final PermissionManager INSTANCE = new PermissionManager();

    private final List<PermissionProvider> providers = new ArrayList<>();
    private Function<UUID, PlayerRef> playerLookup;
    private boolean initialized = false;

    private PermissionManager() {}

    /**
     * Gets the singleton instance.
     *
     * @return the PermissionManager instance
     */
    public static PermissionManager get() {
        return INSTANCE;
    }

    /**
     * Initializes all permission providers.
     * Should be called once during plugin startup.
     */
    public void init() {
        if (initialized) {
            Logger.warn("[PermissionManager] Already initialized, skipping");
            return;
        }

        providers.clear();

        // Initialize providers in priority order
        // VaultUnlocked first (acts as abstraction layer for other plugins)
        VaultUnlockedProvider vaultProvider = new VaultUnlockedProvider();
        vaultProvider.init();
        if (vaultProvider.isAvailable()) {
            providers.add(vaultProvider);
        }

        // HyperPerms second
        HyperPermsProviderAdapter hyperPermsProvider = new HyperPermsProviderAdapter();
        hyperPermsProvider.init();
        if (hyperPermsProvider.isAvailable()) {
            providers.add(hyperPermsProvider);
        }

        // LuckPerms third
        LuckPermsProvider luckPermsProvider = new LuckPermsProvider();
        luckPermsProvider.init();
        if (luckPermsProvider.isAvailable()) {
            providers.add(luckPermsProvider);
        }

        initialized = true;

        if (providers.isEmpty()) {
            Logger.info("[PermissionManager] No permission providers found - using fallback mode");
        } else {
            Logger.info("[PermissionManager] Initialized with %d provider(s): %s",
                providers.size(), getProviderNames());
        }
    }

    /**
     * Sets the player lookup function for OP checks.
     *
     * @param lookup function to get PlayerRef by UUID
     */
    public void setPlayerLookup(@NotNull Function<UUID, PlayerRef> lookup) {
        this.playerLookup = lookup;
    }

    /**
     * Checks if a player has a permission.
     *
     * Chain behavior:
     * 1. Try each provider in order for the specific permission
     * 2. If any provider returns true/false, use that result
     * 3. For user-level permissions (not admin/bypass), also check hyperfactions.use as alternative
     * 4. If all providers return empty (undefined), use fallback:
     *    - Admin perms (hyperfactions.admin.*): Check if player is OP
     *    - Normal perms: Use config fallback behavior (default: allow)
     *
     * @param playerUuid the player's UUID
     * @param permission the permission to check
     * @return true if the player has the permission
     */
    public boolean hasPermission(@NotNull UUID playerUuid, @NotNull String permission) {
        // Try each provider in order for the specific permission
        for (PermissionProvider provider : providers) {
            Optional<Boolean> result = provider.hasPermission(playerUuid, permission);
            if (result.isPresent()) {
                Logger.debug("[PermissionManager] %s answered %s for %s: %s",
                    provider.getName(), permission, playerUuid, result.get());
                return result.get();
            }
        }

        // For user-level permissions, check if player has hyperfactions.use as alternative
        // This allows "hyperfactions.use" to grant all basic user permissions
        if (isUserLevelPermission(permission) && !permission.equals("hyperfactions.use")) {
            for (PermissionProvider provider : providers) {
                Optional<Boolean> result = provider.hasPermission(playerUuid, "hyperfactions.use");
                if (result.isPresent() && result.get()) {
                    Logger.debug("[PermissionManager] %s granted via hyperfactions.use for %s: %s",
                        permission, playerUuid, true);
                    return true;
                }
            }
        }

        // Fallback behavior
        return handleFallback(playerUuid, permission);
    }

    /**
     * Checks if a permission is a user-level permission (not admin or bypass).
     * User-level permissions can be granted via hyperfactions.use.
     *
     * @param permission the permission to check
     * @return true if it's a user-level permission
     */
    private boolean isUserLevelPermission(@NotNull String permission) {
        // Admin and bypass permissions require explicit grants
        if (permission.startsWith("hyperfactions.admin")) return false;
        if (permission.startsWith("hyperfactions.bypass")) return false;
        if (permission.startsWith("hyperfactions.limit")) return false;
        // All other hyperfactions permissions are user-level
        return permission.startsWith("hyperfactions.");
    }

    /**
     * Handles fallback when no provider can answer.
     */
    private boolean handleFallback(@NotNull UUID playerUuid, @NotNull String permission) {
        boolean isAdminPerm = permission.startsWith("hyperfactions.admin");
        HyperFactionsConfig config = HyperFactionsConfig.get();

        if (isAdminPerm) {
            // Admin permissions require OP when no permission plugin is handling them
            if (config.isAdminRequiresOp()) {
                boolean isOp = isPlayerOp(playerUuid);
                Logger.debug("[PermissionManager] Admin fallback for %s: isOp=%s", playerUuid, isOp);
                return isOp;
            }
            // OP check disabled - deny admin perms
            Logger.debug("[PermissionManager] Admin fallback for %s: denied (OP check disabled)", playerUuid);
            return false;
        }

        // Normal permissions: use configured fallback behavior
        String fallbackBehavior = config.getPermissionFallbackBehavior();
        boolean allow = "allow".equalsIgnoreCase(fallbackBehavior);
        Logger.debug("[PermissionManager] Normal fallback for %s: %s (config: %s)",
            playerUuid, allow ? "allow" : "deny", fallbackBehavior);
        return allow;
    }

    /**
     * Checks if a player is OP using Hytale's PermissionsModule.
     * OP players are in the "OP" group which has the "*" wildcard permission.
     *
     * @param playerUuid the player's UUID
     * @return true if the player is OP
     */
    private boolean isPlayerOp(@NotNull UUID playerUuid) {
        try {
            // Try to use Hytale's PermissionsModule
            Class<?> permModuleClass = Class.forName("com.hypixel.hytale.server.core.permissions.PermissionsModule");
            java.lang.reflect.Method getMethod = permModuleClass.getMethod("get");
            Object permModule = getMethod.invoke(null);
            if (permModule == null) return false;

            // Check if player is in the "OP" group
            java.lang.reflect.Method getGroupsMethod = permModuleClass.getMethod("getGroupsForUser", UUID.class);
            Object groupsObj = getGroupsMethod.invoke(permModule, playerUuid);
            if (groupsObj instanceof java.util.Set<?> groups) {
                if (groups.contains("OP")) {
                    return true;
                }
            }

            // Alternative: check if player has "*" permission (OP wildcard)
            java.lang.reflect.Method hasPermMethod = permModuleClass.getMethod("hasPermission", UUID.class, String.class);
            Object result = hasPermMethod.invoke(permModule, playerUuid, "*");
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
        } catch (ClassNotFoundException e) {
            Logger.debug("[PermissionManager] PermissionsModule not found");
        } catch (Exception e) {
            Logger.debug("[PermissionManager] Error checking OP status: %s", e.getMessage());
        }
        return false;
    }

    /**
     * Gets the player's chat prefix from the first available provider.
     *
     * @param playerUuid the player's UUID
     * @param worldName  the world name for context (may be null)
     * @return the prefix string, or empty string if not found
     */
    @NotNull
    public String getPrefix(@NotNull UUID playerUuid, @Nullable String worldName) {
        for (PermissionProvider provider : providers) {
            String prefix = provider.getPrefix(playerUuid, worldName);
            if (prefix != null && !prefix.isEmpty()) {
                return prefix;
            }
        }
        return "";
    }

    /**
     * Gets the player's chat suffix from the first available provider.
     *
     * @param playerUuid the player's UUID
     * @param worldName  the world name for context (may be null)
     * @return the suffix string, or empty string if not found
     */
    @NotNull
    public String getSuffix(@NotNull UUID playerUuid, @Nullable String worldName) {
        for (PermissionProvider provider : providers) {
            String suffix = provider.getSuffix(playerUuid, worldName);
            if (suffix != null && !suffix.isEmpty()) {
                return suffix;
            }
        }
        return "";
    }

    /**
     * Gets the player's primary group from the first available provider.
     *
     * @param playerUuid the player's UUID
     * @return the primary group name, or "default" if not found
     */
    @NotNull
    public String getPrimaryGroup(@NotNull UUID playerUuid) {
        for (PermissionProvider provider : providers) {
            String group = provider.getPrimaryGroup(playerUuid);
            if (group != null && !group.isEmpty() && !"default".equals(group)) {
                return group;
            }
        }
        return "default";
    }

    /**
     * Gets the list of active provider names.
     *
     * @return comma-separated list of provider names
     */
    @NotNull
    public String getProviderNames() {
        if (providers.isEmpty()) {
            return "none";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < providers.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(providers.get(i).getName());
        }
        return sb.toString();
    }

    /**
     * Gets the number of active providers.
     *
     * @return the provider count
     */
    public int getProviderCount() {
        return providers.size();
    }

    /**
     * Checks if any permission provider is available.
     *
     * @return true if at least one provider is available
     */
    public boolean hasProviders() {
        return !providers.isEmpty();
    }

    /**
     * Gets detailed status information for debugging.
     *
     * @return status string with provider information
     */
    @NotNull
    public String getDetailedStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Permission Manager Status ===\n");
        sb.append("Initialized: ").append(initialized).append("\n");
        sb.append("Active Providers: ").append(providers.size()).append("\n");
        for (PermissionProvider provider : providers) {
            sb.append("  - ").append(provider.getName())
                    .append(" (available: ").append(provider.isAvailable()).append(")\n");
        }
        sb.append("Fallback Behavior: ").append(HyperFactionsConfig.get().getPermissionFallbackBehavior()).append("\n");
        sb.append("Admin Requires OP: ").append(HyperFactionsConfig.get().isAdminRequiresOp()).append("\n");
        return sb.toString();
    }
}
