package com.hyperfactions.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hyperfactions.config.HyperFactionsConfig;
import com.hyperfactions.data.JoinRequest;
import com.hyperfactions.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages join requests from players wanting to join closed factions.
 * Uses dual-indexed storage for efficient lookups by player and faction.
 * Persists requests to JSON file.
 */
public class JoinRequestManager {

    /**
     * Result of creating or processing a join request.
     */
    public enum RequestResult {
        SUCCESS,
        ALREADY_REQUESTED,
        PLAYER_IN_FACTION,
        FACTION_NOT_FOUND,
        FACTION_IS_OPEN,
        REQUEST_NOT_FOUND
    }

    // Requests by player: player UUID -> set of requests
    private final Map<UUID, Set<JoinRequest>> requestsByPlayer = new ConcurrentHashMap<>();

    // Requests by faction: faction ID -> set of player UUIDs who requested
    private final Map<UUID, Set<UUID>> requestsByFaction = new ConcurrentHashMap<>();

    // Persistence
    private final Path dataFile;
    private final Gson gson;

    /**
     * Creates a new JoinRequestManager with persistence.
     *
     * @param dataDir the data directory for storage
     */
    public JoinRequestManager(@NotNull Path dataDir) {
        this.dataFile = dataDir.resolve("join_requests.json");
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    }

    /**
     * Initializes the manager by loading persisted requests.
     */
    public void init() {
        load();
    }

    /**
     * Shuts down the manager, saving any pending data.
     */
    public void shutdown() {
        save();
    }

    /**
     * Creates a new join request.
     *
     * @param factionId  the faction ID
     * @param playerUuid the player's UUID
     * @param playerName the player's name
     * @param message    optional intro message
     * @return the created request
     */
    @NotNull
    public JoinRequest createRequest(@NotNull UUID factionId, @NotNull UUID playerUuid,
                                      @NotNull String playerName, @Nullable String message) {
        // Remove any existing request to this faction
        removeRequestInternal(factionId, playerUuid);

        long durationMs = HyperFactionsConfig.get().getJoinRequestExpirationMs();
        JoinRequest request = JoinRequest.create(factionId, playerUuid, playerName, message, durationMs);

        requestsByPlayer.computeIfAbsent(playerUuid, k -> ConcurrentHashMap.newKeySet())
            .add(request);

        requestsByFaction.computeIfAbsent(factionId, k -> ConcurrentHashMap.newKeySet())
            .add(playerUuid);

        save();
        return request;
    }

    /**
     * Gets a request from a specific player for a faction.
     *
     * @param factionId  the faction ID
     * @param playerUuid the player's UUID
     * @return the request, or null if not found or expired
     */
    @Nullable
    public JoinRequest getRequest(@NotNull UUID factionId, @NotNull UUID playerUuid) {
        Set<JoinRequest> requests = requestsByPlayer.get(playerUuid);
        if (requests == null) {
            return null;
        }

        for (JoinRequest request : requests) {
            if (request.factionId().equals(factionId)) {
                if (request.isExpired()) {
                    removeRequest(factionId, playerUuid);
                    return null;
                }
                return request;
            }
        }
        return null;
    }

    /**
     * Gets all pending requests for a faction.
     *
     * @param factionId the faction ID
     * @return list of non-expired requests
     */
    @NotNull
    public List<JoinRequest> getFactionRequests(@NotNull UUID factionId) {
        Set<UUID> playerUuids = requestsByFaction.get(factionId);
        if (playerUuids == null || playerUuids.isEmpty()) {
            return Collections.emptyList();
        }

        List<JoinRequest> valid = new ArrayList<>();
        Set<UUID> expired = new HashSet<>();

        for (UUID playerUuid : playerUuids) {
            JoinRequest request = getRequest(factionId, playerUuid);
            if (request != null && !request.isExpired()) {
                valid.add(request);
            } else {
                expired.add(playerUuid);
            }
        }

        // Clean up expired
        for (UUID playerUuid : expired) {
            removeRequestInternal(factionId, playerUuid);
        }
        if (!expired.isEmpty()) {
            save();
        }

        // Sort by creation time (oldest first)
        valid.sort(Comparator.comparingLong(JoinRequest::createdAt));
        return valid;
    }

