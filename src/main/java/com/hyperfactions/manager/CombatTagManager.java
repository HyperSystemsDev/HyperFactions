package com.hyperfactions.manager;

import com.hyperfactions.config.HyperFactionsConfig;
import com.hyperfactions.data.CombatTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Manages combat tagging for PvP logout prevention.
 */
public class CombatTagManager {

    // Active combat tags: player UUID -> CombatTag
    private final Map<UUID, CombatTag> tags = new ConcurrentHashMap<>();

    // Callbacks for tag events
    private Consumer<UUID> onTagExpired;
    private Consumer<UUID> onCombatLogout;

    /**
     * Sets the callback for when a tag expires naturally.
     *
     * @param callback the callback
     */
    public void setOnTagExpired(@Nullable Consumer<UUID> callback) {
        this.onTagExpired = callback;
    }

    /**
     * Sets the callback for when a player logs out while tagged.
     *
     * @param callback the callback
     */
    public void setOnCombatLogout(@Nullable Consumer<UUID> callback) {
        this.onCombatLogout = callback;
    }

    // === Queries ===

    /**
     * Checks if a player is combat tagged.
     *
     * @param playerUuid the player's UUID
     * @return true if tagged and not expired
     */
    public boolean isTagged(@NotNull UUID playerUuid) {
        CombatTag tag = tags.get(playerUuid);
        if (tag == null) {
            return false;
        }
        if (tag.isExpired()) {
            tags.remove(playerUuid);
            return false;
        }
        return true;
    }

    /**
     * Gets the combat tag for a player.
     *
     * @param playerUuid the player's UUID
     * @return the tag, or null if not tagged
     */
    @Nullable
    public CombatTag getTag(@NotNull UUID playerUuid) {
        CombatTag tag = tags.get(playerUuid);
        if (tag != null && tag.isExpired()) {
            tags.remove(playerUuid);
            return null;
        }
        return tag;
    }

    /**
     * Gets the remaining tag time in seconds.
     *
     * @param playerUuid the player's UUID
     * @return remaining seconds, 0 if not tagged
     */
    public int getRemainingSeconds(@NotNull UUID playerUuid) {
        CombatTag tag = getTag(playerUuid);
        return tag != null ? tag.getRemainingSeconds() : 0;
    }

    /**
     * Gets all currently tagged players.
     *
     * @return set of tagged player UUIDs
     */
    @NotNull
    public Set<UUID> getTaggedPlayers() {
        // Clean up expired tags and return active ones
        Set<UUID> active = new HashSet<>();
        Iterator<Map.Entry<UUID, CombatTag>> iter = tags.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<UUID, CombatTag> entry = iter.next();
            if (entry.getValue().isExpired()) {
                iter.remove();
            } else {
                active.add(entry.getKey());
            }
        }
        return active;
    }

    // === Operations ===

    /**
     * Tags a player with combat.
     *
     * @param playerUuid the player's UUID
     * @return the combat tag
     */
    @NotNull
    public CombatTag tagPlayer(@NotNull UUID playerUuid) {
        int duration = HyperFactionsConfig.get().getTagDurationSeconds();
        return tagPlayer(playerUuid, duration);
    }

    /**
     * Tags a player with combat for a specific duration.
     *
     * @param playerUuid the player's UUID
     * @param durationSeconds the tag duration
     * @return the combat tag
     */
    @NotNull
    public CombatTag tagPlayer(@NotNull UUID playerUuid, int durationSeconds) {
        CombatTag existing = tags.get(playerUuid);

        // Refresh if already tagged
        if (existing != null && !existing.isExpired()) {
            CombatTag refreshed = existing.refresh(durationSeconds);
            tags.put(playerUuid, refreshed);
            return refreshed;
        }

        // New tag
        CombatTag tag = CombatTag.create(playerUuid, durationSeconds);
        tags.put(playerUuid, tag);
        return tag;
    }

    /**
     * Tags both players in a combat interaction.
     *
     * @param attacker the attacker's UUID
     * @param defender the defender's UUID
     */
    public void tagCombat(@NotNull UUID attacker, @NotNull UUID defender) {
        tagPlayer(attacker);
        tagPlayer(defender);
    }

    /**
     * Clears a player's combat tag.
     *
     * @param playerUuid the player's UUID
     */
    public void clearTag(@NotNull UUID playerUuid) {
        tags.remove(playerUuid);
    }

    /**
     * Called when a player disconnects.
     * Handles combat logout penalty if tagged.
     *
     * @param playerUuid the player's UUID
     * @return true if the player was combat tagged
     */
    public boolean handleDisconnect(@NotNull UUID playerUuid) {
        CombatTag tag = tags.remove(playerUuid);
        if (tag != null && !tag.isExpired()) {
            if (HyperFactionsConfig.get().isTaggedLogoutPenalty() && onCombatLogout != null) {
                onCombatLogout.accept(playerUuid);
            }
            return true;
        }
        return false;
    }

    /**
     * Called periodically to decay expired tags.
     * Also triggers callbacks for naturally expired tags.
     */
    public void tickDecay() {
        Iterator<Map.Entry<UUID, CombatTag>> iter = tags.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<UUID, CombatTag> entry = iter.next();
            if (entry.getValue().isExpired()) {
                UUID playerUuid = entry.getKey();
                iter.remove();
                if (onTagExpired != null) {
                    onTagExpired.accept(playerUuid);
                }
            }
        }
    }

    /**
     * Gets the count of active combat tags.
     *
     * @return number of tagged players
     */
    public int getTagCount() {
        return getTaggedPlayers().size();
    }
}
