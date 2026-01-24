package com.hyperfactions.manager;

import com.hyperfactions.data.*;
import com.hyperfactions.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages faction relations (ally, enemy, neutral).
 */
public class RelationManager {

    private final FactionManager factionManager;

    // Pending ally requests: target faction -> (requesting faction -> requester player UUID)
    private final Map<UUID, Map<UUID, UUID>> pendingAllyRequests = new ConcurrentHashMap<>();

    public RelationManager(@NotNull FactionManager factionManager) {
        this.factionManager = factionManager;
    }

    /**
     * Result of a relation operation.
     */
    public enum RelationResult {
        SUCCESS,
        NOT_IN_FACTION,
        NOT_OFFICER,
        FACTION_NOT_FOUND,
        CANNOT_RELATE_SELF,
        ALREADY_ALLY,
        ALREADY_ENEMY,
        ALREADY_NEUTRAL,
        REQUEST_SENT,
        REQUEST_ACCEPTED,
        NO_PENDING_REQUEST
    }

    // === Queries ===

    /**
     * Gets the relation between two factions.
     *
     * @param factionId1 first faction ID
     * @param factionId2 second faction ID
     * @return the relation type
     */
    @NotNull
    public RelationType getRelation(@NotNull UUID factionId1, @NotNull UUID factionId2) {
        if (factionId1.equals(factionId2)) {
            return RelationType.ALLY; // Same faction is always friendly
        }

        Faction faction = factionManager.getFaction(factionId1);
        if (faction == null) {
            return RelationType.NEUTRAL;
        }

        return faction.getRelationType(factionId2);
    }

    /**
     * Gets the relation between two players based on their factions.
     *
     * @param player1 first player UUID
     * @param player2 second player UUID
     * @return the relation type, or null if either has no faction
     */
    @Nullable
    public RelationType getPlayerRelation(@NotNull UUID player1, @NotNull UUID player2) {
        UUID faction1 = factionManager.getPlayerFactionId(player1);
        UUID faction2 = factionManager.getPlayerFactionId(player2);

        if (faction1 == null || faction2 == null) {
            return null;
        }

        if (faction1.equals(faction2)) {
            return RelationType.ALLY; // Same faction
        }

        return getRelation(faction1, faction2);
    }

    /**
     * Checks if two factions are allies.
     *
     * @param factionId1 first faction ID
     * @param factionId2 second faction ID
     * @return true if allies
     */
    public boolean areAllies(@NotNull UUID factionId1, @NotNull UUID factionId2) {
        return getRelation(factionId1, factionId2) == RelationType.ALLY;
    }

    /**
     * Checks if two factions are enemies.
     *
     * @param factionId1 first faction ID
     * @param factionId2 second faction ID
     * @return true if enemies
     */
    public boolean areEnemies(@NotNull UUID factionId1, @NotNull UUID factionId2) {
        return getRelation(factionId1, factionId2) == RelationType.ENEMY;
    }

    /**
     * Checks if two players are in allied factions.
     *
     * @param player1 first player UUID
     * @param player2 second player UUID
     * @return true if allies or same faction
     */
    public boolean arePlayersAllied(@NotNull UUID player1, @NotNull UUID player2) {
        if (factionManager.areInSameFaction(player1, player2)) {
            return true;
        }

        RelationType relation = getPlayerRelation(player1, player2);
        return relation == RelationType.ALLY;
    }

    /**
     * Checks if there's a pending ally request.
     *
     * @param fromFactionId the requesting faction
     * @param toFactionId   the target faction
     * @return true if pending
     */
    public boolean hasPendingRequest(@NotNull UUID fromFactionId, @NotNull UUID toFactionId) {
        Map<UUID, UUID> pending = pendingAllyRequests.get(toFactionId);
        return pending != null && pending.containsKey(fromFactionId);
    }

    /**
     * Gets all pending ally requests for a faction.
     *
     * @param factionId the faction ID
     * @return set of requesting faction IDs
     */
    @NotNull
    public Set<UUID> getPendingRequests(@NotNull UUID factionId) {
        Map<UUID, UUID> pending = pendingAllyRequests.get(factionId);
        return pending != null ? Collections.unmodifiableSet(pending.keySet()) : Collections.emptySet();
    }

