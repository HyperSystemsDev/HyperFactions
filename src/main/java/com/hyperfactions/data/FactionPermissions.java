package com.hyperfactions.data;

import org.jetbrains.annotations.NotNull;

/**
 * Represents configurable territory permissions for a faction.
 * Controls what outsiders, allies, and members can do in faction territory.
 *
 * @param outsiderBreak      Can outsiders break blocks (default: false)
 * @param outsiderPlace      Can outsiders place blocks (default: false)
 * @param outsiderInteract   Can outsiders use doors/buttons/etc (default: false)
 * @param allyBreak          Can allies break blocks (default: false)
 * @param allyPlace          Can allies place blocks (default: false)
 * @param allyInteract       Can allies use doors/buttons/etc (default: true)
 * @param memberBreak        Can members break blocks (default: true)
 * @param memberPlace        Can members place blocks (default: true)
 * @param memberInteract     Can members use doors/buttons/etc (default: true)
 * @param pvpEnabled         Is PvP allowed in territory (default: true)
 * @param officersCanEdit    Can officers edit these permissions (default: false)
 */
public record FactionPermissions(
    boolean outsiderBreak,
    boolean outsiderPlace,
    boolean outsiderInteract,
    boolean allyBreak,
    boolean allyPlace,
    boolean allyInteract,
    boolean memberBreak,
    boolean memberPlace,
    boolean memberInteract,
    boolean pvpEnabled,
    boolean officersCanEdit
) {

    /**
     * Creates a FactionPermissions with default values.
     * Outsiders: deny all
     * Allies: can interact only
     * Members: full access
     * PvP: enabled
     * Officers: cannot edit
     *
     * @return default permissions
     */
    @NotNull
    public static FactionPermissions defaults() {
        return new FactionPermissions(
            false, false, false,  // outsiders: deny all
            false, false, true,   // allies: can interact only
            true, true, true,     // members: full access
            true,                 // pvp enabled
            false                 // officers cannot edit by default
        );
    }

    // === Outsider permission builders ===

    /**
     * Creates a copy with updated outsiderBreak value.
     */
    public FactionPermissions withOutsiderBreak(boolean value) {
        return new FactionPermissions(value, outsiderPlace, outsiderInteract,
            allyBreak, allyPlace, allyInteract,
            memberBreak, memberPlace, memberInteract,
            pvpEnabled, officersCanEdit);
    }

    /**
     * Creates a copy with updated outsiderPlace value.
     */
    public FactionPermissions withOutsiderPlace(boolean value) {
        return new FactionPermissions(outsiderBreak, value, outsiderInteract,
            allyBreak, allyPlace, allyInteract,
            memberBreak, memberPlace, memberInteract,
            pvpEnabled, officersCanEdit);
    }

    /**
     * Creates a copy with updated outsiderInteract value.
     */
    public FactionPermissions withOutsiderInteract(boolean value) {
        return new FactionPermissions(outsiderBreak, outsiderPlace, value,
            allyBreak, allyPlace, allyInteract,
            memberBreak, memberPlace, memberInteract,
            pvpEnabled, officersCanEdit);
    }

    // === Ally permission builders ===

    /**
     * Creates a copy with updated allyBreak value.
     */
    public FactionPermissions withAllyBreak(boolean value) {
        return new FactionPermissions(outsiderBreak, outsiderPlace, outsiderInteract,
            value, allyPlace, allyInteract,
            memberBreak, memberPlace, memberInteract,
            pvpEnabled, officersCanEdit);
    }

    /**
     * Creates a copy with updated allyPlace value.
     */
    public FactionPermissions withAllyPlace(boolean value) {
        return new FactionPermissions(outsiderBreak, outsiderPlace, outsiderInteract,
            allyBreak, value, allyInteract,
            memberBreak, memberPlace, memberInteract,
            pvpEnabled, officersCanEdit);
    }

    /**
     * Creates a copy with updated allyInteract value.
     */
    public FactionPermissions withAllyInteract(boolean value) {
        return new FactionPermissions(outsiderBreak, outsiderPlace, outsiderInteract,
            allyBreak, allyPlace, value,
            memberBreak, memberPlace, memberInteract,
            pvpEnabled, officersCanEdit);
    }

    // === Member permission builders ===

    /**
     * Creates a copy with updated memberBreak value.
     */
    public FactionPermissions withMemberBreak(boolean value) {
        return new FactionPermissions(outsiderBreak, outsiderPlace, outsiderInteract,
            allyBreak, allyPlace, allyInteract,
            value, memberPlace, memberInteract,
            pvpEnabled, officersCanEdit);
    }

    /**
     * Creates a copy with updated memberPlace value.
     */
    public FactionPermissions withMemberPlace(boolean value) {
        return new FactionPermissions(outsiderBreak, outsiderPlace, outsiderInteract,
            allyBreak, allyPlace, allyInteract,
            memberBreak, value, memberInteract,
            pvpEnabled, officersCanEdit);
    }

    /**
     * Creates a copy with updated memberInteract value.
     */
    public FactionPermissions withMemberInteract(boolean value) {
        return new FactionPermissions(outsiderBreak, outsiderPlace, outsiderInteract,
            allyBreak, allyPlace, allyInteract,
            memberBreak, memberPlace, value,
            pvpEnabled, officersCanEdit);
    }

    // === Combat permission builders ===

    /**
     * Creates a copy with updated pvpEnabled value.
     */
    public FactionPermissions withPvpEnabled(boolean value) {
        return new FactionPermissions(outsiderBreak, outsiderPlace, outsiderInteract,
            allyBreak, allyPlace, allyInteract,
            memberBreak, memberPlace, memberInteract,
            value, officersCanEdit);
    }

    // === Access control builders ===

    /**
     * Creates a copy with updated officersCanEdit value.
     */
    public FactionPermissions withOfficersCanEdit(boolean value) {
        return new FactionPermissions(outsiderBreak, outsiderPlace, outsiderInteract,
            allyBreak, allyPlace, allyInteract,
            memberBreak, memberPlace, memberInteract,
            pvpEnabled, value);
    }

    /**
     * Toggles a permission by name and returns a new FactionPermissions.
     *
     * @param permissionName the permission to toggle
     * @return new FactionPermissions with the toggled value, or same instance if name unknown
     */
    @NotNull
    public FactionPermissions toggle(@NotNull String permissionName) {
        return switch (permissionName) {
            case "outsiderBreak" -> withOutsiderBreak(!outsiderBreak);
            case "outsiderPlace" -> withOutsiderPlace(!outsiderPlace);
            case "outsiderInteract" -> withOutsiderInteract(!outsiderInteract);
            case "allyBreak" -> withAllyBreak(!allyBreak);
            case "allyPlace" -> withAllyPlace(!allyPlace);
            case "allyInteract" -> withAllyInteract(!allyInteract);
            case "memberBreak" -> withMemberBreak(!memberBreak);
            case "memberPlace" -> withMemberPlace(!memberPlace);
            case "memberInteract" -> withMemberInteract(!memberInteract);
            case "pvpEnabled" -> withPvpEnabled(!pvpEnabled);
            case "officersCanEdit" -> withOfficersCanEdit(!officersCanEdit);
            default -> this;
        };
    }

    /**
     * Gets a permission value by name.
     *
     * @param permissionName the permission name
     * @return the permission value, or false if unknown
     */
    public boolean get(@NotNull String permissionName) {
        return switch (permissionName) {
            case "outsiderBreak" -> outsiderBreak;
            case "outsiderPlace" -> outsiderPlace;
            case "outsiderInteract" -> outsiderInteract;
            case "allyBreak" -> allyBreak;
            case "allyPlace" -> allyPlace;
            case "allyInteract" -> allyInteract;
            case "memberBreak" -> memberBreak;
            case "memberPlace" -> memberPlace;
            case "memberInteract" -> memberInteract;
            case "pvpEnabled" -> pvpEnabled;
            case "officersCanEdit" -> officersCanEdit;
            default -> false;
        };
    }
}
