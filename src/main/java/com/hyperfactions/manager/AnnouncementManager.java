package com.hyperfactions.manager;

import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.config.modules.AnnouncementConfig;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * Broadcasts server-wide announcements for significant faction events.
 * Checks {@link AnnouncementConfig} to determine which events are enabled.
 */
public class AnnouncementManager {

    private final Supplier<Collection<PlayerRef>> onlinePlayersSupplier;

    /**
     * Creates a new announcement manager.
     *
     * @param onlinePlayersSupplier supplies the collection of currently online players
     */
    public AnnouncementManager(@NotNull Supplier<Collection<PlayerRef>> onlinePlayersSupplier) {
        this.onlinePlayersSupplier = onlinePlayersSupplier;
    }

    /**
     * Announces that a new faction has been created.
     *
     * @param factionName the faction name
     * @param leaderName  the leader's username
     */
    public void announceFactionCreated(@NotNull String factionName, @NotNull String leaderName) {
        AnnouncementConfig config = ConfigManager.get().announcements();
        if (!config.isEnabled() || !config.isFactionCreated()) return;

        broadcast(buildMessage(leaderName + " has founded the faction " + factionName + "!", "#55FF55"));
    }

    /**
     * Announces that a faction has been disbanded.
     *
     * @param factionName the faction name
     */
    public void announceFactionDisbanded(@NotNull String factionName) {
        AnnouncementConfig config = ConfigManager.get().announcements();
        if (!config.isEnabled() || !config.isFactionDisbanded()) return;

        broadcast(buildMessage("The faction " + factionName + " has been disbanded!", "#FF5555"));
    }

    /**
     * Announces a leadership transfer.
     *
     * @param factionName the faction name
     * @param oldLeader   the old leader's username
     * @param newLeader   the new leader's username
     */
    public void announceLeadershipTransfer(@NotNull String factionName,
                                           @NotNull String oldLeader, @NotNull String newLeader) {
        AnnouncementConfig config = ConfigManager.get().announcements();
        if (!config.isEnabled() || !config.isLeadershipTransfer()) return;

        broadcast(buildMessage(newLeader + " is now the leader of " + factionName + "!", "#FFAA00"));
    }

    /**
     * Announces an overclaim.
     *
     * @param attackerFaction the attacking faction name
     * @param defenderFaction the defending faction name
     */
    public void announceOverclaim(@NotNull String attackerFaction, @NotNull String defenderFaction) {
        AnnouncementConfig config = ConfigManager.get().announcements();
        if (!config.isEnabled() || !config.isOverclaim()) return;

        broadcast(buildMessage(attackerFaction + " has overclaimed territory from " + defenderFaction + "!", "#FF5555"));
    }

    /**
     * Announces a war declaration.
     *
     * @param declaringFaction the declaring faction name
     * @param targetFaction    the target faction name
     */
    public void announceWarDeclared(@NotNull String declaringFaction, @NotNull String targetFaction) {
        AnnouncementConfig config = ConfigManager.get().announcements();
        if (!config.isEnabled() || !config.isWarDeclared()) return;

        broadcast(buildMessage(declaringFaction + " has declared war on " + targetFaction + "!", "#FF5555"));
    }

    /**
     * Announces an alliance formation.
     *
     * @param faction1 the first faction name
     * @param faction2 the second faction name
     */
    public void announceAllianceFormed(@NotNull String faction1, @NotNull String faction2) {
        AnnouncementConfig config = ConfigManager.get().announcements();
        if (!config.isEnabled() || !config.isAllianceFormed()) return;

        broadcast(buildMessage(faction1 + " and " + faction2 + " are now allies!", "#55FF55"));
    }

    /**
     * Announces an alliance being broken.
     *
     * @param faction1 the faction breaking the alliance
     * @param faction2 the other faction
     */
    public void announceAllianceBroken(@NotNull String faction1, @NotNull String faction2) {
        AnnouncementConfig config = ConfigManager.get().announcements();
        if (!config.isEnabled() || !config.isAllianceBroken()) return;

        broadcast(buildMessage(faction1 + " and " + faction2 + " are no longer allies!", "#FFAA00"));
    }

    /**
     * Builds a formatted announcement message using the configured prefix from config.json.
     */
    private Message buildMessage(@NotNull String text, @NotNull String color) {
        ConfigManager config = ConfigManager.get();
        String prefixText = config.getPrefixText();
        String prefixColor = config.getPrefixColor();
        String bracketColor = config.getPrefixBracketColor();

        return Message.raw("[").color(bracketColor)
            .insert(Message.raw(prefixText).color(prefixColor))
            .insert(Message.raw("] ").color(bracketColor))
            .insert(Message.raw(text).color(color));
    }

    /**
     * Broadcasts a message to all online players.
     */
    private void broadcast(@NotNull Message message) {
        try {
            Collection<PlayerRef> players = onlinePlayersSupplier.get();
            if (players == null) return;

            for (PlayerRef player : players) {
                player.sendMessage(message);
            }
        } catch (Exception e) {
            Logger.warn("Failed to broadcast announcement: %s", e.getMessage());
        }
    }
}