    // === Operations ===

    /**
     * Sends an ally request or accepts if one is pending from target.
     *
     * @param actorUuid    the actor's UUID
     * @param targetFactionId the target faction ID
     * @return the result
     */
    public RelationResult requestAlly(@NotNull UUID actorUuid, @NotNull UUID targetFactionId) {
        Faction actorFaction = factionManager.getPlayerFaction(actorUuid);
        if (actorFaction == null) {
            return RelationResult.NOT_IN_FACTION;
        }

        var member = actorFaction.getMember(actorUuid);
        if (member == null || !member.isOfficerOrHigher()) {
            return RelationResult.NOT_OFFICER;
        }

        if (actorFaction.id().equals(targetFactionId)) {
            return RelationResult.CANNOT_RELATE_SELF;
        }

        Faction targetFaction = factionManager.getFaction(targetFactionId);
        if (targetFaction == null) {
            return RelationResult.FACTION_NOT_FOUND;
        }

        // Check if already allies
        if (actorFaction.isAlly(targetFactionId)) {
            return RelationResult.ALREADY_ALLY;
        }

        // Check if target has a pending request from us
        if (hasPendingRequest(targetFactionId, actorFaction.id())) {
            // Accept the existing request
            return acceptAlly(actorUuid, targetFactionId);
        }

        // Send new request (store requester's player UUID for logging)
        pendingAllyRequests.computeIfAbsent(targetFactionId, k -> new ConcurrentHashMap<>())
            .put(actorFaction.id(), actorUuid);

        Logger.info("Faction '%s' sent ally request to '%s'", actorFaction.name(), targetFaction.name());
        return RelationResult.REQUEST_SENT;
    }

    /**
     * Accepts a pending ally request.
     *
     * @param actorUuid       the actor's UUID
     * @param fromFactionId   the faction that sent the request
     * @return the result
     */
    public RelationResult acceptAlly(@NotNull UUID actorUuid, @NotNull UUID fromFactionId) {
        Faction actorFaction = factionManager.getPlayerFaction(actorUuid);
        if (actorFaction == null) {
            return RelationResult.NOT_IN_FACTION;
        }

        var member = actorFaction.getMember(actorUuid);
        if (member == null || !member.isOfficerOrHigher()) {
            return RelationResult.NOT_OFFICER;
        }

        Map<UUID, UUID> pending = pendingAllyRequests.get(actorFaction.id());
        if (pending == null || !pending.containsKey(fromFactionId)) {
            return RelationResult.NO_PENDING_REQUEST;
        }

        // Get the original requester's UUID for logging
        UUID requesterUuid = pending.get(fromFactionId);

        Faction fromFaction = factionManager.getFaction(fromFactionId);
        if (fromFaction == null) {
            pending.remove(fromFactionId);
            return RelationResult.FACTION_NOT_FOUND;
        }

        // Set mutual ally relation (both sides get proper actor attribution)
        setRelation(actorFaction.id(), fromFactionId, RelationType.ALLY, actorUuid);
        setRelation(fromFactionId, actorFaction.id(), RelationType.ALLY, requesterUuid);

        // Remove pending request
        pending.remove(fromFactionId);

        Logger.info("Factions '%s' and '%s' are now allies", actorFaction.name(), fromFaction.name());
        return RelationResult.REQUEST_ACCEPTED;
    }

