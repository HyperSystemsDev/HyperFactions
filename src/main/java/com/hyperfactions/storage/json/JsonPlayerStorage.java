package com.hyperfactions.storage.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hyperfactions.data.PlayerPower;
import com.hyperfactions.storage.PlayerStorage;
import com.hyperfactions.storage.StorageHealth;
import com.hyperfactions.storage.StorageUtils;
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
                // Check if there's a backup we can recover from
                if (StorageUtils.hasBackup(file)) {
                    Logger.warn("Player power file %s missing but backup exists, attempting recovery", uuid);
                    if (StorageUtils.recoverFromBackup(file)) {
                        Logger.info("Successfully recovered player power %s from backup", uuid);
                    } else {
                        return Optional.empty();
                    }
                } else {
                    return Optional.empty();
                }
            }

            try {
                String json = Files.readString(file);
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                return Optional.of(deserializePlayerPower(obj));
            } catch (Exception e) {
                Logger.severe("Failed to load player power %s, attempting backup recovery", e, uuid);
                // Attempt backup recovery on parse failure
                if (StorageUtils.recoverFromBackup(file)) {
                    try {
                        String json = Files.readString(file);
                        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                        Logger.info("Successfully loaded player power %s from recovered backup", uuid);
                        return Optional.of(deserializePlayerPower(obj));
                    } catch (Exception e2) {
                        Logger.severe("Backup recovery failed for player power %s", e2, uuid);
                    }
                }
                return Optional.empty();
            }
        });
    }

    @Override
    public CompletableFuture<Void> savePlayerPower(@NotNull PlayerPower power) {
        return CompletableFuture.runAsync(() -> {
            Path file = playersDir.resolve(power.uuid() + ".json");
            String filePath = file.toString();

            try {
                JsonObject obj = serializePlayerPower(power);
                String content = gson.toJson(obj);

                // Use atomic write for bulletproof data protection
                StorageUtils.WriteResult result = StorageUtils.writeAtomic(file, content);

                if (result instanceof StorageUtils.WriteResult.Success) {
                    StorageHealth.get().recordSuccess(filePath);
                } else if (result instanceof StorageUtils.WriteResult.Failure failure) {
                    StorageHealth.get().recordFailure(filePath, failure.error());
                    Logger.severe("Failed to save player power %s: %s", power.uuid(), failure.error());
                }
            } catch (Exception e) {
                StorageHealth.get().recordFailure(filePath, e.getMessage());
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
            List<String> failedFiles = new ArrayList<>();
            int totalFiles = 0;

            if (!Files.exists(playersDir)) {
                Logger.info("Players directory does not exist yet, no player power to load");
                return powers;
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(playersDir, "*.json")) {
                for (Path file : stream) {
                    totalFiles++;
                    try {
                        String json = Files.readString(file);
                        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                        powers.add(deserializePlayerPower(obj));
                    } catch (Exception e) {
                        failedFiles.add(file.getFileName().toString());
                        Logger.severe("Failed to load player file %s: %s", file.getFileName(), e.getMessage());
                    }
                }
            } catch (IOException e) {
                Logger.severe("CRITICAL: Failed to read players directory - data may be lost!", e);
                throw new RuntimeException("Failed to read players directory", e);
            }

            // Report loading results
            if (!failedFiles.isEmpty()) {
                Logger.severe("WARNING: %d of %d player files failed to load: %s",
                    failedFiles.size(), totalFiles, String.join(", ", failedFiles));
            }

            Logger.info("Loaded %d/%d player power records successfully", powers.size(), totalFiles);
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
