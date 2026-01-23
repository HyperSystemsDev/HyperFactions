package com.hyperfactions.storage.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
        obj.addProperty("chunkX", zone.chunkX());
        obj.addProperty("chunkZ", zone.chunkZ());
        obj.addProperty("createdAt", zone.createdAt());
        obj.addProperty("createdBy", zone.createdBy().toString());
        return obj;
    }

    private Zone deserializeZone(JsonObject obj) {
        return new Zone(
            UUID.fromString(obj.get("id").getAsString()),
            obj.get("name").getAsString(),
            ZoneType.valueOf(obj.get("type").getAsString()),
            obj.get("world").getAsString(),
            obj.get("chunkX").getAsInt(),
            obj.get("chunkZ").getAsInt(),
            obj.get("createdAt").getAsLong(),
            UUID.fromString(obj.get("createdBy").getAsString())
        );
    }
}