    /**
     * Sets another faction as enemy.
     *
     * @param actorUuid       the actor's UUID
     * @param targetFactionId the target faction ID
     * @return the result
     */
    public RelationResult setEnemy(@NotNull UUID actorUuid, @NotNull UUID targetFactionId) {
        Faction actorFaction = factionManager.getPlayerFaction(actorUuid);
        if (actorFaction == null) {
            return RelationResult.NOT_IN_FACTION;
        }

        var member = actorFaction.getMember(actorUuid);
        if (member == null || !member.isOfficerOrHigher()) {
            return RelationResult.NOT_OFFICER;
        }

        if (actorFaction.id().equals(targetFactionId)) {
            return RelationResult.CANNOT_RELATE_SELF;
        }

        Faction targetFaction = factionManager.getFaction(targetFactionId);
        if (targetFaction == null) {
            return RelationResult.FACTION_NOT_FOUND;
        }

        if (actorFaction.isEnemy(targetFactionId)) {
            return RelationResult.ALREADY_ENEMY;
        }

        // Remove any pending ally requests
        Map<UUID, UUID> pending = pendingAllyRequests.get(actorFaction.id());
        if (pending != null) {
            pending.remove(targetFactionId);
        }
        pending = pendingAllyRequests.get(targetFactionId);
        if (pending != null) {
            pending.remove(actorFaction.id());
        }

        setRelation(actorFaction.id(), targetFactionId, RelationType.ENEMY, actorUuid);

        Logger.info("Faction '%s' declared '%s' as enemy", actorFaction.name(), targetFaction.name());
        return RelationResult.SUCCESS;
    }

    /**
     * Sets another faction as neutral.
     *
     * @param actorUuid       the actor's UUID
     * @param targetFactionId the target faction ID
     * @return the result
     */
    public RelationResult setNeutral(@NotNull UUID actorUuid, @NotNull UUID targetFactionId) {
        Faction actorFaction = factionManager.getPlayerFaction(actorUuid);
        if (actorFaction == null) {
            return RelationResult.NOT_IN_FACTION;
        }

        var member = actorFaction.getMember(actorUuid);
        if (member == null || !member.isOfficerOrHigher()) {
            return RelationResult.NOT_OFFICER;
        }

        if (actorFaction.id().equals(targetFactionId)) {
            return RelationResult.CANNOT_RELATE_SELF;
        }

        Faction targetFaction = factionManager.getFaction(targetFactionId);
        if (targetFaction == null) {
            return RelationResult.FACTION_NOT_FOUND;
        }

        RelationType currentRelation = actorFaction.getRelationType(targetFactionId);
        if (currentRelation == RelationType.NEUTRAL) {
            return RelationResult.ALREADY_NEUTRAL;
        }

        // If breaking alliance, update both sides
        if (currentRelation == RelationType.ALLY) {
            setRelation(targetFactionId, actorFaction.id(), RelationType.NEUTRAL, null);
        }

        setRelation(actorFaction.id(), targetFactionId, RelationType.NEUTRAL, actorUuid);

        Logger.info("Faction '%s' set '%s' as neutral", actorFaction.name(), targetFaction.name());
        return RelationResult.SUCCESS;
    }

    /**
     * Internal method to set a relation.
     */
    private void setRelation(@NotNull UUID factionId, @NotNull UUID targetId,
                             @NotNull RelationType type, @Nullable UUID actorUuid) {
        Faction faction = factionManager.getFaction(factionId);
        if (faction == null) return;

        Faction target = factionManager.getFaction(targetId);
        String targetName = target != null ? target.name() : "Unknown";

        FactionRelation relation = FactionRelation.create(targetId, type);
        FactionLog.LogType logType = switch (type) {
            case ALLY -> FactionLog.LogType.RELATION_ALLY;
            case ENEMY -> FactionLog.LogType.RELATION_ENEMY;
            case NEUTRAL -> FactionLog.LogType.RELATION_NEUTRAL;
        };

        Faction updated = faction.withRelation(relation)
            .withLog(FactionLog.create(logType, "Set " + targetName + " as " + type.getDisplayName(), actorUuid));

        factionManager.updateFaction(updated);
    }

    /**
     * Clears all relations for a faction (used when disbanding).
     *
     * @param factionId the faction ID
     */
    public void clearAllRelations(@NotNull UUID factionId) {
        // Remove pending requests
        pendingAllyRequests.remove(factionId);
        for (Map<UUID, UUID> requests : pendingAllyRequests.values()) {
            requests.remove(factionId);
        }

        // Remove relations from other factions pointing to this one
        for (Faction faction : factionManager.getAllFactions()) {
            if (faction.getRelation(factionId) != null) {
                FactionRelation neutral = FactionRelation.create(factionId, RelationType.NEUTRAL);
                Faction updated = faction.withRelation(neutral);
                factionManager.updateFaction(updated);
            }
        }
    }
}
