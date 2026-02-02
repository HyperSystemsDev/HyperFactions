package com.hyperfactions.importer.hyfactions;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents the NameCache.json file structure from HyFactions.
 *
 * @param Values list of UUID to username mappings
 */
public record HyFactionNameCache(
    @Nullable List<HyFactionNameEntry> Values
) {
}
