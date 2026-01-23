package com.hyperfactions.manager;

import com.hyperfactions.data.PendingInvite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages pending faction invitations.
 */
public class InviteManager {

    // Invites by player: player UUID -> set of invites
    private final Map<UUID, Set<PendingInvite>> invitesByPlayer = new ConcurrentHashMap<>();

    // Invites by faction: faction ID -> set of invited player UUIDs
    private final Map<UUID, Set<UUID>> invitesByFaction = new ConcurrentHashMap<>();

    /**
     * Creates a new invite.
     *
     * @param factionId  the faction ID
     * @param playerUuid the invited player's UUID
     * @param invitedBy  UUID of the inviter
     * @return the created invite
     */
    @NotNull
    public PendingInvite createInvite(@NotNull UUID factionId, @NotNull UUID playerUuid, @NotNull UUID invitedBy) {
        // Remove any existing invite from this faction
        removeInvite(factionId, playerUuid);

        PendingInvite invite = PendingInvite.create(factionId, playerUuid, invitedBy);

        invitesByPlayer.computeIfAbsent(playerUuid, k -> ConcurrentHashMap.newKeySet())
            .add(invite);

        invitesByFaction.computeIfAbsent(factionId, k -> ConcurrentHashMap.newKeySet())
            .add(playerUuid);

        return invite;
    }

    /**
     * Gets an invite from a specific faction for a player.
     *
     * @param factionId  the faction ID
     * @param playerUuid the player's UUID
     * @return the invite, or null if not found or expired
     */
    @Nullable
    public PendingInvite getInvite(@NotNull UUID factionId, @NotNull UUID playerUuid) {
        Set<PendingInvite> invites = invitesByPlayer.get(playerUuid);
        if (invites == null) {
            return null;
        }

        for (PendingInvite invite : invites) {
            if (invite.factionId().equals(factionId)) {
                if (invite.isExpired()) {
                    removeInvite(factionId, playerUuid);
                    return null;
                }
                return invite;
            }
        }
        return null;
    }

    /**
     * Gets all pending invites for a player.
     *
     * @param playerUuid the player's UUID
     * @return list of non-expired invites
     */
    @NotNull
    public List<PendingInvite> getPlayerInvites(@NotNull UUID playerUuid) {
        Set<PendingInvite> invites = invitesByPlayer.get(playerUuid);
        if (invites == null) {
            return Collections.emptyList();
        }

        // Filter out expired and return
        List<PendingInvite> valid = invites.stream()
            .filter(i -> !i.isExpired())
            .collect(Collectors.toList());

        // Clean up expired
        if (valid.size() != invites.size()) {
            invites.removeIf(PendingInvite::isExpired);
        }

        return valid;
    }

    /**
     * Checks if a player has any pending invites.
     *
     * @param playerUuid the player's UUID
     * @return true if has non-expired invites
     */
    public boolean hasInvites(@NotNull UUID playerUuid) {
        return !getPlayerInvites(playerUuid).isEmpty();
    }

    /**
     * Checks if a player has an invite from a specific faction.
     *
     * @param factionId  the faction ID
     * @param playerUuid the player's UUID
     * @return true if has valid invite
     */
    public boolean hasInvite(@NotNull UUID factionId, @NotNull UUID playerUuid) {
        return getInvite(factionId, playerUuid) != null;
    }

    /**
     * Gets all players invited by a faction.
     *
     * @param factionId the faction ID
     * @return set of invited player UUIDs
     */
    @NotNull
    public Set<UUID> getFactionInvites(@NotNull UUID factionId) {
        Set<UUID> invited = invitesByFaction.get(factionId);
        if (invited == null) {
            return Collections.emptySet();
        }

        // Filter expired invites
        Set<UUID> valid = new HashSet<>();
        for (UUID playerUuid : invited) {
            if (hasInvite(factionId, playerUuid)) {
                valid.add(playerUuid);
            }
        }

        return valid;
    }

    /**
     * Removes an invite.
     *
     * @param factionId  the faction ID
     * @param playerUuid the player's UUID
     */
    public void removeInvite(@NotNull UUID factionId, @NotNull UUID playerUuid) {
        Set<PendingInvite> invites = invitesByPlayer.get(playerUuid);
        if (invites != null) {
            invites.removeIf(i -> i.factionId().equals(factionId));
            if (invites.isEmpty()) {
                invitesByPlayer.remove(playerUuid);
            }
        }

        Set<UUID> factionInvites = invitesByFaction.get(factionId);
        if (factionInvites != null) {
            factionInvites.remove(playerUuid);
            if (factionInvites.isEmpty()) {
                invitesByFaction.remove(factionId);
            }
        }
    }

    /**
     * Removes all invites for a player.
     *
     * @param playerUuid the player's UUID
     */
    public void clearPlayerInvites(@NotNull UUID playerUuid) {
        Set<PendingInvite> invites = invitesByPlayer.remove(playerUuid);
        if (invites != null) {
            for (PendingInvite invite : invites) {
                Set<UUID> factionInvites = invitesByFaction.get(invite.factionId());
                if (factionInvites != null) {
                    factionInvites.remove(playerUuid);
                }
            }
        }
    }

    /**
     * Removes all invites from a faction.
     *
     * @param factionId the faction ID
     */
    public void clearFactionInvites(@NotNull UUID factionId) {
        Set<UUID> invited = invitesByFaction.remove(factionId);
        if (invited != null) {
            for (UUID playerUuid : invited) {
                Set<PendingInvite> invites = invitesByPlayer.get(playerUuid);
                if (invites != null) {
                    invites.removeIf(i -> i.factionId().equals(factionId));
                    if (invites.isEmpty()) {
                        invitesByPlayer.remove(playerUuid);
                    }
                }
            }
        }
    }

    /**
     * Cleans up all expired invites.
     * Call periodically.
     */
    public void cleanupExpired() {
        for (Map.Entry<UUID, Set<PendingInvite>> entry : invitesByPlayer.entrySet()) {
            entry.getValue().removeIf(PendingInvite::isExpired);
            if (entry.getValue().isEmpty()) {
                invitesByPlayer.remove(entry.getKey());
            }
        }

        // Rebuild faction index
        invitesByFaction.clear();
        for (Map.Entry<UUID, Set<PendingInvite>> entry : invitesByPlayer.entrySet()) {
            for (PendingInvite invite : entry.getValue()) {
                invitesByFaction.computeIfAbsent(invite.factionId(), k -> ConcurrentHashMap.newKeySet())
                    .add(entry.getKey());
            }
        }
    }
}
