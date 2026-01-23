package com.hyperfactions.api.events;

import com.hyperfactions.data.Faction;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Event fired when a faction is created.
 */
public record FactionCreateEvent(
    @NotNull Faction faction,
    @NotNull UUID creatorUuid
) {}
