package com.hyperfactions.api.events;

import com.hyperfactions.data.Faction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Event fired when a faction is disbanded.
 * The disbandedBy field may be null for system-initiated disbands
 * (e.g., when the last member leaves).
 */
public record FactionDisbandEvent(
    @NotNull Faction faction,
    @Nullable UUID disbandedBy
) {}
