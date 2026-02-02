package com.hyperfactions.importer.hyfactions;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents the Claims.json file structure from HyFactions.
 *
 * @param Dimensions list of dimension entries with their claims
 */
public record HyFactionClaims(
    @Nullable List<HyFactionDimension> Dimensions
) {
}
