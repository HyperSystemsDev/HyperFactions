package com.hyperfactions.gui.page.admin.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tracks pending config changes in memory until the user clicks Save.
 * Stores key-value pairs where keys match config field identifiers.
 */
public class ConfigChangeTracker {

    private final Map<String, Object> pending = new LinkedHashMap<>();

    /**
     * Sets a pending change.
     */
    public void set(@NotNull String key, @NotNull Object value) {
        pending.put(key, value);
    }

    /**
     * Removes a pending change (reverts to saved value).
     */
    public void remove(@NotNull String key) {
        pending.remove(key);
    }

    /**
     * Gets the effective value: pending if set, otherwise the current saved value.
     */
    @SuppressWarnings("unchecked")
    public <T> T getEffective(@NotNull String key, @NotNull T currentValue) {
        Object pendingValue = pending.get(key);
        if (pendingValue != null) {
            try {
                return (T) pendingValue;
            } catch (ClassCastException e) {
                return currentValue;
            }
        }
        return currentValue;
    }

    /**
     * Checks if there are any pending changes.
     */
    public boolean hasPendingChanges() {
        return !pending.isEmpty();
    }

    /**
     * Gets all pending changes.
     */
    @NotNull
    public Map<String, Object> getPendingChanges() {
        return pending;
    }

    /**
     * Checks if a specific key has a pending change.
     */
    public boolean isPending(@NotNull String key) {
        return pending.containsKey(key);
    }

    /**
     * Gets a pending value, or null if not set.
     */
    @Nullable
    public Object getPending(@NotNull String key) {
        return pending.get(key);
    }

    /**
     * Clears all pending changes.
     */
    public void clear() {
        pending.clear();
    }
}
