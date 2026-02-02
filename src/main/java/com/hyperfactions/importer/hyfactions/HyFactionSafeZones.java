package com.hyperfactions.importer.hyfactions;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents the SafeZones.json file structure from HyFactions.
 *
 * @param SafeZones list of safe zone chunks
 */
public record HyFactionSafeZones(
    @Nullable List<HyFactionZoneChunk> SafeZones
) {
}
