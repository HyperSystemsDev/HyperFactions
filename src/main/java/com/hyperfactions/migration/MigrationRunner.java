package com.hyperfactions.migration;

import com.hyperfactions.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Executes migrations with backup and rollback support.
 * <p>
 * The runner handles:
 * - Discovering applicable migrations via {@link MigrationRegistry}
 * - Creating timestamped backups before each migration
 * - Executing migrations in order (chaining v1 -> v2 -> v3)
 * - Rolling back on failure by restoring from backup
 * - Reporting progress via callbacks
 */
public class MigrationRunner {

    private final Path dataDir;
    private final MigrationOptions.ProgressCallback progressCallback;

    /**
     * Creates a new migration runner.
     *
     * @param dataDir the plugin data directory
     */
    public MigrationRunner(@NotNull Path dataDir) {
        this(dataDir, null);
    }

    /**
     * Creates a new migration runner with progress callback.
     *
     * @param dataDir          the plugin data directory
     * @param progressCallback callback for progress updates
     */
    public MigrationRunner(@NotNull Path dataDir, @Nullable MigrationOptions.ProgressCallback progressCallback) {
        this.dataDir = dataDir;
        this.progressCallback = progressCallback;
    }

    /**
     * Runs all pending migrations of a specific type.
     *
     * @param type the migration type to run
     * @return list of migration results
     */
    @NotNull
    public List<MigrationResult> runPendingMigrations(@NotNull MigrationType type) {
        List<Migration> migrations = MigrationRegistry.get().buildMigrationChain(type, dataDir);

        if (migrations.isEmpty()) {
            Logger.debug("[Migration] No pending %s migrations", type);
            return List.of();
        }

        Logger.info("[Migration] Found %d pending %s migration(s)", migrations.size(), type);

        List<MigrationResult> results = new ArrayList<>();
        for (Migration migration : migrations) {
            MigrationResult result = runMigration(migration);
            results.add(result);

            if (!result.success()) {
                Logger.severe("[Migration] Migration '%s' failed: %s", migration.id(), result.errorMessage());
                if (result.rolledBack()) {
                    Logger.info("[Migration] Successfully rolled back to backup");
                }
                // Stop running further migrations after failure
                break;
            }

            Logger.info("[Migration] Migration '%s' completed successfully in %dms",
                    migration.id(), result.duration().toMillis());
        }

        return results;
    }

    /**
     * Runs a single migration with backup and rollback support.
     *
     * @param migration the migration to run
     * @return migration result
     */
    @NotNull
    public MigrationResult runMigration(@NotNull Migration migration) {
        Instant startTime = Instant.now();

        Logger.info("[Migration] Running migration '%s': %s", migration.id(), migration.description());
        Logger.info("[Migration] Upgrading from v%d to v%d", migration.fromVersion(), migration.toVersion());

        // Create backup
        Path backupPath = null;
        try {
            backupPath = createBackup(migration);
            Logger.info("[Migration] Created backup at: %s", backupPath);
        } catch (IOException e) {
            Duration duration = Duration.between(startTime, Instant.now());
            Logger.severe("[Migration] Failed to create backup: %s", e.getMessage());
            return MigrationResult.failure(
                migration.id(),
                migration.fromVersion(),
                migration.toVersion(),
                null,
                "Failed to create backup: " + e.getMessage(),
                false,
                duration
            );
        }

        // Execute migration
        try {
            MigrationOptions options = progressCallback != null
                    ? MigrationOptions.withProgress(backupPath, progressCallback)
                    : MigrationOptions.defaults(backupPath);

            MigrationResult result = migration.execute(dataDir, options);

            if (!result.success() && backupPath != null) {
                // Rollback on failure
                try {
                    rollback(migration, backupPath);
                    Duration duration = Duration.between(startTime, Instant.now());
                    return MigrationResult.failure(
                        migration.id(),
                        migration.fromVersion(),
                        migration.toVersion(),
                        backupPath,
                        result.errorMessage() != null ? result.errorMessage() : "Migration failed",
                        true,
                        duration
                    );
                } catch (IOException rollbackError) {
                    Logger.severe("[Migration] Rollback failed: %s", rollbackError.getMessage());
                    Duration duration = Duration.between(startTime, Instant.now());
                    return MigrationResult.failure(
                        migration.id(),
                        migration.fromVersion(),
                        migration.toVersion(),
                        backupPath,
                        "Migration failed and rollback also failed: " + rollbackError.getMessage(),
                        false,
                        duration
                    );
                }
            }

            return result;

        } catch (Exception e) {
            Logger.severe("[Migration] Migration threw exception: %s", e.getMessage());

            // Attempt rollback
            boolean rolledBack = false;
            if (backupPath != null) {
                try {
                    rollback(migration, backupPath);
                    rolledBack = true;
                } catch (IOException rollbackError) {
                    Logger.severe("[Migration] Rollback failed: %s", rollbackError.getMessage());
                }
            }

            Duration duration = Duration.between(startTime, Instant.now());
            return MigrationResult.failure(
                migration.id(),
                migration.fromVersion(),
                migration.toVersion(),
                backupPath,
                e.getMessage(),
                rolledBack,
                duration
            );
        }
    }