    /**
     * Gets the count of pending requests for a faction.
     *
     * @param factionId the faction ID
     * @return count of non-expired requests
     */
    public int getFactionRequestCount(@NotNull UUID factionId) {
        return getFactionRequests(factionId).size();
    }

    /**
     * Gets all pending requests from a player.
     *
     * @param playerUuid the player's UUID
     * @return list of non-expired requests
     */
    @NotNull
    public List<JoinRequest> getPlayerRequests(@NotNull UUID playerUuid) {
        Set<JoinRequest> requests = requestsByPlayer.get(playerUuid);
        if (requests == null) {
            return Collections.emptyList();
        }

        // Filter out expired and return
        List<JoinRequest> valid = requests.stream()
            .filter(r -> !r.isExpired())
            .collect(Collectors.toList());

        // Clean up expired
        if (valid.size() != requests.size()) {
            requests.removeIf(JoinRequest::isExpired);
            save();
        }

        return valid;
    }

    /**
     * Checks if a player has a pending request to a faction.
     *
     * @param factionId  the faction ID
     * @param playerUuid the player's UUID
     * @return true if has valid request
     */
    public boolean hasRequest(@NotNull UUID factionId, @NotNull UUID playerUuid) {
        return getRequest(factionId, playerUuid) != null;
    }

    /**
     * Accepts a join request and returns the request for processing.
     * The caller should handle actually adding the player to the faction.
     *
     * @param factionId  the faction ID
     * @param playerUuid the player's UUID
     * @return the accepted request, or null if not found
     */
    @Nullable
    public JoinRequest acceptRequest(@NotNull UUID factionId, @NotNull UUID playerUuid) {
        JoinRequest request = getRequest(factionId, playerUuid);
        if (request != null) {
            removeRequest(factionId, playerUuid);
        }
        return request;
    }

    /**
     * Declines a join request.
     *
     * @param factionId  the faction ID
     * @param playerUuid the player's UUID
     */
    public void declineRequest(@NotNull UUID factionId, @NotNull UUID playerUuid) {
        removeRequest(factionId, playerUuid);
    }

    /**
     * Removes a request.
     *
     * @param factionId  the faction ID
     * @param playerUuid the player's UUID
     */
    public void removeRequest(@NotNull UUID factionId, @NotNull UUID playerUuid) {
        removeRequestInternal(factionId, playerUuid);
        save();
    }

    /**
     * Internal remove without save (for batch operations).
     */
    private void removeRequestInternal(@NotNull UUID factionId, @NotNull UUID playerUuid) {
        Set<JoinRequest> requests = requestsByPlayer.get(playerUuid);
        if (requests != null) {
            requests.removeIf(r -> r.factionId().equals(factionId));
            if (requests.isEmpty()) {
                requestsByPlayer.remove(playerUuid);
            }
        }

        Set<UUID> factionRequests = requestsByFaction.get(factionId);
        if (factionRequests != null) {
            factionRequests.remove(playerUuid);
            if (factionRequests.isEmpty()) {
                requestsByFaction.remove(factionId);
            }
        }
    }

    /**
     * Removes all requests from a player.
     * Called when a player joins any faction.
     *
     * @param playerUuid the player's UUID
     */
    public void clearPlayerRequests(@NotNull UUID playerUuid) {
        Set<JoinRequest> requests = requestsByPlayer.remove(playerUuid);
        if (requests != null) {
            for (JoinRequest request : requests) {
                Set<UUID> factionRequests = requestsByFaction.get(request.factionId());
                if (factionRequests != null) {
                    factionRequests.remove(playerUuid);
                    if (factionRequests.isEmpty()) {
                        requestsByFaction.remove(request.factionId());
                    }
                }
            }
            save();
        }
    }

