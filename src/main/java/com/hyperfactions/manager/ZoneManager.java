package com.hyperfactions.manager;

import com.hyperfactions.data.ChunkKey;
import com.hyperfactions.data.Zone;
import com.hyperfactions.data.ZoneFlags;
import com.hyperfactions.data.ZoneType;
import com.hyperfactions.storage.ZoneStorage;
import com.hyperfactions.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages SafeZones and WarZones.
 */
public class ZoneManager {

    private final ZoneStorage storage;
    private final ClaimManager claimManager;

    // Index: ChunkKey -> Zone
    private final Map<ChunkKey, Zone> zoneIndex = new ConcurrentHashMap<>();

    // All zones by ID
    private final Map<UUID, Zone> zonesById = new ConcurrentHashMap<>();

    public ZoneManager(@NotNull ZoneStorage storage, @NotNull ClaimManager claimManager) {
        this.storage = storage;
        this.claimManager = claimManager;
    }

    /**
     * Loads all zones from storage.
     *
     * @return a future that completes when loading is done
     */
    public CompletableFuture<Void> loadAll() {
        return storage.loadAllZones().thenAccept(loaded -> {
            zoneIndex.clear();
            zonesById.clear();

            for (Zone zone : loaded) {
                zonesById.put(zone.id(), zone);
                zoneIndex.put(zone.toChunkKey(), zone);
            }

            Logger.info("Loaded %d zones", zonesById.size());
        });
    }

    /**
     * Saves all zones to storage.
     *
     * @return a future that completes when saving is done
     */
    public CompletableFuture<Void> saveAll() {
        return storage.saveAllZones(zonesById.values());
    }

    // === Queries ===

    /**
     * Gets the zone at a chunk location.
     *
     * @param world  the world name
     * @param chunkX the chunk X
     * @param chunkZ the chunk Z
     * @return the zone, or null if none
     */
    @Nullable
    public Zone getZone(@NotNull String world, int chunkX, int chunkZ) {
        return zoneIndex.get(new ChunkKey(world, chunkX, chunkZ));
    }

    /**
     * Gets the zone at world coordinates.
     *
     * @param world the world name
     * @param x     the world X coordinate
     * @param z     the world Z coordinate
     * @return the zone, or null if none
     */
    @Nullable
    public Zone getZoneAt(@NotNull String world, double x, double z) {
        return zoneIndex.get(ChunkKey.fromWorldCoords(world, x, z));
    }

    /**
     * Checks if a location is in a SafeZone.
     *
     * @param world  the world name
     * @param chunkX the chunk X
     * @param chunkZ the chunk Z
     * @return true if in SafeZone
     */
    public boolean isInSafeZone(@NotNull String world, int chunkX, int chunkZ) {
        Zone zone = getZone(world, chunkX, chunkZ);
        return zone != null && zone.isSafeZone();
    }

    /**
     * Checks if a location is in a WarZone.
     *
     * @param world  the world name
     * @param chunkX the chunk X
     * @param chunkZ the chunk Z
     * @return true if in WarZone
     */
    public boolean isInWarZone(@NotNull String world, int chunkX, int chunkZ) {
        Zone zone = getZone(world, chunkX, chunkZ);
        return zone != null && zone.isWarZone();
    }

    /**
     * Checks if world coordinates are in a SafeZone.
     *
     * @param world the world name
     * @param x     the world X
     * @param z     the world Z
     * @return true if in SafeZone
     */
    public boolean isInSafeZoneAt(@NotNull String world, double x, double z) {
        Zone zone = getZoneAt(world, x, z);
        return zone != null && zone.isSafeZone();
    }

    /**
     * Checks if world coordinates are in a WarZone.
     *
     * @param world the world name
     * @param x     the world X
     * @param z     the world Z
     * @return true if in WarZone
     */
    public boolean isInWarZoneAt(@NotNull String world, double x, double z) {
        Zone zone = getZoneAt(world, x, z);
        return zone != null && zone.isWarZone();
    }

    /**
     * Gets a zone by ID.
     *
     * @param zoneId the zone ID
     * @return the zone, or null if not found
     */
    @Nullable
    public Zone getZoneById(@NotNull UUID zoneId) {
        return zonesById.get(zoneId);
    }

    /**
     * Gets all zones.
     *
     * @return collection of all zones
     */
    @NotNull
    public Collection<Zone> getAllZones() {
        return Collections.unmodifiableCollection(zonesById.values());
    }

    /**
     * Gets all zones of a specific type.
     *
     * @param type the zone type
     * @return list of zones
     */
    @NotNull
    public List<Zone> getZonesByType(@NotNull ZoneType type) {
        return zonesById.values().stream()
            .filter(z -> z.type() == type)
            .collect(Collectors.toList());
    }

    /**
     * Gets the total zone count.
     *
     * @return number of zones
     */
    public int getZoneCount() {
        return zonesById.size();
    }

    // === Operations ===

    /**
     * Result of a zone operation.
     */
    public enum ZoneResult {
        SUCCESS,
        ALREADY_EXISTS,
        NOT_FOUND,
        CHUNK_CLAIMED
    }

