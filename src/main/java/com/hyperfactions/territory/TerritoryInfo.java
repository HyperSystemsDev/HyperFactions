package com.hyperfactions.territory;

import com.hyperfactions.data.RelationType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents information about a territory at a specific location.
 * Used for tracking player position and territory change notifications.
 *
 * @param type        the type of territory
 * @param factionId   the faction ID if this is a faction claim, null otherwise
 * @param factionName the faction name if this is a faction claim, null otherwise
 * @param factionTag  the faction tag if this is a faction claim, null otherwise
 * @param relation    the player's relation to this territory's owner, null for non-faction territories
 */
public record TerritoryInfo(
    @NotNull TerritoryType type,
    @Nullable UUID factionId,
    @Nullable String factionName,
    @Nullable String factionTag,
    @Nullable RelationType relation
) {
    /**
     * Types of territories.
     */
    public enum TerritoryType {
        /** Unclaimed wilderness */
        WILDERNESS("Wilderness", "#AAAAAA"),
        /** Admin-protected SafeZone */
        SAFEZONE("SafeZone", "#55FF55"),
        /** Admin PvP zone */
        WARZONE("WarZone", "#FF5555"),
        /** Faction-claimed territory */
        FACTION_CLAIM("Faction Claim", "#55FFFF");

        private final String displayName;
        private final String defaultColor;

        TerritoryType(String displayName, String defaultColor) {
            this.displayName = displayName;
            this.defaultColor = defaultColor;
        }

        /**
         * Gets the display name of this territory type.
         *
         * @return the display name
         */
        @NotNull
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Gets the default color for this territory type.
         *
         * @return the hex color code
         */
        @NotNull
        public String getDefaultColor() {
            return defaultColor;
        }
    }

    /**
     * Creates TerritoryInfo for wilderness.
     *
     * @return wilderness territory info
     */
    public static TerritoryInfo wilderness() {
        return new TerritoryInfo(TerritoryType.WILDERNESS, null, null, null, null);
    }

    /**
     * Creates TerritoryInfo for a SafeZone.
     *
     * @param zoneName the zone name
     * @return safezone territory info
     */
    public static TerritoryInfo safeZone(@NotNull String zoneName) {
        return new TerritoryInfo(TerritoryType.SAFEZONE, null, zoneName, null, null);
    }

    /**
     * Creates TerritoryInfo for a WarZone.
     *
     * @param zoneName the zone name
     * @return warzone territory info
     */
    public static TerritoryInfo warZone(@NotNull String zoneName) {
        return new TerritoryInfo(TerritoryType.WARZONE, null, zoneName, null, null);
    }

    /**
     * Creates TerritoryInfo for a faction claim.
     *
     * @param factionId   the faction ID
     * @param factionName the faction name
     * @param factionTag  the faction tag (may be null)
     * @param relation    the player's relation to this faction
     * @return faction claim territory info
     */
    public static TerritoryInfo factionClaim(
            @NotNull UUID factionId,
            @NotNull String factionName,
            @Nullable String factionTag,
            @NotNull RelationType relation) {
        return new TerritoryInfo(TerritoryType.FACTION_CLAIM, factionId, factionName, factionTag, relation);
    }

    /**
     * Checks if this territory is different from another territory.
     * Used to determine if a notification should be sent.
     *
     * @param other the other territory info (can be null for initial entry)
     * @return true if territories are different and notification should be sent
     */
    public boolean isDifferentFrom(@Nullable TerritoryInfo other) {
        if (other == null) {
            return true; // First territory always triggers notification
        }

        // Different territory types always count as different
        if (this.type != other.type) {
            return true;
        }

        // For faction claims, check if faction changed
        if (this.type == TerritoryType.FACTION_CLAIM) {
            return !Objects.equals(this.factionId, other.factionId);
        }

        // For zones (SafeZone/WarZone), check if zone name changed
        if (this.type == TerritoryType.SAFEZONE || this.type == TerritoryType.WARZONE) {
            return !Objects.equals(this.factionName, other.factionName);
        }

        // Same wilderness
        return false;
    }

    /**
     * Gets the display color for this territory based on type and relation.
     *
     * @return the hex color code
     */
    @NotNull
    public String getDisplayColor() {
        if (type == TerritoryType.FACTION_CLAIM && relation != null) {
            return switch (relation) {
                case OWN -> "#55FF55";     // Green for own faction
                case ALLY -> "#55FF55";    // Green for ally
                case NEUTRAL -> "#FFFF55"; // Yellow for neutral
                case ENEMY -> "#FF5555";   // Red for enemy
            };
        }
        return type.getDefaultColor();
    }

    /**
     * Gets the primary display text for the notification.
     * For faction claims, includes the tag if available (e.g., "FactionName [TAG]").
     *
     * @return the primary display text
     */
    @NotNull
    public String getPrimaryText() {
        return switch (type) {
            case WILDERNESS -> "Wilderness";
            case SAFEZONE -> factionName != null ? factionName : "SafeZone";
            case WARZONE -> factionName != null ? factionName : "WarZone";
            case FACTION_CLAIM -> {
                if (factionName == null) {
                    yield "Unknown Faction";
                }
                if (factionTag != null && !factionTag.isEmpty()) {
                    yield factionName + " [" + factionTag + "]";
                }
                yield factionName;
            }
        };
    }

    /**
     * Gets the secondary display text for the notification.
     * Includes territory type and special status.
     *
     * @return the secondary display text, or null if none
     */
    @Nullable
    public String getSecondaryText() {
        return switch (type) {
            case WILDERNESS -> null;
            case SAFEZONE -> "PvP Disabled";
            case WARZONE -> "PvP Enabled - No Protection";
            case FACTION_CLAIM -> {
                if (relation == RelationType.OWN) {
                    yield "Your Territory";
                }
                if (relation != null) {
                    yield relation.getDisplayName() + " Territory";
                }
                yield "Faction Territory";
            }
        };
    }
}
