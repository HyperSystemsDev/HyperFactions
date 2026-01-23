package com.hyperfactions.api.events;

import com.hyperfactions.data.Faction;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Event fired when a faction claims a chunk.
 */
public record FactionClaimEvent(
    @NotNull Faction faction,
    @NotNull UUID claimedBy,
    @NotNull String world,
    int chunkX,
    int chunkZ
) {}
