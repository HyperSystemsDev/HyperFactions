package com.hyperfactions.manager;

import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionRelation;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Manages chat channels for faction and ally chat.
 * Players can toggle between normal, faction, and ally chat modes.
 */
public class ChatManager {

    /**
     * Chat channel types.
     */
    public enum ChatChannel {
        NORMAL,
        FACTION,
        ALLY
    }

    // Player UUID -> current chat channel
    private final Map<UUID, ChatChannel> playerChannels = new ConcurrentHashMap<>();

    private final FactionManager factionManager;
    private final RelationManager relationManager;
    private final Function<UUID, PlayerRef> playerLookup;

    /**
     * Creates a new ChatManager.
     *
     * @param factionManager  the faction manager
     * @param relationManager the relation manager
     * @param playerLookup    function to look up online PlayerRef by UUID
     */
    public ChatManager(@NotNull FactionManager factionManager,
                       @NotNull RelationManager relationManager,
                       @NotNull Function<UUID, PlayerRef> playerLookup) {
        this.factionManager = factionManager;
        this.relationManager = relationManager;
        this.playerLookup = playerLookup;
    }

    /**
     * Gets a player's current chat channel.
     *
     * @param playerUuid the player's UUID
     * @return the current channel (defaults to NORMAL)
     */
    @NotNull
    public ChatChannel getChannel(@NotNull UUID playerUuid) {
        return playerChannels.getOrDefault(playerUuid, ChatChannel.NORMAL);
    }

    /**
     * Sets a player's chat channel.
     *
     * @param playerUuid the player's UUID
     * @param channel    the channel to set
     */
    public void setChannel(@NotNull UUID playerUuid, @NotNull ChatChannel channel) {
        if (channel == ChatChannel.NORMAL) {
            playerChannels.remove(playerUuid);
        } else {
            playerChannels.put(playerUuid, channel);
        }
    }

    /**
     * Toggles faction chat for a player.
     *
     * @param playerUuid the player's UUID
     * @return the new channel state
     */
    @NotNull
    public ChatChannel toggleFactionChat(@NotNull UUID playerUuid) {
        ChatChannel current = getChannel(playerUuid);
        if (current == ChatChannel.FACTION) {
            setChannel(playerUuid, ChatChannel.NORMAL);
            return ChatChannel.NORMAL;
        } else {
            setChannel(playerUuid, ChatChannel.FACTION);
            return ChatChannel.FACTION;
        }
    }

    /**
     * Toggles ally chat for a player.
     *
     * @param playerUuid the player's UUID
     * @return the new channel state
     */
    @NotNull
    public ChatChannel toggleAllyChat(@NotNull UUID playerUuid) {
        ChatChannel current = getChannel(playerUuid);
        if (current == ChatChannel.ALLY) {
            setChannel(playerUuid, ChatChannel.NORMAL);
            return ChatChannel.NORMAL;
        } else {
            setChannel(playerUuid, ChatChannel.ALLY);
            return ChatChannel.ALLY;
        }
    }

    /**
     * Resets a player's chat channel to normal.
     * Call when player leaves faction or disconnects.
     *
     * @param playerUuid the player's UUID
     */
    public void resetChannel(@NotNull UUID playerUuid) {
        playerChannels.remove(playerUuid);
    }

    /**
     * Processes a chat message based on the player's current channel.
     * Returns true if the message was handled (faction/ally chat), false if normal chat.
     *
     * @param sender  the sender's PlayerRef
     * @param message the message content
     * @return true if message was handled as faction/ally chat
     */
    public boolean processChatMessage(@NotNull PlayerRef sender, @NotNull String message) {
        UUID senderUuid = sender.getUuid();
        ChatChannel channel = getChannel(senderUuid);

        if (channel == ChatChannel.NORMAL) {
            return false;
        }

        Faction senderFaction = factionManager.getPlayerFaction(senderUuid);
        if (senderFaction == null) {
            // Player is not in a faction, reset to normal
            resetChannel(senderUuid);
            return false;
        }

        if (channel == ChatChannel.FACTION) {
            sendFactionMessage(sender, senderFaction, message);
            return true;
        } else if (channel == ChatChannel.ALLY) {
            sendAllyMessage(sender, senderFaction, message);
            return true;
        }

        return false;
    }

    /**
     * Sends a message to all faction members.
     */
    private void sendFactionMessage(@NotNull PlayerRef sender, @NotNull Faction faction, @NotNull String message) {
        Message formatted = Message.raw("[Faction] ").color("#00FFFF")
                .insert(Message.raw(sender.getUsername()).color("#FFFF55"))
                .insert(Message.raw(": ").color("#AAAAAA"))
                .insert(Message.raw(message).color("#FFFFFF"));

        // Send to all online faction members
        for (UUID memberUuid : faction.members().keySet()) {
            PlayerRef member = playerLookup.apply(memberUuid);
            if (member != null) {
                member.sendMessage(formatted);
            }
        }

        Logger.debug("[FChat] %s: %s", sender.getUsername(), message);
    }

    /**
     * Sends a message to all faction and ally faction members.
     */
    private void sendAllyMessage(@NotNull PlayerRef sender, @NotNull Faction faction, @NotNull String message) {
        Message formatted = Message.raw("[Ally] ").color("#AA00AA")
                .insert(Message.raw("[" + faction.tag() + "] ").color("#AAAAAA"))
                .insert(Message.raw(sender.getUsername()).color("#FFFF55"))
                .insert(Message.raw(": ").color("#AAAAAA"))
                .insert(Message.raw(message).color("#FFFFFF"));

        // Send to sender's faction members
        for (UUID memberUuid : faction.members().keySet()) {
            PlayerRef member = playerLookup.apply(memberUuid);
            if (member != null) {
                member.sendMessage(formatted);
            }
        }

        // Send to all ally faction members
        for (UUID allyFactionId : faction.relations().keySet()) {
            FactionRelation relation = faction.relations().get(allyFactionId);
            if (relation != null && relation.isAlly()) {
                Faction allyFaction = factionManager.getFaction(allyFactionId);
                if (allyFaction != null) {
                    for (UUID allyMemberUuid : allyFaction.members().keySet()) {
                        PlayerRef allyMember = playerLookup.apply(allyMemberUuid);
                        if (allyMember != null) {
                            allyMember.sendMessage(formatted);
                        }
                    }
                }
            }
        }

        Logger.debug("[AChat] %s: %s", sender.getUsername(), message);
    }

    /**
     * Gets a display string for a chat channel.
     *
     * @param channel the channel
     * @return display string
     */
    @NotNull
    public static String getChannelDisplay(@NotNull ChatChannel channel) {
        return switch (channel) {
            case NORMAL -> "Public";
            case FACTION -> "Faction";
            case ALLY -> "Ally";
        };
    }

    /**
     * Gets the color for a chat channel.
     *
     * @param channel the channel
     * @return hex color string
     */
    @NotNull
    public static String getChannelColor(@NotNull ChatChannel channel) {
        return switch (channel) {
            case NORMAL -> "#FFFFFF";
            case FACTION -> "#00FFFF";
            case ALLY -> "#AA00AA";
        };
    }
}
