package com.hyperfactions.backup;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.config.HyperFactionsConfig;
import com.hyperfactions.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Manages backup creation, restoration, and GFS (Grandfather-Father-Son) rotation.
 *
 * Backup schedule:
 * - Hourly (Son): Every hour, keep last N hours (default: 24)
 * - Daily (Father): At midnight, keep last N days (default: 7)
 * - Weekly (Grandfather): Sunday midnight, keep last N weeks (default: 4)
 * - Manual: User-created, never auto-deleted
 *
 * Backup contents:
 * - data/factions/ directory
 * - data/players/ directory
 * - data/zones.json
 * - config.json
 */
public class BackupManager {

    /** Result of a backup operation */
    public sealed interface BackupResult permits BackupResult.Success, BackupResult.Failure {
        record Success(@NotNull BackupMetadata metadata, @NotNull Path file) implements BackupResult {}
        record Failure(@NotNull String error) implements BackupResult {}
    }

    /** Result of a restore operation */
    public sealed interface RestoreResult permits RestoreResult.Success, RestoreResult.Failure {
        record Success(@NotNull String backupName, int filesRestored) implements RestoreResult {}
        record Failure(@NotNull String error) implements RestoreResult {}
    }

    private final Path dataDir;
    private final Path backupsDir;
    private final HyperFactions hyperFactions;
    private int scheduledTaskId = -1;

    /**
     * Creates a new BackupManager.
     *
     * @param dataDir       the plugin data directory
     * @param hyperFactions the main plugin instance
     */
    public BackupManager(@NotNull Path dataDir, @NotNull HyperFactions hyperFactions) {
        this.dataDir = dataDir;
        this.backupsDir = dataDir.resolve("backups");
        this.hyperFactions = hyperFactions;
    }

    /**
     * Initializes the backup manager and starts scheduled tasks.
     */
    public void init() {
        try {
            Files.createDirectories(backupsDir);
            Logger.info("[BackupManager] Initialized, backup directory: %s", backupsDir);
        } catch (IOException e) {
            Logger.severe("[BackupManager] Failed to create backups directory: %s", e.getMessage());
        }

        if (HyperFactionsConfig.get().isBackupEnabled()) {
            startScheduledBackups();
        }
    }

    /**
     * Shuts down the backup manager.
     * Creates a shutdown backup if configured.
     */
    public void shutdown() {
        if (scheduledTaskId > 0) {
            hyperFactions.cancelTask(scheduledTaskId);
            scheduledTaskId = -1;
        }

        // Create shutdown backup if enabled
        if (HyperFactionsConfig.get().isBackupOnShutdown()) {
            Logger.info("[BackupManager] Creating shutdown backup...");
            createBackup(BackupType.MANUAL, "shutdown", null).join();
        }
    }

    /**
     * Starts the scheduled backup task.
     */
    private void startScheduledBackups() {
        // Run every hour (20 ticks/sec * 60 sec * 60 min = 72000 ticks)
        int periodTicks = 60 * 60 * 20;
        scheduledTaskId = hyperFactions.scheduleRepeatingTask(periodTicks, periodTicks, this::runScheduledBackup);
        if (scheduledTaskId > 0) {
            Logger.info("[BackupManager] Scheduled backups enabled (hourly)");
        }
    }

    /**
     * Runs a scheduled backup with automatic type determination.
     */
    private void runScheduledBackup() {
        BackupType type = determineBackupType();
        Logger.info("[BackupManager] Running scheduled %s backup", type.getDisplayName().toLowerCase());

        createBackup(type, null, null).thenAccept(result -> {
            if (result instanceof BackupResult.Success success) {
                Logger.info("[BackupManager] %s backup created: %s (%s)",
                    type.getDisplayName(), success.metadata().name(), success.metadata().getFormattedSize());
                performRotation();
            } else if (result instanceof BackupResult.Failure failure) {
                Logger.severe("[BackupManager] %s backup failed: %s", type.getDisplayName(), failure.error());
            }
        });
    }

    /**
     * Determines what type of backup to create based on current time.
     */
    @NotNull
    private BackupType determineBackupType() {
        LocalDateTime now = LocalDateTime.now();

        // Weekly: Sunday at midnight
        if (now.getDayOfWeek() == DayOfWeek.SUNDAY && now.getHour() == 0) {
            return BackupType.WEEKLY;
        }

        // Daily: Midnight (non-Sunday)
        if (now.getHour() == 0) {
            return BackupType.DAILY;
        }

        // All other times: Hourly
        return BackupType.HOURLY;
    }

