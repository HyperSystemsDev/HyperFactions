package com.hyperfactions.integration;

import com.hyperfactions.util.Logger;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Reflection-based soft dependency bridge for Zurku's GravestonePlugin.
 * <p>
 * Provides access to gravestone ownership data without requiring GravestonePlugin
 * at compile time. If GravestonePlugin is not installed, all methods return safe defaults.
 * <p>
 * Uses {@code HytaleServer.get().getPluginManager().getPlugins()} to find the running
 * GravestonePlugin instance, then reflects into {@code GravestoneManager} for ownership lookups.
 */
public class GravestoneIntegration {

    private boolean available = false;
    private Object gravestoneManager;
    private Method getGravestoneOwnerMethod;
    private Method getSettingsMethod;
    private Method isOwnerProtectionMethod;

    /**
     * Initializes the GravestonePlugin integration.
     * Safe to call even if GravestonePlugin is not installed.
     */
    public void init() {
        try {
            // Check if GravestonePlugin classes exist
            Class<?> pluginClass = Class.forName("zurku.gravestones.GravestonePlugin");

            // Find the running plugin instance via Hytale's PluginManager
            Object pluginInstance = findPluginInstance(pluginClass);
            if (pluginInstance == null) {
                available = false;
                Logger.info("[Integration] GravestonePlugin class found but instance not available");
                return;
            }

            // Get the GravestoneManager from the plugin
            Method getManagerMethod = pluginClass.getMethod("getGravestoneManager");
            gravestoneManager = getManagerMethod.invoke(pluginInstance);
            if (gravestoneManager == null) {
                available = false;
                Logger.info("[Integration] GravestonePlugin manager not available");
                return;
            }

            // Cache reflection methods for GravestoneManager
            Class<?> managerClass = gravestoneManager.getClass();
            getGravestoneOwnerMethod = managerClass.getMethod("getGravestoneOwner", int.class, int.class, int.class);

            getSettingsMethod = managerClass.getMethod("getSettings");

            // Cache isOwnerProtection from settings
            Object settings = getSettingsMethod.invoke(gravestoneManager);
            if (settings != null) {
                isOwnerProtectionMethod = settings.getClass().getMethod("isOwnerProtection");
            }

            available = true;
            Logger.info("[Integration] GravestonePlugin detected - gravestone protection enabled");

        } catch (ClassNotFoundException e) {
            available = false;
            Logger.info("[Integration] GravestonePlugin not found - gravestone integration disabled");
        } catch (NoSuchMethodException e) {
            available = false;
            Logger.warn("[Integration] GravestonePlugin API incompatible (missing method: %s)", e.getMessage());
        } catch (Exception e) {
            available = false;
            Logger.warn("[Integration] Failed to initialize GravestonePlugin integration: %s", e.getMessage());
        }
    }

    /**
     * Finds the running GravestonePlugin instance by scanning all loaded plugins.
     */
    @Nullable
    private Object findPluginInstance(Class<?> pluginClass) {
        try {
            // HytaleServer.get().getPluginManager().getPlugins()
            Class<?> serverClass = Class.forName("com.hypixel.hytale.server.core.HytaleServer");
            Method getServer = serverClass.getMethod("get");
            Object server = getServer.invoke(null);

            Method getPluginManager = server.getClass().getMethod("getPluginManager");
            Object pluginManager = getPluginManager.invoke(server);

            Method getPlugins = pluginManager.getClass().getMethod("getPlugins");
            @SuppressWarnings("unchecked")
            java.util.List<Object> plugins = (java.util.List<Object>) getPlugins.invoke(pluginManager);

            for (Object plugin : plugins) {
                if (pluginClass.isInstance(plugin)) {
                    return plugin;
                }
            }
        } catch (Exception e) {
            Logger.debug("[Integration] Failed to scan plugins for GravestonePlugin: %s", e.getMessage());
        }
        return null;
    }

    /**
     * Checks if GravestonePlugin is available and integrated.
     *
     * @return true if GravestonePlugin is loaded and accessible
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * Gets the UUID of the player who owns the gravestone at the given coordinates.
     *
     * @param x block X coordinate
     * @param y block Y coordinate
     * @param z block Z coordinate
     * @return the owner's UUID, or null if no gravestone exists or plugin unavailable
     */
    @Nullable
    public UUID getGravestoneOwner(int x, int y, int z) {
        if (!available || getGravestoneOwnerMethod == null) return null;
        try {
            return (UUID) getGravestoneOwnerMethod.invoke(gravestoneManager, x, y, z);
        } catch (Exception e) {
            Logger.debug("[Integration] Failed to get gravestone owner at (%d,%d,%d): %s",
                    x, y, z, e.getMessage());
            return null;
        }
    }

    /**
     * Checks if GravestonePlugin's native owner protection is enabled.
     *
     * @return true if native owner protection is on, false otherwise or if unavailable
     */
    public boolean isNativeOwnerProtectionEnabled() {
        if (!available || getSettingsMethod == null || isOwnerProtectionMethod == null) return false;
        try {
            Object settings = getSettingsMethod.invoke(gravestoneManager);
            if (settings == null) return false;
            return (boolean) isOwnerProtectionMethod.invoke(settings);
        } catch (Exception e) {
            Logger.debug("[Integration] Failed to check native owner protection: %s", e.getMessage());
            return false;
        }
    }
}
