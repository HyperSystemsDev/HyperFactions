package com.hyperfactions.command.util;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.data.Faction;
import com.hyperfactions.integration.PermissionManager;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Shared utility methods for faction commands.
 */
public final class CommandUtil {

    // Color constants used across commands
    public static final String COLOR_CYAN = "#55FFFF";
    public static final String COLOR_GREEN = "#55FF55";
    public static final String COLOR_RED = "#FF5555";
    public static final String COLOR_YELLOW = "#FFFF55";
    public static final String COLOR_GRAY = "#AAAAAA";
    public static final String COLOR_WHITE = "#FFFFFF";

    private CommandUtil() {}

    /**
     * Creates the standard HyperFactions message prefix.
     *
     * @return the prefix message
     */
    @NotNull
    public static Message prefix() {
        return Message.raw("[").color(COLOR_GRAY)
            .insert(Message.raw("HyperFactions").color(COLOR_CYAN))
            .insert(Message.raw("] ").color(COLOR_GRAY));
    }

    /**
     * Creates a colored message.
     *
     * @param text the text content
     * @param color the hex color code
     * @return the colored message
     */
    @NotNull
    public static Message msg(@NotNull String text, @NotNull String color) {
        return Message.raw(text).color(color);
    }

    /**
     * Checks if a player has a specific permission.
     *
     * @param player the player to check
     * @param permission the permission node
     * @return true if the player has the permission
     */
    public static boolean hasPermission(@NotNull PlayerRef player, @NotNull String permission) {
        return PermissionManager.get().hasPermission(player.getUuid(), permission);
    }

    /**
     * Finds an online player by name (case-insensitive).
     *
     * @param plugin the plugin instance
     * @param name the player name to search for
     * @return the PlayerRef if found online, null otherwise
     */
    @Nullable
    public static PlayerRef findOnlinePlayer(@NotNull HyperFactionsPlugin plugin, @NotNull String name) {
        for (PlayerRef player : plugin.getTrackedPlayers().values()) {
            if (player.getUsername().equalsIgnoreCase(name)) {
                return player;
            }
        }
        return null;
    }

    /**
     * Broadcasts a message to all online members of a faction.
     *
     * @param hyperFactions the HyperFactions instance
     * @param plugin the plugin instance
     * @param factionId the faction UUID
     * @param message the message to broadcast
     */
    public static void broadcastToFaction(@NotNull HyperFactions hyperFactions,
                                          @NotNull HyperFactionsPlugin plugin,
                                          @NotNull UUID factionId,
                                          @NotNull Message message) {
        Faction faction = hyperFactions.getFactionManager().getFaction(factionId);
        if (faction == null) return;

        for (UUID memberUuid : faction.members().keySet()) {
            PlayerRef member = plugin.getTrackedPlayer(memberUuid);
            if (member != null) {
                member.sendMessage(message);
            }
        }
    }
}
