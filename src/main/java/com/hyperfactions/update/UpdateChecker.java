package com.hyperfactions.update;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hyperfactions.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Checks for plugin updates from the GitHub Releases API.
 * <p>
 * Uses the GitHub API endpoint for the latest release, parsing
 * {@code tag_name} for the version, {@code body} for the changelog,
 * and the first JAR asset's {@code browser_download_url} for the download.
 */
public final class UpdateChecker {

    private static final String USER_AGENT = "HyperFactions-UpdateChecker";
    private static final int TIMEOUT_MS = 10000;
    private static final Gson GSON = new Gson();

    private final Path dataDirectory;
    private final String currentVersion;
    private final String checkUrl;

    private final AtomicReference<UpdateInfo> cachedUpdate = new AtomicReference<>();
    private volatile long lastCheckTime = 0;
    private static final long CACHE_DURATION_MS = 300000; // 5 minutes

    public UpdateChecker(@NotNull Path dataDirectory, @NotNull String currentVersion, @NotNull String checkUrl) {
        this.dataDirectory = dataDirectory;
        this.currentVersion = currentVersion;
        this.checkUrl = checkUrl;
    }

    /**
     * Gets the current plugin version.
     */
    @NotNull
    public String getCurrentVersion() {
        return currentVersion;
    }

    /**
     * Checks for updates asynchronously.
     *
     * @return a future containing update info, or null if up-to-date or error
     */
    public CompletableFuture<UpdateInfo> checkForUpdates() {
        return checkForUpdates(false);
    }

    /**
     * Checks for updates asynchronously.
     *
     * @param forceRefresh if true, ignores cache
     * @return a future containing update info, or null if up-to-date or error
     */
    public CompletableFuture<UpdateInfo> checkForUpdates(boolean forceRefresh) {
        // Check cache first
        if (!forceRefresh && System.currentTimeMillis() - lastCheckTime < CACHE_DURATION_MS) {
            return CompletableFuture.completedFuture(cachedUpdate.get());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                Logger.info("[Update] Checking for updates from %s", checkUrl);

                URL url = URI.create(checkUrl).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setRequestProperty("Accept", "application/vnd.github+json");
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    Logger.warn("[Update] Failed to check for updates: HTTP %d", responseCode);
                    return null;
                }

                // Read response
                String json;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    json = sb.toString();
                }

                // Parse GitHub release JSON
                JsonObject obj = GSON.fromJson(json, JsonObject.class);
                String tagName = obj.get("tag_name").getAsString();
                String latestVersion = tagName.startsWith("v") ? tagName.substring(1) : tagName;
                String changelog = obj.has("body") && !obj.get("body").isJsonNull()
                        ? obj.get("body").getAsString() : null;

                // Find the first .jar asset download URL
                String downloadUrl = null;
                if (obj.has("assets") && obj.get("assets").isJsonArray()) {
                    JsonArray assets = obj.getAsJsonArray("assets");
                    for (int i = 0; i < assets.size(); i++) {
                        JsonObject asset = assets.get(i).getAsJsonObject();
                        String assetName = asset.get("name").getAsString();
                        if (assetName.endsWith(".jar")) {
                            downloadUrl = asset.get("browser_download_url").getAsString();
                            break;
                        }
                    }
                }

                lastCheckTime = System.currentTimeMillis();

