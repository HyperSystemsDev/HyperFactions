package com.hyperfactions.data;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents a player's power data for faction mechanics.
 *
 * @param uuid      the player's UUID
 * @param power     the current power level
 * @param maxPower  the maximum power this player can have
 * @param lastDeath when the player last died (epoch millis, 0 if never)
 * @param lastRegen when power was last regenerated (epoch millis)
 */
public record PlayerPower(
    @NotNull UUID uuid,
    double power,
    double maxPower,
    long lastDeath,
    long lastRegen
) {
    /**
     * Creates a new PlayerPower with default values.
     *
     * @param uuid         the player's UUID
     * @param startingPower the starting power value
     * @param maxPower     the maximum power
     * @return a new PlayerPower
     */
    public static PlayerPower create(@NotNull UUID uuid, double startingPower, double maxPower) {
        long now = System.currentTimeMillis();
        return new PlayerPower(uuid, startingPower, maxPower, 0, now);
    }

    /**
     * Creates a copy with updated power.
     *
     * @param newPower the new power value
     * @return a new PlayerPower with clamped power
     */
    public PlayerPower withPower(double newPower) {
        double clamped = Math.max(0, Math.min(maxPower, newPower));
        return new PlayerPower(uuid, clamped, maxPower, lastDeath, lastRegen);
    }

    /**
     * Creates a copy with power reduced by the death penalty.
     *
     * @param penalty the amount to reduce
     * @return a new PlayerPower with reduced power
     */
    public PlayerPower withDeathPenalty(double penalty) {
        double newPower = Math.max(0, power - penalty);
        return new PlayerPower(uuid, newPower, maxPower, System.currentTimeMillis(), lastRegen);
    }

    /**
     * Creates a copy with power increased by regeneration.
     *
     * @param amount the amount to add
     * @return a new PlayerPower with increased power
     */
    public PlayerPower withRegen(double amount) {
        double newPower = Math.min(maxPower, power + amount);
        return new PlayerPower(uuid, newPower, maxPower, lastDeath, System.currentTimeMillis());
    }

    /**
     * Creates a copy with updated max power.
     *
     * @param newMaxPower the new max power
     * @return a new PlayerPower with updated max
     */
    public PlayerPower withMaxPower(double newMaxPower) {
        double newPower = Math.min(power, newMaxPower);
        return new PlayerPower(uuid, newPower, newMaxPower, lastDeath, lastRegen);
    }

    /**
     * Gets the power as an integer (floored).
     *
     * @return the floored power value
     */
    public int getPowerInt() {
        return (int) Math.floor(power);
    }

    /**
     * Gets the max power as an integer (floored).
     *
     * @return the floored max power value
     */
    public int getMaxPowerInt() {
        return (int) Math.floor(maxPower);
    }

    /**
     * Checks if power is at maximum.
     *
     * @return true if power >= maxPower
     */
    public boolean isAtMax() {
        return power >= maxPower;
    }

    /**
     * Gets the power percentage (0-100).
     *
     * @return the power percentage
     */
    public int getPowerPercent() {
        if (maxPower <= 0) return 0;
        return (int) Math.round((power / maxPower) * 100);
    }
}