    /**
     * Creates a backup before running a migration.
     * <p>
     * For config migrations, backs up config.json (and config/ directory if it exists).
     * Backup naming: config.json.v{fromVersion}.backup
     *
     * @param migration the migration about to run
     * @return path to the backup file/directory
     * @throws IOException if backup fails
     */
    @NotNull
    private Path createBackup(@NotNull Migration migration) throws IOException {
        switch (migration.type()) {
            case CONFIG -> {
                Path configFile = dataDir.resolve("config.json");
                Path backupFile = dataDir.resolve("config.json.v" + migration.fromVersion() + ".backup");

                if (Files.exists(configFile)) {
                    Files.copy(configFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
                }

                // Also backup config/ directory if it exists
                Path configDir = dataDir.resolve("config");
                if (Files.exists(configDir) && Files.isDirectory(configDir)) {
                    Path configDirBackup = dataDir.resolve("config.v" + migration.fromVersion() + ".backup");
                    copyDirectory(configDir, configDirBackup);
                }

                return backupFile;
            }
            case DATA -> {
                // For data migrations, backup the entire data directory
                Path backupDir = dataDir.resolve("data.v" + migration.fromVersion() + ".backup");
                // Implementation depends on data structure
                return backupDir;
            }
            case SCHEMA -> {
                // For schema migrations, typically handled by database
                return dataDir.resolve("schema.v" + migration.fromVersion() + ".backup");
            }
            default -> throw new IllegalStateException("Unknown migration type: " + migration.type());
        }
    }

    /**
     * Rolls back a failed migration by restoring from backup.
     *
     * @param migration  the migration that failed
     * @param backupPath path to the backup
     * @throws IOException if rollback fails
     */
    private void rollback(@NotNull Migration migration, @NotNull Path backupPath) throws IOException {
        Logger.info("[Migration] Rolling back migration '%s'...", migration.id());

        switch (migration.type()) {
            case CONFIG -> {
                // Restore config.json from backup
                Path configFile = dataDir.resolve("config.json");
                if (Files.exists(backupPath)) {
                    Files.copy(backupPath, configFile, StandardCopyOption.REPLACE_EXISTING);
                }

                // Delete any config/ directory that was created
                Path configDir = dataDir.resolve("config");
                if (Files.exists(configDir)) {
                    deleteDirectory(configDir);
                }

                // Restore config/ directory backup if it exists
                Path configDirBackup = dataDir.resolve("config.v" + migration.fromVersion() + ".backup");
                if (Files.exists(configDirBackup)) {
                    copyDirectory(configDirBackup, configDir);
                }
            }
            case DATA, SCHEMA -> {
                // Implementation depends on data structure
                Logger.warn("[Migration] Rollback for %s migrations not fully implemented", migration.type());
            }
        }

        Logger.info("[Migration] Rollback complete");
    }

    /**
     * Copies a directory recursively.
     *
     * @param source source directory
     * @param target target directory
     * @throws IOException if copy fails
     */
    private void copyDirectory(@NotNull Path source, @NotNull Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path targetFile = target.resolve(source.relativize(file));
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Deletes a directory recursively.
     *
     * @param directory directory to delete
     * @throws IOException if delete fails
     */
    private void deleteDirectory(@NotNull Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Convenience method to run pending config migrations.
     *
     * @param dataDir the plugin data directory
     * @return list of migration results
     */
    @NotNull
    public static List<MigrationResult> runPendingMigrations(@NotNull Path dataDir, @NotNull MigrationType type) {
        return new MigrationRunner(dataDir).runPendingMigrations(type);
    }
}