    /**
     * Creates a new zone.
     *
     * @param name      the zone name
     * @param type      the zone type
     * @param world     the world name
     * @param chunkX    the chunk X
     * @param chunkZ    the chunk Z
     * @param createdBy the creator's UUID
     * @return the result
     */
    public ZoneResult createZone(@NotNull String name, @NotNull ZoneType type,
                                 @NotNull String world, int chunkX, int chunkZ,
                                 @NotNull UUID createdBy) {
        ChunkKey key = new ChunkKey(world, chunkX, chunkZ);

        // Check if zone already exists at location
        if (zoneIndex.containsKey(key)) {
            return ZoneResult.ALREADY_EXISTS;
        }

        // Check if chunk is claimed by a faction
        if (claimManager.getClaimOwner(world, chunkX, chunkZ) != null) {
            return ZoneResult.CHUNK_CLAIMED;
        }

        Zone zone = Zone.create(name, type, world, chunkX, chunkZ, createdBy);

        zonesById.put(zone.id(), zone);
        zoneIndex.put(key, zone);

        // Save async
        saveAll();

        Logger.info("Created %s '%s' at %d, %d in %s", type.getDisplayName(), name, chunkX, chunkZ, world);
        return ZoneResult.SUCCESS;
    }

    /**
     * Removes a zone.
     *
     * @param zoneId the zone ID
     * @return the result
     */
    public ZoneResult removeZone(@NotNull UUID zoneId) {
        Zone zone = zonesById.remove(zoneId);
        if (zone == null) {
            return ZoneResult.NOT_FOUND;
        }

        zoneIndex.remove(zone.toChunkKey());

        // Save async
        saveAll();

        Logger.info("Removed %s '%s'", zone.type().getDisplayName(), zone.name());
        return ZoneResult.SUCCESS;
    }

    /**
     * Removes a zone at a specific location.
     *
     * @param world  the world name
     * @param chunkX the chunk X
     * @param chunkZ the chunk Z
     * @return the result
     */
    public ZoneResult removeZoneAt(@NotNull String world, int chunkX, int chunkZ) {
        ChunkKey key = new ChunkKey(world, chunkX, chunkZ);
        Zone zone = zoneIndex.remove(key);
        if (zone == null) {
            return ZoneResult.NOT_FOUND;
        }

        zonesById.remove(zone.id());

        // Save async
        saveAll();

        return ZoneResult.SUCCESS;
    }

    /**
     * Renames a zone.
     *
     * @param zoneId  the zone ID
     * @param newName the new name
     * @return the result
     */
    public ZoneResult renameZone(@NotNull UUID zoneId, @NotNull String newName) {
        Zone zone = zonesById.get(zoneId);
        if (zone == null) {
            return ZoneResult.NOT_FOUND;
        }

        Zone updated = zone.withName(newName);
        zonesById.put(zoneId, updated);
        zoneIndex.put(zone.toChunkKey(), updated);

        saveAll();
        return ZoneResult.SUCCESS;
    }

    // === Flag Management ===

    /**
     * Sets a flag on a zone.
     *
     * @param zoneId   the zone ID
     * @param flagName the flag name (see ZoneFlags)
     * @param value    the flag value
     * @return the result
     */
    public ZoneResult setZoneFlag(@NotNull UUID zoneId, @NotNull String flagName, boolean value) {
        if (!ZoneFlags.isValidFlag(flagName)) {
            return ZoneResult.NOT_FOUND; // Invalid flag
        }

        Zone zone = zonesById.get(zoneId);
        if (zone == null) {
            return ZoneResult.NOT_FOUND;
        }

        Zone updated = zone.withFlag(flagName, value);
        zonesById.put(zoneId, updated);
        zoneIndex.put(zone.toChunkKey(), updated);

        saveAll();
        Logger.info("Set flag '%s' to %s on zone '%s'", flagName, value, zone.name());
        return ZoneResult.SUCCESS;
    }

    /**
     * Clears a flag from a zone (reverts to default).
     *
     * @param zoneId   the zone ID
     * @param flagName the flag name
     * @return the result
     */
    public ZoneResult clearZoneFlag(@NotNull UUID zoneId, @NotNull String flagName) {
        Zone zone = zonesById.get(zoneId);
        if (zone == null) {
            return ZoneResult.NOT_FOUND;
        }

        Zone updated = zone.withoutFlag(flagName);
        zonesById.put(zoneId, updated);
        zoneIndex.put(zone.toChunkKey(), updated);

        saveAll();
        Logger.info("Cleared flag '%s' from zone '%s' (using default)", flagName, zone.name());
        return ZoneResult.SUCCESS;
    }

    /**
     * Gets the effective value of a flag for a zone.
     * Returns the zone's custom value if set, otherwise the default for its type.
     *
     * @param zone     the zone
     * @param flagName the flag name
     * @return the effective flag value
     */
    public boolean getEffectiveFlag(@NotNull Zone zone, @NotNull String flagName) {
        return zone.getEffectiveFlag(flagName);
    }

    /**
     * Gets the effective value of a flag at a location.
     * Returns the zone's flag value if in a zone, otherwise returns the default.
     *
     * @param world    the world name
     * @param chunkX   the chunk X
     * @param chunkZ   the chunk Z
     * @param flagName the flag name
     * @param defaultValue the default value if not in a zone
     * @return the effective flag value
     */
    public boolean getEffectiveFlagAt(@NotNull String world, int chunkX, int chunkZ,
                                       @NotNull String flagName, boolean defaultValue) {
        Zone zone = getZone(world, chunkX, chunkZ);
        if (zone == null) {
            return defaultValue;
        }
        return zone.getEffectiveFlag(flagName);
    }
}
