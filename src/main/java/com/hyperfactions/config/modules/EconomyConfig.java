package com.hyperfactions.config.modules;

import com.google.gson.JsonObject;
import com.hyperfactions.config.ModuleConfig;
import com.hyperfactions.config.ValidationResult;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Configuration for the faction economy system.
 * <p>
 * Controls currency display and starting balances for factions.
 */
public class EconomyConfig extends ModuleConfig {

    private String currencyName = "dollar";
    private String currencyNamePlural = "dollars";
    private String currencySymbol = "$";
    private double startingBalance = 0.0;

    /**
     * Creates a new economy config.
     *
     * @param filePath path to config/economy.json
     */
    public EconomyConfig(@NotNull Path filePath) {
        super(filePath);
    }

    @Override
    @NotNull
    public String getModuleName() {
        return "economy";
    }

    @Override
    protected void createDefaults() {
        enabled = true;
        currencyName = "dollar";
        currencyNamePlural = "dollars";
        currencySymbol = "$";
        startingBalance = 0.0;
    }

    @Override
    protected void loadModuleSettings(@NotNull JsonObject root) {
        currencyName = getString(root, "currencyName", currencyName);
        currencyNamePlural = getString(root, "currencyNamePlural", currencyNamePlural);
        currencySymbol = getString(root, "currencySymbol", currencySymbol);
        startingBalance = getDouble(root, "startingBalance", startingBalance);
    }

    @Override
    protected void writeModuleSettings(@NotNull JsonObject root) {
        root.addProperty("currencyName", currencyName);
        root.addProperty("currencyNamePlural", currencyNamePlural);
        root.addProperty("currencySymbol", currencySymbol);
        root.addProperty("startingBalance", startingBalance);
    }

    // === Getters ===

    /**
     * Gets the singular currency name (e.g., "dollar").
     *
     * @return currency name
     */
    @NotNull
    public String getCurrencyName() {
        return currencyName;
    }

    /**
     * Gets the plural currency name (e.g., "dollars").
     *
     * @return plural currency name
     */
    @NotNull
    public String getCurrencyNamePlural() {
        return currencyNamePlural;
    }

    /**
     * Gets the currency symbol (e.g., "$").
     *
     * @return currency symbol
     */
    @NotNull
    public String getCurrencySymbol() {
        return currencySymbol;
    }

    /**
     * Gets the starting balance for new factions.
     *
     * @return starting balance
     */
    public double getStartingBalance() {
        return startingBalance;
    }

    /**
     * Formats an amount with the currency symbol.
     *
     * @param amount the amount to format
     * @return formatted string (e.g., "$100.00")
     */
    @NotNull
    public String format(double amount) {
        return currencySymbol + String.format("%.2f", amount);
    }

    /**
     * Formats an amount with the currency name.
     *
     * @param amount the amount to format
     * @return formatted string (e.g., "100 dollars")
     */
    @NotNull
    public String formatWithName(double amount) {
        String name = amount == 1.0 ? currencyName : currencyNamePlural;
        return String.format("%.2f %s", amount, name);
    }

    // === Validation ===

    @Override
    @NotNull
    public ValidationResult validate() {
        ValidationResult result = new ValidationResult();

        // Starting balance must be >= 0
        startingBalance = validateMin(result, "startingBalance", startingBalance, 0.0, 0.0);

        // Currency names should not be empty
        if (currencyName.isBlank()) {
            result.addWarning(getConfigName(), "currencyName",
                    "Currency name should not be empty", currencyName, "dollar");
            currencyName = "dollar";
            needsSave = true;
        }
        if (currencyNamePlural.isBlank()) {
            result.addWarning(getConfigName(), "currencyNamePlural",
                    "Currency name plural should not be empty", currencyNamePlural, "dollars");
            currencyNamePlural = "dollars";
            needsSave = true;
        }

        return result;
    }
}
