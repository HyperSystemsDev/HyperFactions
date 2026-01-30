package com.hyperfactions.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a special zone (SafeZone or WarZone).
 *
 * @param id        unique identifier for the zone
 * @param name      display name of the zone
 * @param type      the zone type
 * @param world     the world name
 * @param chunkX    the chunk X coordinate
 * @param chunkZ    the chunk Z coordinate
 * @param createdAt when the zone was created (epoch millis)
 * @param createdBy UUID of the admin who created it
 * @param flags     custom flags for this zone (null = use defaults)
 */
public record Zone(
    @NotNull UUID id,
    @NotNull String name,
    @NotNull ZoneType type,
    @NotNull String world,
    int chunkX,
    int chunkZ,
    long createdAt,
    @NotNull UUID createdBy,
    @Nullable Map<String, Boolean> flags
) {
    /**
     * Creates a new zone with default flags.
     *
     * @param name      the zone name
     * @param type      the zone type
     * @param world     the world name
     * @param chunkX    the chunk X
     * @param chunkZ    the chunk Z
     * @param createdBy UUID of the creator
     * @return a new Zone
     */
    public static Zone create(@NotNull String name, @NotNull ZoneType type,
                              @NotNull String world, int chunkX, int chunkZ,
                              @NotNull UUID createdBy) {
        return new Zone(UUID.randomUUID(), name, type, world, chunkX, chunkZ,
                       System.currentTimeMillis(), createdBy, null);
    }

    /**
     * Gets the ChunkKey for this zone.
     *
     * @return the chunk key
     */
    @NotNull
    public ChunkKey toChunkKey() {
        return new ChunkKey(world, chunkX, chunkZ);
    }

    /**
     * Checks if this zone is at the given location.
     *
     * @param worldName the world name
     * @param x         the chunk X
     * @param z         the chunk Z
     * @return true if matching
     */
    public boolean isAt(@NotNull String worldName, int x, int z) {
        return world.equals(worldName) && chunkX == x && chunkZ == z;
    }

    /**
     * Checks if this is a safe zone.
     *
     * @return true if safe zone
     */
    public boolean isSafeZone() {
        return type == ZoneType.SAFE;
    }

    /**
     * Checks if this is a war zone.
     *
     * @return true if war zone
     */
    public boolean isWarZone() {
        return type == ZoneType.WAR;
    }

    /**
     * Creates a copy with a new name.
     *
     * @param newName the new name
     * @return a new Zone with updated name
     */
    public Zone withName(@NotNull String newName) {
        return new Zone(id, newName, type, world, chunkX, chunkZ, createdAt, createdBy, flags);
    }

    /**
     * Creates a copy with a flag set.
     *
     * @param flagName the flag name
     * @param value    the flag value
     * @return a new Zone with updated flag
     */
    public Zone withFlag(@NotNull String flagName, boolean value) {
        Map<String, Boolean> newFlags = flags != null ? new HashMap<>(flags) : new HashMap<>();
        newFlags.put(flagName, value);
        return new Zone(id, name, type, world, chunkX, chunkZ, createdAt, createdBy, newFlags);
    }

    /**
     * Creates a copy with a flag removed (reverts to default).
     *
     * @param flagName the flag name to remove
     * @return a new Zone with flag removed
     */
    public Zone withoutFlag(@NotNull String flagName) {
        if (flags == null || !flags.containsKey(flagName)) {
            return this;
        }
        Map<String, Boolean> newFlags = new HashMap<>(flags);
        newFlags.remove(flagName);
        return new Zone(id, name, type, world, chunkX, chunkZ, createdAt, createdBy,
                       newFlags.isEmpty() ? null : newFlags);
    }

    /**
     * Creates a copy with updated flags.
     *
     * @param newFlags the new flags map (null = use defaults)
     * @return a new Zone with updated flags
     */
    public Zone withFlags(@Nullable Map<String, Boolean> newFlags) {
        return new Zone(id, name, type, world, chunkX, chunkZ, createdAt, createdBy, newFlags);
    }

    /**
     * Gets the value of a specific flag, or null if using default.
     *
     * @param flagName the flag name
     * @return the flag value, or null if not set (use default)
     */
    @Nullable
    public Boolean getFlag(@NotNull String flagName) {
        return flags != null ? flags.get(flagName) : null;
    }

    /**
     * Checks if a specific flag has been explicitly set (overriding default).
     *
     * @param flagName the flag name
     * @return true if flag has been explicitly set
     */
    public boolean hasFlagSet(@NotNull String flagName) {
        return flags != null && flags.containsKey(flagName);
    }

    /**
     * Gets all explicitly set flags (immutable copy).
     *
     * @return map of flag name to value, never null
     */
    @NotNull
    public Map<String, Boolean> getFlags() {
        return flags != null ? Collections.unmodifiableMap(flags) : Collections.emptyMap();
    }

    /**
     * Gets the effective value for a flag, considering zone type defaults.
     *
     * @param flagName the flag name
     * @return the effective flag value
     */
    public boolean getEffectiveFlag(@NotNull String flagName) {
        // Check if explicitly set
        if (flags != null && flags.containsKey(flagName)) {
            return flags.get(flagName);
        }
        // Return default based on zone type
        return isSafeZone() ? ZoneFlags.getSafeZoneDefault(flagName) : ZoneFlags.getWarZoneDefault(flagName);
    }
}
