package com.hyperfactions.integration;

import com.hyperfactions.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter wrapping existing HyperPermsIntegration as a PermissionProvider.
 * This allows HyperPerms to participate in the chain of permission providers.
 */
public class HyperPermsProviderAdapter implements PermissionProvider {

    private boolean available = false;
    private Object hyperPermsInstance = null;
    private Method hasPermissionMethod = null;
    private Method getUserManagerMethod = null;
    private Method getPrefixMethod = null;
    private Method getSuffixMethod = null;

    @Override
    @NotNull
    public String getName() {
        return "HyperPerms";
    }

    /**
     * Initializes the HyperPerms provider.
     * Attempts to load HyperPerms via reflection.
     */
    public void init() {
        try {
            // Try to load HyperPerms via HyperPermsBootstrap
            Class<?> bootstrapClass = Class.forName("com.hyperperms.HyperPermsBootstrap");
            Method getInstanceMethod = bootstrapClass.getMethod("getInstance");
            hyperPermsInstance = getInstanceMethod.invoke(null);

            if (hyperPermsInstance == null) {
                available = false;
                Logger.debug("[HyperPermsProvider] Bootstrap returned null");
                return;
            }

            Class<?> instanceClass = hyperPermsInstance.getClass();

            // Get the hasPermission method
            hasPermissionMethod = instanceClass.getMethod("hasPermission", UUID.class, String.class);
            getUserManagerMethod = instanceClass.getMethod("getUserManager");

            // Try to find prefix/suffix methods
            try {
                Object userManager = getUserManagerMethod.invoke(hyperPermsInstance);
                if (userManager != null) {
                    Class<?> userManagerClass = userManager.getClass();
                    // Check if chat meta methods exist
                    try {
                        Method getUserMethod = userManagerClass.getMethod("getUser", UUID.class);
                        Object testUser = getUserMethod.invoke(userManager, UUID.randomUUID());
                        // Methods exist if we get this far without exception
                    } catch (Exception e) {
                        // User methods not available
                    }
                }
            } catch (Exception e) {
                Logger.debug("[HyperPermsProvider] Prefix/suffix not available: %s", e.getMessage());
            }

            available = true;
            Logger.info("[PermissionManager] HyperPerms provider initialized");

        } catch (ClassNotFoundException e) {
            available = false;
            Logger.debug("[HyperPermsProvider] HyperPerms not found");
        } catch (Exception e) {
            available = false;
            Logger.debug("[HyperPermsProvider] Failed to initialize: %s", e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    @NotNull
    public Optional<Boolean> hasPermission(@NotNull UUID playerUuid, @NotNull String permission) {
        if (!available || hyperPermsInstance == null || hasPermissionMethod == null) {
            return Optional.empty();
        }

        try {
            Object result = hasPermissionMethod.invoke(hyperPermsInstance, playerUuid, permission);

            if (result instanceof Boolean) {
                return Optional.of((Boolean) result);
            }

            // Unexpected result type
            Logger.debug("[HyperPermsProvider] Unexpected result type: %s",
                result != null ? result.getClass().getName() : "null");
            return Optional.empty();

        } catch (Exception e) {
            Logger.debug("[HyperPermsProvider] Exception checking permission: %s", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    @Nullable
    public String getPrefix(@NotNull UUID playerUuid, @Nullable String worldName) {
        if (!available || hyperPermsInstance == null || getUserManagerMethod == null) {
            return null;
        }

        try {
            Object userManager = getUserManagerMethod.invoke(hyperPermsInstance);
            if (userManager == null) return null;

            Method getUserMethod = userManager.getClass().getMethod("getUser", UUID.class);
            Object user = getUserMethod.invoke(userManager, playerUuid);
            if (user == null) return null;

            // Try getPrefix method
            try {
                Method getPrefixMethod = user.getClass().getMethod("getPrefix");
                Object prefix = getPrefixMethod.invoke(user);
                return prefix != null ? prefix.toString() : null;
            } catch (NoSuchMethodException e) {
                // Method doesn't exist
                return null;
            }
        } catch (Exception e) {
            Logger.debug("[HyperPermsProvider] Failed to get prefix: %s", e.getMessage());
            return null;
        }
    }

    @Override
    @Nullable
    public String getSuffix(@NotNull UUID playerUuid, @Nullable String worldName) {
        if (!available || hyperPermsInstance == null || getUserManagerMethod == null) {
            return null;
        }

        try {
            Object userManager = getUserManagerMethod.invoke(hyperPermsInstance);
            if (userManager == null) return null;

            Method getUserMethod = userManager.getClass().getMethod("getUser", UUID.class);
            Object user = getUserMethod.invoke(userManager, playerUuid);
            if (user == null) return null;

            // Try getSuffix method
            try {
                Method getSuffixMethod = user.getClass().getMethod("getSuffix");
                Object suffix = getSuffixMethod.invoke(user);
                return suffix != null ? suffix.toString() : null;
            } catch (NoSuchMethodException e) {
                // Method doesn't exist
                return null;
            }
        } catch (Exception e) {
            Logger.debug("[HyperPermsProvider] Failed to get suffix: %s", e.getMessage());
            return null;
        }
    }

    @Override
    @NotNull
    public String getPrimaryGroup(@NotNull UUID playerUuid) {
        if (!available || hyperPermsInstance == null || getUserManagerMethod == null) {
            return "default";
        }

        try {
            Object userManager = getUserManagerMethod.invoke(hyperPermsInstance);
            if (userManager == null) return "default";

            Method getUserMethod = userManager.getClass().getMethod("getUser", UUID.class);
            Object user = getUserMethod.invoke(userManager, playerUuid);
            if (user == null) return "default";

            Method getPrimaryGroupMethod = user.getClass().getMethod("getPrimaryGroup");
            Object result = getPrimaryGroupMethod.invoke(user);
            return result != null ? result.toString() : "default";

        } catch (Exception e) {
            Logger.debug("[HyperPermsProvider] Failed to get primary group: %s", e.getMessage());
            return "default";
        }
    }
}
