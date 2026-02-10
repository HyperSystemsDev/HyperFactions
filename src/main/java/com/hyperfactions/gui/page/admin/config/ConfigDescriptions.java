package com.hyperfactions.gui.page.admin.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Human-readable descriptions for all config settings, shown in the callout box.
 */
public final class ConfigDescriptions {

    private ConfigDescriptions() {}

    private static final Map<String, String> DESCRIPTIONS = Map.ofEntries(
        // === General: Faction ===
        Map.entry("faction.maxMembers", "Maximum players allowed in a single faction. Existing factions above this limit keep their members but cannot invite more."),
        Map.entry("faction.maxNameLength", "Maximum character length for faction names (1-64)."),
        Map.entry("faction.minNameLength", "Minimum character length for faction names."),
        Map.entry("faction.allowColors", "Allow factions to use color codes in their name and description."),

        // === General: Relations ===
        Map.entry("relations.maxAllies", "Maximum number of ally relations per faction. Set to -1 for unlimited."),
        Map.entry("relations.maxEnemies", "Maximum number of enemy relations per faction. Set to -1 for unlimited."),

        // === General: Invites ===
        Map.entry("invites.inviteExpirationMinutes", "How long a faction invite remains valid before expiring (minutes)."),
        Map.entry("invites.joinRequestExpirationHours", "How long a join request remains pending before expiring (hours)."),

        // === General: Teleport ===
        Map.entry("teleport.warmupSeconds", "Seconds a player must wait before teleporting to faction home."),
        Map.entry("teleport.cooldownSeconds", "Cooldown between teleport uses (seconds)."),
        Map.entry("teleport.cancelOnMove", "Cancel teleport warmup if the player moves."),
        Map.entry("teleport.cancelOnDamage", "Cancel teleport warmup if the player takes damage."),

        // === General: Stuck ===
        Map.entry("stuck.warmupSeconds", "Seconds a player must wait before the /f stuck teleport activates."),
        Map.entry("stuck.cooldownSeconds", "Cooldown between /f stuck uses (seconds)."),

        // === General: Permissions ===
        Map.entry("permissions.adminRequiresOp", "Require server OP status for /f admin commands when no permission plugin is present."),
        Map.entry("permissions.allowWithoutPermissionMod", "Allow players to use commands without a permission plugin installed. If false, only OP players can use commands."),

        // === General: Auto-save ===
        Map.entry("autoSave.enabled", "Automatically save faction data at regular intervals."),
        Map.entry("autoSave.intervalMinutes", "Minutes between automatic saves."),

        // === General: Updates ===
        Map.entry("updates.enabled", "Check for plugin updates on startup and notify admins."),
        Map.entry("updates.releaseChannel", "Which release channel to check: 'stable' for releases only, 'prerelease' to include betas."),

        // === General: Messages ===
        Map.entry("messages.prefixText", "Text shown in the chat prefix brackets, e.g. 'HyperFactions'."),
        Map.entry("messages.prefixColor", "Hex color for the prefix text."),
        Map.entry("messages.prefixBracketColor", "Hex color for the brackets around the prefix."),
        Map.entry("messages.primaryColor", "Primary accent color used throughout messages."),

        // === General: GUI ===
        Map.entry("gui.title", "Title text shown at the top of faction GUI pages."),

        // === General: Territory ===
        Map.entry("territoryNotifications.enabled", "Show territory entry/exit notifications when players cross faction borders."),

        // === Power ===
        Map.entry("power.maxPlayerPower", "Maximum power a single player can accumulate."),
        Map.entry("power.startingPower", "Power given to new players when they first join."),
        Map.entry("power.powerPerClaim", "Power cost per claimed chunk. Determines how many chunks a faction can claim."),
        Map.entry("power.deathPenalty", "Power lost when a player dies."),
        Map.entry("power.killReward", "Power gained when killing another player."),
        Map.entry("power.regenPerMinute", "Power regenerated per minute while online."),
        Map.entry("power.regenWhenOffline", "Allow power regeneration while players are offline."),

        // === Claims ===
        Map.entry("claims.maxClaims", "Hard cap on claims per faction, regardless of power."),
        Map.entry("claims.onlyAdjacent", "Require new claims to be adjacent to existing faction territory."),
        Map.entry("claims.preventDisconnect", "Prevent unclaiming if it would split territory into disconnected pieces."),
        Map.entry("claims.decayEnabled", "Automatically unclaim territory from inactive factions."),
        Map.entry("claims.decayDaysInactive", "Days of inactivity before claim decay begins."),
        Map.entry("claims.worldWhitelist", "Only allow claiming in these worlds. If empty, all worlds are allowed (unless blacklisted)."),
        Map.entry("claims.worldBlacklist", "Prevent claiming in these worlds. Ignored if whitelist is set."),

        // === Claims: Economy ===
        Map.entry("economy.enabled", "Enable the faction economy system."),
        Map.entry("economy.currencyName", "Singular currency name (e.g. 'dollar')."),
        Map.entry("economy.currencyNamePlural", "Plural currency name (e.g. 'dollars')."),
        Map.entry("economy.currencySymbol", "Currency symbol for display (e.g. '$')."),
        Map.entry("economy.startingBalance", "Starting balance for new factions."),

        // === Combat ===
        Map.entry("combat.tagDurationSeconds", "How long a player remains combat-tagged after attacking or being attacked."),
        Map.entry("combat.allyDamage", "Allow damage between allied faction members."),
        Map.entry("combat.factionDamage", "Allow damage between members of the same faction."),
        Map.entry("combat.taggedLogoutPenalty", "Apply a power penalty when combat-tagged players log out."),
        Map.entry("combat.logoutPowerLoss", "Power lost when a combat-tagged player logs out."),
        Map.entry("combat.neutralAttackPenalty", "Power penalty for attacking neutral (non-enemy) players in their territory."),
        Map.entry("combat.spawnProtection.enabled", "Grant temporary invulnerability after respawning."),
        Map.entry("combat.spawnProtection.durationSeconds", "Duration of spawn protection in seconds."),
        Map.entry("combat.spawnProtection.breakOnAttack", "Remove spawn protection if the player attacks someone."),
        Map.entry("combat.spawnProtection.breakOnMove", "Remove spawn protection if the player moves."),

        // === Chat: Format ===
        Map.entry("chat.enabled", "Enable HyperFactions chat formatting."),
        Map.entry("chat.format", "Chat format template. Placeholders: {faction_tag}, {prefix}, {player}, {suffix}, {message}."),
        Map.entry("chat.tagDisplay", "How faction identity appears in chat: 'tag' (short tag), 'name' (full name), or 'none'."),
        Map.entry("chat.tagFormat", "Format for the faction tag. Use {tag} as placeholder."),
        Map.entry("chat.noFactionTag", "Tag shown for players without a faction. Leave empty to show nothing."),
        Map.entry("chat.noFactionTagColor", "Color for the no-faction tag."),
        Map.entry("chat.priority", "Event priority for chat formatting. Higher priority runs later."),

        // === Chat: Relation Colors ===
        Map.entry("chat.relationColorOwn", "Color for your own faction members in chat."),
        Map.entry("chat.relationColorAlly", "Color for allied faction members in chat."),
        Map.entry("chat.relationColorNeutral", "Color for neutral faction members in chat."),
        Map.entry("chat.relationColorEnemy", "Color for enemy faction members in chat."),

        // === Chat: Faction Chat ===
        Map.entry("chat.factionChatColor", "Color for faction-only chat messages."),
        Map.entry("chat.factionChatPrefix", "Prefix shown before faction chat messages."),
        Map.entry("chat.allyChatColor", "Color for ally chat messages."),
        Map.entry("chat.allyChatPrefix", "Prefix shown before ally chat messages."),
        Map.entry("chat.senderNameColor", "Color for the sender's name in faction/ally chat."),
        Map.entry("chat.messageColor", "Color for the message text in faction/ally chat."),
        Map.entry("chat.historyEnabled", "Store faction chat history so players see recent messages on join."),
        Map.entry("chat.historyMaxMessages", "Maximum messages kept in chat history per faction."),
        Map.entry("chat.historyRetentionDays", "Days before old chat history is cleaned up."),
        Map.entry("chat.historyCleanupIntervalMinutes", "Minutes between chat history cleanup runs."),

        // === Modules: Backup ===
        Map.entry("backup.enabled", "Enable automatic faction data backups."),
        Map.entry("backup.hourlyRetention", "Number of hourly backups to keep."),
        Map.entry("backup.dailyRetention", "Number of daily backups to keep."),
        Map.entry("backup.weeklyRetention", "Number of weekly backups to keep."),
        Map.entry("backup.manualRetention", "Number of manual backups to keep. 0 = keep all."),
        Map.entry("backup.onShutdown", "Create a backup when the server shuts down."),
        Map.entry("backup.shutdownRetention", "Number of shutdown backups to keep."),

        // === Modules: Announcements ===
        Map.entry("announcements.enabled", "Enable server-wide faction event announcements."),
        Map.entry("announcements.factionCreated", "Announce when a new faction is created."),
        Map.entry("announcements.factionDisbanded", "Announce when a faction is disbanded."),
        Map.entry("announcements.leadershipTransfer", "Announce when faction leadership changes."),
        Map.entry("announcements.overclaim", "Announce when territory is overclaimed."),
        Map.entry("announcements.warDeclared", "Announce when war is declared between factions."),
        Map.entry("announcements.allianceFormed", "Announce when an alliance is formed."),
        Map.entry("announcements.allianceBroken", "Announce when an alliance is broken."),

        // === Modules: Gravestones ===
        Map.entry("gravestones.enabled", "Enable GravestonePlugin integration for faction territory."),
        Map.entry("gravestones.protectInOwnTerritory", "Protect gravestones in the owner's faction territory."),
        Map.entry("gravestones.factionMembersCanAccess", "Allow faction members to access each other's gravestones."),
        Map.entry("gravestones.alliesCanAccess", "Allow allied faction members to access gravestones."),
        Map.entry("gravestones.protectInSafeZone", "Protect gravestones in safe zones."),
        Map.entry("gravestones.protectInWarZone", "Protect gravestones in war zones."),
        Map.entry("gravestones.protectInWilderness", "Protect gravestones in wilderness (unclaimed) areas."),
        Map.entry("gravestones.announceDeathLocation", "Send death location to faction members when someone dies."),

        // === World Map ===
        Map.entry("worldmap.enabled", "Enable world map claim overlay integration."),
        Map.entry("worldmap.refreshMode", "How map updates are triggered: proximity (most performant), incremental, debounced, immediate, or manual."),
        Map.entry("worldmap.showFactionTags", "Display faction tag text on claimed chunks in the world map."),
        Map.entry("worldmap.autoFallbackOnError", "Automatically fall back to debounced mode if map integration encounters errors."),
        Map.entry("worldmap.factionWideRefreshThreshold", "If a faction has more claims than this, use full refresh instead of per-chunk updates."),
        Map.entry("worldmap.proximityChunkRadius", "Chunk radius for proximity refresh mode. Players within this range get map updates."),
        Map.entry("worldmap.proximityBatchIntervalTicks", "Ticks between proximity batch processing (30 ticks = 1 second at 30 TPS)."),
        Map.entry("worldmap.proximityMaxChunksPerBatch", "Maximum chunks processed per proximity batch."),
        Map.entry("worldmap.incrementalBatchIntervalTicks", "Ticks between incremental batch processing."),
        Map.entry("worldmap.incrementalMaxChunksPerBatch", "Maximum chunks processed per incremental batch."),
        Map.entry("worldmap.debouncedDelaySeconds", "Seconds to wait after last change before triggering a full map refresh."),

        // === Protection: Debug ===
        Map.entry("debug.enabled", "Enable debug logging system."),
        Map.entry("debug.enabledByDefault", "Enable all debug categories by default."),
        Map.entry("debug.logToConsole", "Output debug messages to the server console."),
        Map.entry("debug.power", "Debug power calculations and changes."),
        Map.entry("debug.claim", "Debug claim operations."),
        Map.entry("debug.combat", "Debug combat tag and PvP events."),
        Map.entry("debug.protection", "Debug territory protection checks."),
        Map.entry("debug.relation", "Debug faction relation changes."),
        Map.entry("debug.territory", "Debug territory entry/exit events."),
        Map.entry("debug.worldmap", "Debug world map overlay updates."),
        Map.entry("debug.interaction", "Debug block/entity interaction checks."),
        Map.entry("debug.mixin", "Debug mixin injection events."),
        Map.entry("debug.spawning", "Debug mob spawning control.")
    );

    /**
     * Gets the description for a config key.
     *
     * @param key the config key
     * @return description text, or null if no description exists
     */
    @Nullable
    public static String get(@NotNull String key) {
        return DESCRIPTIONS.get(key);
    }

    /**
     * Gets the description for a config key with a fallback.
     */
    @NotNull
    public static String getOrDefault(@NotNull String key, @NotNull String fallback) {
        return DESCRIPTIONS.getOrDefault(key, fallback);
    }
}
