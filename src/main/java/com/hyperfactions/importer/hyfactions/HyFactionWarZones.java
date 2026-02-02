package com.hyperfactions.importer.hyfactions;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents the WarZones.json file structure from HyFactions.
 *
 * @param WarZones list of war zone chunks
 */
public record HyFactionWarZones(
    @Nullable List<HyFactionZoneChunk> WarZones
) {
}
