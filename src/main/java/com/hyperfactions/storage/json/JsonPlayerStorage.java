package com.hyperfactions.storage.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hyperfactions.data.PlayerPower;
import com.hyperfactions.storage.PlayerStorage;
import com.hyperfactions.util.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * JSON file-based implementation of PlayerStorage.
 * Stores each player's power data in: data/players/{uuid}.json
 */
public class JsonPlayerStorage implements PlayerStorage {

    private final Path dataDir;
    private final Path playersDir;
    private final Gson gson;

    public JsonPlayerStorage(@NotNull Path dataDir) {
        this.dataDir = dataDir;
        this.playersDir = dataDir.resolve("players");
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    }

    @Override
    public CompletableFuture<Void> init() {
        return CompletableFuture.runAsync(() -> {
            try {
                Files.createDirectories(playersDir);
                Logger.info("Player storage initialized at %s", playersDir);
            } catch (IOException e) {
                Logger.severe("Failed to create players directory", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Optional<PlayerPower>> loadPlayerPower(@NotNull UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Path file = playersDir.resolve(uuid + ".json");
            if (!Files.exists(file)) {
                return Optional.empty();
            }

            try {
                String json = Files.readString(file);
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                return Optional.of(deserializePlayerPower(obj));
            } catch (Exception e) {
                Logger.severe("Failed to load player power %s", e, uuid);
                return Optional.empty();
            }
        });
    }

    @Override
    public CompletableFuture<Void> savePlayerPower(@NotNull PlayerPower power) {
        return CompletableFuture.runAsync(() -> {
            Path file = playersDir.resolve(power.uuid() + ".json");
            try {
                JsonObject obj = serializePlayerPower(power);
                Files.writeString(file, gson.toJson(obj));
            } catch (IOException e) {
                Logger.severe("Failed to save player power %s", e, power.uuid());
            }
        });
    }

    @Override
    public CompletableFuture<Void> deletePlayerPower(@NotNull UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            Path file = playersDir.resolve(uuid + ".json");
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                Logger.severe("Failed to delete player power %s", e, uuid);
            }
        });
    }

    @Override
    public CompletableFuture<Collection<PlayerPower>> loadAllPlayerPower() {
        return CompletableFuture.supplyAsync(() -> {
            List<PlayerPower> powers = new ArrayList<>();

            if (!Files.exists(playersDir)) {
                return powers;
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(playersDir, "*.json")) {
                for (Path file : stream) {
                    try {
                        String json = Files.readString(file);
                        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                        powers.add(deserializePlayerPower(obj));
                    } catch (Exception e) {
                        Logger.warn("Failed to load player file %s: %s", file.getFileName(), e.getMessage());
                    }
                }
            } catch (IOException e) {
                Logger.severe("Failed to read players directory", e);
            }

            Logger.info("Loaded %d player power records", powers.size());
            return powers;
        });
    }

    private JsonObject serializePlayerPower(PlayerPower power) {
        JsonObject obj = new JsonObject();
        obj.addProperty("uuid", power.uuid().toString());
        obj.addProperty("power", power.power());
        obj.addProperty("maxPower", power.maxPower());
        obj.addProperty("lastDeath", power.lastDeath());
        obj.addProperty("lastRegen", power.lastRegen());
        return obj;
    }

    private PlayerPower deserializePlayerPower(JsonObject obj) {
        return new PlayerPower(
            UUID.fromString(obj.get("uuid").getAsString()),
            obj.get("power").getAsDouble(),
            obj.get("maxPower").getAsDouble(),
            obj.get("lastDeath").getAsLong(),
            obj.get("lastRegen").getAsLong()
        );
    }
}