    /**
     * Removes all requests to a faction.
     * Called when a faction disbands.
     *
     * @param factionId the faction ID
     */
    public void clearFactionRequests(@NotNull UUID factionId) {
        Set<UUID> requesters = requestsByFaction.remove(factionId);
        if (requesters != null) {
            for (UUID playerUuid : requesters) {
                Set<JoinRequest> requests = requestsByPlayer.get(playerUuid);
                if (requests != null) {
                    requests.removeIf(r -> r.factionId().equals(factionId));
                    if (requests.isEmpty()) {
                        requestsByPlayer.remove(playerUuid);
                    }
                }
            }
            save();
        }
    }

    /**
     * Cleans up all expired requests.
     * Call periodically.
     */
    public void cleanupExpired() {
        boolean changed = false;

        for (Map.Entry<UUID, Set<JoinRequest>> entry : requestsByPlayer.entrySet()) {
            int before = entry.getValue().size();
            entry.getValue().removeIf(JoinRequest::isExpired);
            if (entry.getValue().size() != before) {
                changed = true;
            }
            if (entry.getValue().isEmpty()) {
                requestsByPlayer.remove(entry.getKey());
            }
        }

        // Rebuild faction index
        requestsByFaction.clear();
        for (Map.Entry<UUID, Set<JoinRequest>> entry : requestsByPlayer.entrySet()) {
            for (JoinRequest request : entry.getValue()) {
                requestsByFaction.computeIfAbsent(request.factionId(), k -> ConcurrentHashMap.newKeySet())
                    .add(entry.getKey());
            }
        }

        if (changed) {
            save();
        }
    }

    // === Persistence ===

    /**
     * Loads requests from the JSON file.
     */
    private void load() {
        if (!Files.exists(dataFile)) {
            Logger.info("No join requests file found, starting fresh");
            return;
        }

        try {
            String json = Files.readString(dataFile);
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();

            int loaded = 0;
            int expired = 0;

            for (JsonElement el : array) {
                JoinRequest request = deserializeRequest(el.getAsJsonObject());
                if (request.isExpired()) {
                    expired++;
                    continue;
                }

                requestsByPlayer.computeIfAbsent(request.playerUuid(), k -> ConcurrentHashMap.newKeySet())
                    .add(request);
                requestsByFaction.computeIfAbsent(request.factionId(), k -> ConcurrentHashMap.newKeySet())
                    .add(request.playerUuid());
                loaded++;
            }

            Logger.info("Loaded %d join requests (%d expired and skipped)", loaded, expired);
        } catch (Exception e) {
            Logger.severe("Failed to load join requests", e);
        }
    }

    /**
     * Saves all requests to the JSON file.
     */
    private void save() {
        try {
            Files.createDirectories(dataFile.getParent());

            JsonArray array = new JsonArray();
            for (Set<JoinRequest> requests : requestsByPlayer.values()) {
                for (JoinRequest request : requests) {
                    if (!request.isExpired()) {
                        array.add(serializeRequest(request));
                    }
                }
            }

            Files.writeString(dataFile, gson.toJson(array));
        } catch (IOException e) {
            Logger.severe("Failed to save join requests", e);
        }
    }

    private JsonObject serializeRequest(JoinRequest request) {
        JsonObject obj = new JsonObject();
        obj.addProperty("factionId", request.factionId().toString());
        obj.addProperty("playerUuid", request.playerUuid().toString());
        obj.addProperty("playerName", request.playerName());
        if (request.message() != null) {
            obj.addProperty("message", request.message());
        }
        obj.addProperty("createdAt", request.createdAt());
        obj.addProperty("expiresAt", request.expiresAt());
        return obj;
    }

    private JoinRequest deserializeRequest(JsonObject obj) {
        return new JoinRequest(
            UUID.fromString(obj.get("factionId").getAsString()),
            UUID.fromString(obj.get("playerUuid").getAsString()),
            obj.get("playerName").getAsString(),
            obj.has("message") ? obj.get("message").getAsString() : null,
            obj.get("createdAt").getAsLong(),
            obj.get("expiresAt").getAsLong()
        );
    }
}