    /**
     * Creates a backup asynchronously.
     *
     * @param type      the backup type
     * @param customName optional custom name for manual backups
     * @param createdBy  the UUID of the player creating the backup (null for auto)
     * @return a future containing the backup result
     */
    @NotNull
    public CompletableFuture<BackupResult> createBackup(
            @NotNull BackupType type,
            @Nullable String customName,
            @Nullable UUID createdBy) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Generate backup name
                Instant timestamp = Instant.now();
                String name;
                if (type == BackupType.MANUAL && customName != null && !customName.isEmpty()) {
                    name = BackupMetadata.generateManualName(customName);
                } else {
                    name = BackupMetadata.generateName(type, timestamp);
                }

                Path backupFile = backupsDir.resolve(name + ".zip");

                // Create ZIP file
                try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(backupFile.toFile()))) {
                    // Add data/factions/ directory
                    Path factionsDir = dataDir.resolve("factions");
                    if (Files.exists(factionsDir)) {
                        addDirectoryToZip(zos, factionsDir, "factions");
                    }

                    // Add data/players/ directory
                    Path playersDir = dataDir.resolve("players");
                    if (Files.exists(playersDir)) {
                        addDirectoryToZip(zos, playersDir, "players");
                    }

                    // Add zones.json
                    Path zonesFile = dataDir.resolve("zones.json");
                    if (Files.exists(zonesFile)) {
                        addFileToZip(zos, zonesFile, "zones.json");
                    }

                    // Add config.json
                    Path configFile = dataDir.resolve("config.json");
                    if (Files.exists(configFile)) {
                        addFileToZip(zos, configFile, "config.json");
                    }
                }

                // Create metadata
                long size = Files.size(backupFile);
                BackupMetadata metadata = new BackupMetadata(name, type, timestamp, size, createdBy);

                return new BackupResult.Success(metadata, backupFile);

            } catch (Exception e) {
                Logger.severe("[BackupManager] Failed to create backup: %s", e.getMessage());
                return new BackupResult.Failure("Failed to create backup: " + e.getMessage());
            }
        });
    }

    /**
     * Restores from a backup asynchronously.
     *
     * @param backupName the name of the backup to restore
     * @return a future containing the restore result
     */
    @NotNull
    public CompletableFuture<RestoreResult> restoreBackup(@NotNull String backupName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Find the backup file
                Path backupFile = findBackupFile(backupName);
                if (backupFile == null) {
                    return new RestoreResult.Failure("Backup not found: " + backupName);
                }

                int filesRestored = 0;

                // Extract ZIP contents
                try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(backupFile))) {
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        if (entry.isDirectory()) {
                            Files.createDirectories(dataDir.resolve(entry.getName()));
                        } else {
                            Path targetFile = dataDir.resolve(entry.getName());
                            Files.createDirectories(targetFile.getParent());
                            Files.copy(zis, targetFile, StandardCopyOption.REPLACE_EXISTING);
                            filesRestored++;
                        }
                        zis.closeEntry();
                    }
                }

                Logger.info("[BackupManager] Restored %d files from backup '%s'", filesRestored, backupName);
                return new RestoreResult.Success(backupName, filesRestored);

            } catch (Exception e) {
                Logger.severe("[BackupManager] Failed to restore backup: %s", e.getMessage());
                return new RestoreResult.Failure("Failed to restore backup: " + e.getMessage());
            }
        });
    }

    /**
     * Deletes a backup.
     *
     * @param backupName the name of the backup to delete
     * @return true if deleted successfully
     */
    @NotNull
    public CompletableFuture<Boolean> deleteBackup(@NotNull String backupName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path backupFile = findBackupFile(backupName);
                if (backupFile == null) {
                    return false;
                }
                Files.delete(backupFile);
                Logger.info("[BackupManager] Deleted backup: %s", backupName);
                return true;
            } catch (Exception e) {
                Logger.severe("[BackupManager] Failed to delete backup '%s': %s", backupName, e.getMessage());
                return false;
            }
        });
    }

    /**
     * Lists all available backups.
     *
     * @return list of backup metadata, sorted by timestamp (newest first)
     */
    @NotNull
    public List<BackupMetadata> listBackups() {
        List<BackupMetadata> backups = new ArrayList<>();

        try {
            if (!Files.exists(backupsDir)) {
                return backups;
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(backupsDir, "backup_*.zip")) {
                for (Path file : stream) {
                    try {
                        long size = Files.size(file);
                        BackupMetadata metadata = BackupMetadata.fromFilename(file.getFileName().toString(), size);
                        if (metadata != null) {
                            // Update timestamp from file's actual modification time
                            Instant fileTime = Files.getLastModifiedTime(file).toInstant();
                            backups.add(new BackupMetadata(
                                metadata.name(), metadata.type(), fileTime, size, metadata.createdBy()
                            ));
                        }
                    } catch (Exception e) {
                        Logger.warn("[BackupManager] Could not read backup metadata for %s: %s",
                            file.getFileName(), e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            Logger.severe("[BackupManager] Failed to list backups: %s", e.getMessage());
        }

        // Sort by timestamp, newest first
        backups.sort((a, b) -> b.timestamp().compareTo(a.timestamp()));
        return backups;
    }

    /**
     * Gets backups grouped by type.
     *
     * @return map of backup type to list of backups
     */
    @NotNull
    public Map<BackupType, List<BackupMetadata>> getBackupsGroupedByType() {
        return listBackups().stream()
            .collect(Collectors.groupingBy(BackupMetadata::type));
    }

    /**
     * Performs GFS rotation, pruning old backups according to retention settings.
     */
    public void performRotation() {
        HyperFactionsConfig config = HyperFactionsConfig.get();
        Map<BackupType, List<BackupMetadata>> grouped = getBackupsGroupedByType();

        // Rotate hourly backups
        rotateBackups(grouped.getOrDefault(BackupType.HOURLY, List.of()),
            config.getBackupHourlyRetention());

        // Rotate daily backups
        rotateBackups(grouped.getOrDefault(BackupType.DAILY, List.of()),
            config.getBackupDailyRetention());

        // Rotate weekly backups
        rotateBackups(grouped.getOrDefault(BackupType.WEEKLY, List.of()),
            config.getBackupWeeklyRetention());

        // Manual backups are never auto-deleted
    }

    /**
     * Rotates backups of a specific type, keeping only the most recent N.
     */
    private void rotateBackups(@NotNull List<BackupMetadata> backups, int retentionCount) {
        if (retentionCount <= 0 || backups.size() <= retentionCount) {
            return;
        }

        // Sort by timestamp, newest first
        List<BackupMetadata> sorted = new ArrayList<>(backups);
        sorted.sort((a, b) -> b.timestamp().compareTo(a.timestamp()));

        // Delete backups beyond the retention count
        for (int i = retentionCount; i < sorted.size(); i++) {
            BackupMetadata toDelete = sorted.get(i);
            deleteBackup(toDelete.name()).thenAccept(success -> {
                if (success) {
                    Logger.debug("[BackupManager] Rotated out old backup: %s", toDelete.name());
                }
            });
        }
    }

    /**
     * Finds a backup file by name.
     */
    @Nullable
    private Path findBackupFile(@NotNull String name) {
        // Try exact match first
        Path exact = backupsDir.resolve(name + ".zip");
        if (Files.exists(exact)) {
            return exact;
        }

        // Try with backup_ prefix
        Path withPrefix = backupsDir.resolve("backup_" + name + ".zip");
        if (Files.exists(withPrefix)) {
            return withPrefix;
        }

        // Search for partial match
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(backupsDir, "*" + name + "*.zip")) {
            for (Path file : stream) {
                return file; // Return first match
            }
        } catch (IOException ignored) {}

        return null;
    }

    /**
     * Adds a directory to a ZIP output stream.
     */
    private void addDirectoryToZip(@NotNull ZipOutputStream zos, @NotNull Path dir, @NotNull String zipPath)
            throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String entryName = zipPath + "/" + dir.relativize(file).toString().replace("\\", "/");
                addFileToZip(zos, file, entryName);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir2, BasicFileAttributes attrs) throws IOException {
                String entryName = zipPath + "/" + dir.relativize(dir2).toString().replace("\\", "/") + "/";
                if (!entryName.equals(zipPath + "//")) {
                    zos.putNextEntry(new ZipEntry(entryName));
                    zos.closeEntry();
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Adds a single file to a ZIP output stream.
     */
    private void addFileToZip(@NotNull ZipOutputStream zos, @NotNull Path file, @NotNull String entryName)
            throws IOException {
        zos.putNextEntry(new ZipEntry(entryName));
        Files.copy(file, zos);
        zos.closeEntry();
    }

    /**
     * Gets the backups directory path.
     *
     * @return the backups directory
     */
    @NotNull
    public Path getBackupsDir() {
        return backupsDir;
    }
}
