package com.hyperfactions.storage.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hyperfactions.data.*;
import com.hyperfactions.storage.FactionStorage;
import com.hyperfactions.util.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * JSON file-based implementation of FactionStorage.
 * Stores each faction in its own file: data/factions/{uuid}.json
 */
public class JsonFactionStorage implements FactionStorage {

    private final Path dataDir;
    private final Path factionsDir;
    private final Gson gson;

    public JsonFactionStorage(@NotNull Path dataDir) {
        this.dataDir = dataDir;
        this.factionsDir = dataDir.resolve("factions");
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    }

    @Override
    public CompletableFuture<Void> init() {
        return CompletableFuture.runAsync(() -> {
            try {
                Files.createDirectories(factionsDir);
                Logger.info("Faction storage initialized at %s", factionsDir);
            } catch (IOException e) {
                Logger.severe("Failed to create factions directory", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Optional<Faction>> loadFaction(@NotNull UUID factionId) {
        return CompletableFuture.supplyAsync(() -> {
            Path file = factionsDir.resolve(factionId + ".json");
            if (!Files.exists(file)) {
                return Optional.empty();
            }

            try {
                String json = Files.readString(file);
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                return Optional.of(deserializeFaction(obj));
            } catch (Exception e) {
                Logger.severe("Failed to load faction %s", e, factionId);
                return Optional.empty();
            }
        });
    }

    @Override
    public CompletableFuture<Void> saveFaction(@NotNull Faction faction) {
        return CompletableFuture.runAsync(() -> {
            Path file = factionsDir.resolve(faction.id() + ".json");
            try {
                JsonObject obj = serializeFaction(faction);
                Files.writeString(file, gson.toJson(obj));
            } catch (IOException e) {
                Logger.severe("Failed to save faction %s", e, faction.name());
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteFaction(@NotNull UUID factionId) {
        return CompletableFuture.runAsync(() -> {
            Path file = factionsDir.resolve(factionId + ".json");
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                Logger.severe("Failed to delete faction %s", e, factionId);
            }
        });
    }

    @Override
    public CompletableFuture<Collection<Faction>> loadAllFactions() {
        return CompletableFuture.supplyAsync(() -> {
            List<Faction> factions = new ArrayList<>();

            if (!Files.exists(factionsDir)) {
                return factions;
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(factionsDir, "*.json")) {
                for (Path file : stream) {
                    try {
                        String json = Files.readString(file);
                        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                        factions.add(deserializeFaction(obj));
                    } catch (Exception e) {
                        Logger.warn("Failed to load faction file %s: %s", file.getFileName(), e.getMessage());
                    }
                }
            } catch (IOException e) {
                Logger.severe("Failed to read factions directory", e);
            }

            Logger.info("Loaded %d factions", factions.size());
            return factions;
        });
    }

    // === Serialization ===

    private JsonObject serializeFaction(Faction faction) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", faction.id().toString());
        obj.addProperty("name", faction.name());
        if (faction.description() != null) {
            obj.addProperty("description", faction.description());
        }
        obj.addProperty("color", faction.color());
        obj.addProperty("createdAt", faction.createdAt());
        obj.addProperty("open", faction.open());

        // Home
        if (faction.home() != null) {
            obj.add("home", serializeHome(faction.home()));
        }

        // Members
        JsonArray members = new JsonArray();
        for (FactionMember member : faction.members().values()) {
            members.add(serializeMember(member));
        }
        obj.add("members", members);

        // Claims
        JsonArray claims = new JsonArray();
        for (FactionClaim claim : faction.claims()) {
            claims.add(serializeClaim(claim));
        }
        obj.add("claims", claims);

        // Relations
        JsonArray relations = new JsonArray();
        for (FactionRelation rel : faction.relations().values()) {
            relations.add(serializeRelation(rel));
        }
        obj.add("relations", relations);

        // Logs (only save last 50 to keep file size reasonable)
        JsonArray logs = new JsonArray();
        int logCount = Math.min(faction.logs().size(), 50);
        for (int i = 0; i < logCount; i++) {
            logs.add(serializeLog(faction.logs().get(i)));
        }
        obj.add("logs", logs);

        return obj;
    }

    private JsonObject serializeHome(Faction.FactionHome home) {
        JsonObject obj = new JsonObject();
        obj.addProperty("world", home.world());
        obj.addProperty("x", home.x());
        obj.addProperty("y", home.y());
        obj.addProperty("z", home.z());
        obj.addProperty("yaw", home.yaw());
        obj.addProperty("pitch", home.pitch());
        obj.addProperty("setAt", home.setAt());
        obj.addProperty("setBy", home.setBy().toString());
        return obj;
    }

    private JsonObject serializeMember(FactionMember member) {
        JsonObject obj = new JsonObject();
        obj.addProperty("uuid", member.uuid().toString());
        obj.addProperty("username", member.username());
        obj.addProperty("role", member.role().name());
        obj.addProperty("joinedAt", member.joinedAt());
        obj.addProperty("lastOnline", member.lastOnline());
        return obj;
    }

    private JsonObject serializeClaim(FactionClaim claim) {
        JsonObject obj = new JsonObject();
        obj.addProperty("world", claim.world());
        obj.addProperty("chunkX", claim.chunkX());
        obj.addProperty("chunkZ", claim.chunkZ());
        obj.addProperty("claimedAt", claim.claimedAt());
        obj.addProperty("claimedBy", claim.claimedBy().toString());
        return obj;
    }

    private JsonObject serializeRelation(FactionRelation rel) {
        JsonObject obj = new JsonObject();
        obj.addProperty("targetFactionId", rel.targetFactionId().toString());
        obj.addProperty("type", rel.type().name());
        obj.addProperty("since", rel.since());
        return obj;
    }

    private JsonObject serializeLog(FactionLog log) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", log.type().name());
        obj.addProperty("message", log.message());
        obj.addProperty("timestamp", log.timestamp());
        if (log.actorUuid() != null) {
            obj.addProperty("actorUuid", log.actorUuid().toString());
        }
        return obj;
    }

    // === Deserialization ===

    private Faction deserializeFaction(JsonObject obj) {
        UUID id = UUID.fromString(obj.get("id").getAsString());
        String name = obj.get("name").getAsString();
        String description = obj.has("description") ? obj.get("description").getAsString() : null;
        String color = obj.has("color") ? obj.get("color").getAsString() : "f";
        long createdAt = obj.get("createdAt").getAsLong();
        boolean open = obj.has("open") && obj.get("open").getAsBoolean();

        // Home
        Faction.FactionHome home = null;
        if (obj.has("home") && obj.get("home").isJsonObject()) {
            home = deserializeHome(obj.getAsJsonObject("home"));
        }

        // Members
        Map<UUID, FactionMember> members = new HashMap<>();
        if (obj.has("members")) {
            for (JsonElement el : obj.getAsJsonArray("members")) {
                FactionMember member = deserializeMember(el.getAsJsonObject());
                members.put(member.uuid(), member);
            }
        }

        // Claims
        Set<FactionClaim> claims = new HashSet<>();
        if (obj.has("claims")) {
            for (JsonElement el : obj.getAsJsonArray("claims")) {
                claims.add(deserializeClaim(el.getAsJsonObject()));
            }
        }

        // Relations
        Map<UUID, FactionRelation> relations = new HashMap<>();
        if (obj.has("relations")) {
            for (JsonElement el : obj.getAsJsonArray("relations")) {
                FactionRelation rel = deserializeRelation(el.getAsJsonObject());
                relations.put(rel.targetFactionId(), rel);
            }
        }

        // Logs
        List<FactionLog> logs = new ArrayList<>();
        if (obj.has("logs")) {
            for (JsonElement el : obj.getAsJsonArray("logs")) {
                logs.add(deserializeLog(el.getAsJsonObject()));
            }
        }

        return new Faction(id, name, description, color, createdAt, home, members, claims, relations, logs, open);
    }

    private Faction.FactionHome deserializeHome(JsonObject obj) {
        return new Faction.FactionHome(
            obj.get("world").getAsString(),
            obj.get("x").getAsDouble(),
            obj.get("y").getAsDouble(),
            obj.get("z").getAsDouble(),
            obj.get("yaw").getAsFloat(),
            obj.get("pitch").getAsFloat(),
            obj.get("setAt").getAsLong(),
            UUID.fromString(obj.get("setBy").getAsString())
        );
    }

    private FactionMember deserializeMember(JsonObject obj) {
        return new FactionMember(
            UUID.fromString(obj.get("uuid").getAsString()),
            obj.get("username").getAsString(),
            FactionRole.valueOf(obj.get("role").getAsString()),
            obj.get("joinedAt").getAsLong(),
            obj.get("lastOnline").getAsLong()
        );
    }

    private FactionClaim deserializeClaim(JsonObject obj) {
        return new FactionClaim(
            obj.get("world").getAsString(),
            obj.get("chunkX").getAsInt(),
            obj.get("chunkZ").getAsInt(),
            obj.get("claimedAt").getAsLong(),
            UUID.fromString(obj.get("claimedBy").getAsString())
        );
    }

    private FactionRelation deserializeRelation(JsonObject obj) {
        return new FactionRelation(
            UUID.fromString(obj.get("targetFactionId").getAsString()),
            RelationType.valueOf(obj.get("type").getAsString()),
            obj.get("since").getAsLong()
        );
    }

    private FactionLog deserializeLog(JsonObject obj) {
        UUID actorUuid = obj.has("actorUuid") ? UUID.fromString(obj.get("actorUuid").getAsString()) : null;
        return new FactionLog(
            FactionLog.LogType.valueOf(obj.get("type").getAsString()),
            obj.get("message").getAsString(),
            obj.get("timestamp").getAsLong(),
            actorUuid
        );
    }
}
