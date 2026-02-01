package com.hyperfactions.storage;

import com.hyperfactions.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility class for bulletproof file storage operations.
 * Provides atomic writes with checksums, verification, and backup recovery.
 */
public final class StorageUtils {

    private StorageUtils() {}

    /** Counter for unique temp file names to prevent race conditions */
    private static final AtomicLong TEMP_COUNTER = new AtomicLong(System.currentTimeMillis());

    /** File extension for temporary files during writes */
    private static final String TMP_SUFFIX = ".tmp";

    /** File extension for backup files */
    private static final String BAK_SUFFIX = ".bak";

    /**
     * Sealed interface representing the result of a write operation.
     */
    public sealed interface WriteResult permits WriteResult.Success, WriteResult.Failure {

        /**
         * Successful write result.
         *
         * @param file     the target file that was written
         * @param checksum the SHA-256 checksum of the written content
         */
        record Success(@NotNull Path file, @NotNull String checksum) implements WriteResult {}

        /**
         * Failed write result.
         *
         * @param file  the target file that failed to write
         * @param error human-readable error message
         * @param cause the underlying exception (may be null)
         */
        record Failure(@NotNull Path file, @NotNull String error, @Nullable Exception cause) implements WriteResult {}
    }

    /**
     * Atomically writes content to a file using a temp file and rename pattern.
     * This ensures the file is never in a corrupted state, even if the process crashes.
     *
     * <p>Steps:
     * <ol>
     *   <li>Write content to file.tmp</li>
     *   <li>Calculate SHA-256 checksum</li>
     *   <li>Verify by reading back and comparing checksum</li>
     *   <li>Backup existing file to file.bak</li>
     *   <li>Atomic rename: tmp → target</li>
     * </ol>
     *
     * @param targetFile the final destination file
     * @param content    the content to write
     * @return WriteResult indicating success or failure
     */
    @NotNull
    public static WriteResult writeAtomic(@NotNull Path targetFile, @NotNull String content) {
        // Use unique temp file name to prevent race conditions when multiple writes happen concurrently
        long uniqueId = TEMP_COUNTER.incrementAndGet();
        Path tempFile = targetFile.resolveSibling(targetFile.getFileName() + "." + uniqueId + TMP_SUFFIX);
        Path backupFile = targetFile.resolveSibling(targetFile.getFileName() + BAK_SUFFIX);

        try {
            // Step 1: Ensure parent directory exists
            Path parent = targetFile.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            // Step 2: Write to temp file
            Files.writeString(tempFile, content);

            // Step 3: Calculate checksum of original content
            String expectedChecksum = computeChecksum(content);

            // Step 4: Verify by reading back the temp file
            String writtenContent = Files.readString(tempFile);
            String actualChecksum = computeChecksum(writtenContent);

            if (!expectedChecksum.equals(actualChecksum)) {
                // Verification failed - temp file is corrupt
                Files.deleteIfExists(tempFile);
                String error = "Checksum verification failed: expected " + expectedChecksum + ", got " + actualChecksum;
                Logger.severe("[StorageUtils] %s for %s", error, targetFile);
                return new WriteResult.Failure(targetFile, error, null);
            }

            // Step 5: Backup existing file (if it exists)
            if (Files.exists(targetFile)) {
                try {
                    Files.copy(targetFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    Logger.warn("[StorageUtils] Could not create backup for %s: %s", targetFile, e.getMessage());
                    // Continue anyway - backup is best-effort
                }
            }

            // Step 6: Atomic rename temp → target
            Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            Logger.debug("[StorageUtils] Atomic write successful: %s (checksum: %s)", targetFile.getFileName(), expectedChecksum.substring(0, 8));
            return new WriteResult.Success(targetFile, expectedChecksum);

        } catch (IOException e) {
            // Clean up temp file if it exists
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {}

            String error = "I/O error during atomic write: " + e.getMessage();
            Logger.severe("[StorageUtils] %s for %s", error, targetFile);
            return new WriteResult.Failure(targetFile, error, e);

        } catch (Exception e) {
            // Clean up temp file if it exists
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {}

            String error = "Unexpected error during atomic write: " + e.getMessage();
            Logger.severe("[StorageUtils] %s for %s", error, targetFile);
            return new WriteResult.Failure(targetFile, error, e);
        }
    }

    /**
     * Computes the SHA-256 checksum of the given content.
     *
     * @param content the content to checksum
     * @return the hex-encoded SHA-256 checksum
     */
    @NotNull
    public static String computeChecksum(@NotNull String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Attempts to recover a file from its backup.
     * Use this when the main file is missing or corrupt.
     *
     * @param targetFile the file to recover
     * @return true if recovery was successful, false otherwise
     */
    public static boolean recoverFromBackup(@NotNull Path targetFile) {
        Path backupFile = targetFile.resolveSibling(targetFile.getFileName() + BAK_SUFFIX);

        if (!Files.exists(backupFile)) {
            Logger.warn("[StorageUtils] No backup file found for %s", targetFile);
            return false;
        }

        try {
            // Verify backup is readable and valid
            String backupContent = Files.readString(backupFile);
            if (backupContent.isEmpty()) {
                Logger.warn("[StorageUtils] Backup file is empty for %s", targetFile);
                return false;
            }

            // Copy backup to main file
            Files.copy(backupFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
            Logger.info("[StorageUtils] Recovered %s from backup (size: %d bytes)", targetFile.getFileName(), backupContent.length());
            return true;

        } catch (IOException e) {
            Logger.severe("[StorageUtils] Failed to recover %s from backup: %s", targetFile, e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a backup file exists for the given target file.
     *
     * @param targetFile the file to check backup for
     * @return true if a backup exists
     */
    public static boolean hasBackup(@NotNull Path targetFile) {
        Path backupFile = targetFile.resolveSibling(targetFile.getFileName() + BAK_SUFFIX);
        return Files.exists(backupFile);
    }

    /**
     * Gets the backup file path for a given target file.
     *
     * @param targetFile the target file
     * @return the backup file path
     */
    @NotNull
    public static Path getBackupPath(@NotNull Path targetFile) {
        return targetFile.resolveSibling(targetFile.getFileName() + BAK_SUFFIX);
    }

    /**
     * Converts a byte array to a hexadecimal string.
     */
    @NotNull
    private static String bytesToHex(@NotNull byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