                // Compare versions
                if (isNewerVersion(latestVersion, currentVersion)) {
                    UpdateInfo info = new UpdateInfo(latestVersion, downloadUrl, changelog);
                    cachedUpdate.set(info);
                    Logger.info("[Update] New version available: %s (current: %s)", latestVersion, currentVersion);
                    return info;
                } else {
                    cachedUpdate.set(null);
                    Logger.info("[Update] Plugin is up-to-date (v%s)", currentVersion);
                    return null;
                }

            } catch (Exception e) {
                Logger.warn("[Update] Failed to check for updates: %s", e.getMessage());
                return null;
            }
        });
    }

    /**
     * Downloads the update to the mods folder.
     *
     * @param info the update info
     * @return a future containing the downloaded file path, or null on failure
     */
    public CompletableFuture<Path> downloadUpdate(@NotNull UpdateInfo info) {
        if (info.downloadUrl() == null || info.downloadUrl().isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                Logger.info("[Update] Downloading update v%s from %s", info.version(), info.downloadUrl());

                URL url = URI.create(info.downloadUrl()).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(60000); // 60 second read timeout for downloads

                // Follow redirects
                conn.setInstanceFollowRedirects(true);

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    Logger.warn("[Update] Failed to download update: HTTP %d", responseCode);
                    return null;
                }

                // Determine output path
                Path modsFolder = dataDirectory.getParent();
                Path updateFile = modsFolder.resolve("HyperFactions-" + info.version() + ".jar");
                Path currentJar = modsFolder.resolve("HyperFactions-" + currentVersion + ".jar");

                // Download to temp file first
                Path tempFile = modsFolder.resolve("HyperFactions-" + info.version() + ".jar.tmp");

                try (InputStream in = conn.getInputStream()) {
                    Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
                }

                // Verify download (basic check - file size > 0)
                if (Files.size(tempFile) < 1000) {
                    Logger.warn("[Update] Downloaded file seems too small, aborting");
                    Files.deleteIfExists(tempFile);
                    return null;
                }

                // Rename temp to final
                Files.move(tempFile, updateFile, StandardCopyOption.REPLACE_EXISTING);

                // Backup current JAR if it exists
                // On Windows, the JAR may be locked by the JVM - handle gracefully
                if (Files.exists(currentJar)) {
                    Path backupFile = modsFolder.resolve("HyperFactions-" + currentVersion + ".jar.backup");
                    try {
                        Files.move(currentJar, backupFile, StandardCopyOption.REPLACE_EXISTING);
                        Logger.info("[Update] Backed up current JAR to %s", backupFile.getFileName());
                    } catch (java.nio.file.FileSystemException e) {
                        // Windows locks loaded JARs - can't backup while running
                        Logger.warn("[Update] Could not backup old JAR (file in use). Please delete %s manually after restart.", currentJar.getFileName());
                    }
                }

                Logger.info("[Update] Successfully downloaded update to %s", updateFile.getFileName());
                return updateFile;

            } catch (Exception e) {
                Logger.warn("[Update] Failed to download update: %s", e.getMessage());
                e.printStackTrace();
                return null;
            }
        });
    }

    /**
     * Gets the cached update info, if any.
     */
    @Nullable
    public UpdateInfo getCachedUpdate() {
        return cachedUpdate.get();
    }

    /**
     * Checks if there's a cached update available.
     */
    public boolean hasUpdateAvailable() {
        return cachedUpdate.get() != null;
    }

    /**
     * Compares two version strings.
     *
     * @return true if newVersion is newer than oldVersion
     */
    private boolean isNewerVersion(@NotNull String newVersion, @NotNull String oldVersion) {
        try {
            // Remove 'v' prefix if present
            String v1 = newVersion.toLowerCase().startsWith("v") ? newVersion.substring(1) : newVersion;
            String v2 = oldVersion.toLowerCase().startsWith("v") ? oldVersion.substring(1) : oldVersion;

            String[] parts1 = v1.split("\\.");
            String[] parts2 = v2.split("\\.");

            int maxLen = Math.max(parts1.length, parts2.length);
            for (int i = 0; i < maxLen; i++) {
                int num1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
                int num2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;

                if (num1 > num2) return true;
                if (num1 < num2) return false;
            }

            return false; // Equal versions
        } catch (Exception e) {
            // Fallback to string comparison
            return newVersion.compareTo(oldVersion) > 0;
        }
    }

    private int parseVersionPart(@NotNull String part) {
        // Handle versions like "1.0.0-beta1"
        String numPart = part.split("-")[0];
        return Integer.parseInt(numPart);
    }

    /**
     * Record containing update information.
     */
    public record UpdateInfo(
            @NotNull String version,
            @Nullable String downloadUrl,
            @Nullable String changelog
    ) {}
}
