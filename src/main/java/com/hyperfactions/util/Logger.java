package com.hyperfactions.util;

import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * Wrapped logger with HyperFactions prefix and formatting.
 */
public final class Logger {

    private static final String PREFIX = "[HyperFactions] ";
    private static java.util.logging.Logger logger;

    private Logger() {}

    /**
     * Initializes the logger.
     *
     * @param parentLogger the parent logger from the plugin
     */
    public static void init(@NotNull java.util.logging.Logger parentLogger) {
        logger = parentLogger;
    }

    /**
     * Logs an info message.
     *
     * @param message the message
     */
    public static void info(@NotNull String message) {
        if (logger != null) {
            logger.info(PREFIX + message);
        } else {
            System.out.println(PREFIX + "[INFO] " + message);
        }
    }

    /**
     * Logs an info message with formatting.
     *
     * @param message the message format
     * @param args    the format arguments
     */
    public static void info(@NotNull String message, Object... args) {
        info(String.format(message, args));
    }

    /**
     * Logs a warning message.
     *
     * @param message the message
     */
    public static void warn(@NotNull String message) {
        if (logger != null) {
            logger.warning(PREFIX + message);
        } else {
            System.out.println(PREFIX + "[WARN] " + message);
        }
    }

    /**
     * Logs a warning message with formatting.
     *
     * @param message the message format
     * @param args    the format arguments
     */
    public static void warn(@NotNull String message, Object... args) {
        warn(String.format(message, args));
    }

    /**
     * Logs a severe error message.
     *
     * @param message the message
     */
    public static void severe(@NotNull String message) {
        if (logger != null) {
            logger.severe(PREFIX + message);
        } else {
            System.err.println(PREFIX + "[SEVERE] " + message);
        }
    }

    /**
     * Logs a severe error message with formatting.
     *
     * @param message the message format
     * @param args    the format arguments
     */
    public static void severe(@NotNull String message, Object... args) {
        severe(String.format(message, args));
    }

    /**
     * Logs a severe error with exception.
     *
     * @param message   the message format
     * @param throwable the exception
     * @param args      format arguments
     */
    public static void severe(@NotNull String message, @NotNull Throwable throwable, Object... args) {
        String formatted = String.format(message, args);
        if (logger != null) {
            logger.log(Level.SEVERE, PREFIX + formatted, throwable);
        } else {
            System.err.println(PREFIX + "[SEVERE] " + formatted);
            throwable.printStackTrace();
        }
    }

    /**
     * Logs a debug message (only if debug is enabled).
     *
     * @param message the message
     */
    public static void debug(@NotNull String message) {
        if (logger != null) {
            logger.fine(PREFIX + "[DEBUG] " + message);
        }
    }

    /**
     * Logs a debug message with formatting.
     *
     * @param message the message format
     * @param args    the format arguments
     */
    public static void debug(@NotNull String message, Object... args) {
        debug(String.format(message, args));
    }
}
