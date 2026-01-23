package com.hyperfactions.data;

import org.jetbrains.annotations.NotNull;

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
 */
public record Zone(
    @NotNull UUID id,
    @NotNull String name,
    @NotNull ZoneType type,
    @NotNull String world,
    int chunkX,
    int chunkZ,
    long createdAt,
    @NotNull UUID createdBy
) {
    /**
     * Creates a new zone.
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
                       System.currentTimeMillis(), createdBy);
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
        return new Zone(id, newName, type, world, chunkX, chunkZ, createdAt, createdBy);
    }
}
