package com.hyperfactions.storage.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hyperfactions.data.ChunkKey;
import com.hyperfactions.data.Zone;
import com.hyperfactions.data.ZoneType;
import com.hyperfactions.storage.ZoneStorage;
import com.hyperfactions.util.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * JSON file-based implementation of ZoneStorage.
 * Stores all zones in a single file: data/zones.json
 *
 * Supports both old single-chunk format (for migration) and new multi-chunk format.
 */
public class JsonZoneStorage implements ZoneStorage {

    private final Path dataDir;
    private final Path zonesFile;
    private final Gson gson;

    public JsonZoneStorage(@NotNull Path dataDir) {
        this.dataDir = dataDir;
        this.zonesFile = dataDir.resolve("zones.json");
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    }

    @Override
    public CompletableFuture<Void> init() {
        return CompletableFuture.runAsync(() -> {
            try {
                Files.createDirectories(dataDir);
                Logger.info("Zone storage initialized");
            } catch (IOException e) {
                Logger.severe("Failed to create data directory", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Collection<Zone>> loadAllZones() {
        return CompletableFuture.supplyAsync(() -> {
            List<Zone> zones = new ArrayList<>();

            if (!Files.exists(zonesFile)) {
                return zones;
            }

            try {
                String json = Files.readString(zonesFile);
                JsonArray array = JsonParser.parseString(json).getAsJsonArray();

                for (JsonElement el : array) {
                    try {
                        zones.add(deserializeZone(el.getAsJsonObject()));
                    } catch (Exception e) {
                        Logger.warn("Failed to parse zone: %s", e.getMessage());
                    }
                }
            } catch (Exception e) {
                Logger.severe("Failed to load zones", e);
            }

            Logger.info("Loaded %d zones", zones.size());
            return zones;
        });
    }

    @Override
    public CompletableFuture<Void> saveAllZones(@NotNull Collection<Zone> zones) {
        return CompletableFuture.runAsync(() -> {
            try {
                JsonArray array = new JsonArray();
                for (Zone zone : zones) {
                    array.add(serializeZone(zone));
                }
                Files.writeString(zonesFile, gson.toJson(array));
            } catch (IOException e) {
                Logger.severe("Failed to save zones", e);
            }
        });
    }

    private JsonObject serializeZone(Zone zone) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", zone.id().toString());
        obj.addProperty("name", zone.name());
        obj.addProperty("type", zone.type().name());
        obj.addProperty("world", zone.world());

        // Serialize chunks as array
        JsonArray chunksArray = new JsonArray();
        for (ChunkKey chunk : zone.chunks()) {
            JsonObject chunkObj = new JsonObject();
            chunkObj.addProperty("x", chunk.chunkX());
            chunkObj.addProperty("z", chunk.chunkZ());
            chunksArray.add(chunkObj);
        }
        obj.add("chunks", chunksArray);

        obj.addProperty("createdAt", zone.createdAt());
        obj.addProperty("createdBy", zone.createdBy().toString());

        // Serialize flags if present
        if (zone.flags() != null && !zone.flags().isEmpty()) {
            JsonObject flagsObj = new JsonObject();
            for (Map.Entry<String, Boolean> entry : zone.flags().entrySet()) {
                flagsObj.addProperty(entry.getKey(), entry.getValue());
            }
            obj.add("flags", flagsObj);
        }

        return obj;
    }

    private Zone deserializeZone(JsonObject obj) {
        UUID id = UUID.fromString(obj.get("id").getAsString());
        String name = obj.get("name").getAsString();
        ZoneType type = ZoneType.valueOf(obj.get("type").getAsString());
        String world = obj.get("world").getAsString();
        long createdAt = obj.get("createdAt").getAsLong();
        UUID createdBy = UUID.fromString(obj.get("createdBy").getAsString());

        // Deserialize chunks - support both old and new format
        Set<ChunkKey> chunks = new HashSet<>();

        if (obj.has("chunks") && obj.get("chunks").isJsonArray()) {
            // New multi-chunk format
            JsonArray chunksArray = obj.getAsJsonArray("chunks");
            for (JsonElement el : chunksArray) {
                JsonObject chunkObj = el.getAsJsonObject();
                int x = chunkObj.get("x").getAsInt();
                int z = chunkObj.get("z").getAsInt();
                chunks.add(new ChunkKey(world, x, z));
            }
        } else if (obj.has("chunkX") && obj.has("chunkZ")) {
            // Old single-chunk format (migration support)
            int chunkX = obj.get("chunkX").getAsInt();
            int chunkZ = obj.get("chunkZ").getAsInt();
            chunks.add(new ChunkKey(world, chunkX, chunkZ));
            Logger.info("Migrated zone '%s' from single-chunk to multi-chunk format", name);
        }

        // Deserialize flags if present
        Map<String, Boolean> flags = null;
        if (obj.has("flags") && obj.get("flags").isJsonObject()) {
            flags = new HashMap<>();
            JsonObject flagsObj = obj.getAsJsonObject("flags");
            for (String key : flagsObj.keySet()) {
                flags.put(key, flagsObj.get(key).getAsBoolean());
            }
        }

        return new Zone(id, name, type, world, chunks, createdAt, createdBy, flags);
    }
}
