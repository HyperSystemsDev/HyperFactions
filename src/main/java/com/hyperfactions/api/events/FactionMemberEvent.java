package com.hyperfactions.api.events;

import com.hyperfactions.data.Faction;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Event fired when a player joins or leaves a faction.
 */
public record FactionMemberEvent(
    @NotNull Faction faction,
    @NotNull UUID playerUuid,
    @NotNull Type type
) {
    public enum Type {
        JOIN,
        LEAVE,
        KICK
    }
}
