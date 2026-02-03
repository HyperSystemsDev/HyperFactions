package com.hyperfactions.manager;

import com.hyperfactions.api.EconomyAPI;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionEconomy;
import com.hyperfactions.data.FactionLog;
import com.hyperfactions.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Manages faction treasury and economy operations.
 */
public class EconomyManager implements EconomyAPI {

    private final FactionManager factionManager;

    // Cache for economy data (stored on Faction record)
    private final Map<UUID, FactionEconomy> economyCache = new HashMap<>();

    // Currency formatting
    private final NumberFormat currencyFormat;

    public EconomyManager(@NotNull FactionManager factionManager) {
        this.factionManager = factionManager;
        this.currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    }

    /**
     * Initializes economy data for a faction.
     *
     * @param factionId the faction ID
     */
    public void initializeFaction(@NotNull UUID factionId) {
        if (!economyCache.containsKey(factionId)) {
            economyCache.put(factionId, FactionEconomy.empty());
        }
    }

    /**
     * Cleans up economy data when a faction is disbanded.
     *
     * @param factionId the faction ID
     */
    public void removeFaction(@NotNull UUID factionId) {
        economyCache.remove(factionId);
    }

    /**
     * Gets the economy data for a faction.
     *
     * @param factionId the faction ID
     * @return the economy data, or null if faction not found
     */
    @Nullable
    public FactionEconomy getEconomy(@NotNull UUID factionId) {
        return economyCache.get(factionId);
    }

    /**
     * Loads economy data from all factions.
     * Should be called after FactionManager loads.
     */
    public void loadAll() {
        for (Faction faction : factionManager.getAllFactions()) {
            // Initialize with empty economy if not present
            economyCache.put(faction.id(), FactionEconomy.empty());
        }
        Logger.info("Loaded economy data for %d factions", economyCache.size());
    }

    // === EconomyAPI Implementation ===

    @Override
    public double getFactionBalance(@NotNull UUID factionId) {
        FactionEconomy economy = economyCache.get(factionId);
        return economy != null ? economy.balance() : 0.0;
    }

    @Override
    public boolean hasFunds(@NotNull UUID factionId, double amount) {
        if (amount <= 0) return true;
        FactionEconomy economy = economyCache.get(factionId);
        return economy != null && economy.hasFunds(amount);
    }

    @Override
    @NotNull
    public CompletableFuture<TransactionResult> deposit(
        @NotNull UUID factionId,
        double amount,
        @Nullable UUID actorId,
        @NotNull String description
    ) {
        return CompletableFuture.supplyAsync(() -> {
            if (amount <= 0) {
                return TransactionResult.INVALID_AMOUNT;
            }

            Faction faction = factionManager.getFaction(factionId);
            if (faction == null) {
                return TransactionResult.FACTION_NOT_FOUND;
            }

            FactionEconomy economy = economyCache.get(factionId);
            if (economy == null) {
                economy = FactionEconomy.empty();
            }

            double newBalance = economy.balance() + amount;
            Transaction transaction = new Transaction(
                factionId,
                actorId,
                TransactionType.DEPOSIT,
                amount,
                newBalance,
                System.currentTimeMillis(),
                description
            );

            FactionEconomy updated = economy.withBalanceAndTransaction(newBalance, transaction);
            economyCache.put(factionId, updated);

            // Log to faction
            String logMessage = String.format("Deposit: %s (+%s)", 
                formatCurrency(newBalance), formatCurrency(amount));
            Faction updatedFaction = faction.withLog(
                FactionLog.create(FactionLog.LogType.ECONOMY, logMessage, actorId)
            );
            factionManager.updateFaction(updatedFaction);

            Logger.debug("Deposit to %s: %s (new balance: %s)", 
                faction.name(), formatCurrency(amount), formatCurrency(newBalance));

            return TransactionResult.SUCCESS;
        });
    }

    @Override
    @NotNull
    public CompletableFuture<TransactionResult> withdraw(
        @NotNull UUID factionId,
        double amount,
        @NotNull UUID actorId,
        @NotNull String description
    ) {
        return CompletableFuture.supplyAsync(() -> {
            if (amount <= 0) {
                return TransactionResult.INVALID_AMOUNT;
            }

            Faction faction = factionManager.getFaction(factionId);
            if (faction == null) {
                return TransactionResult.FACTION_NOT_FOUND;
            }

            // Check permission (officer+)
            var member = faction.getMember(actorId);
            if (member == null) {
                return TransactionResult.NOT_IN_FACTION;
            }
            if (!member.isOfficerOrHigher()) {
                return TransactionResult.NO_PERMISSION;
            }

            FactionEconomy economy = economyCache.get(factionId);
            if (economy == null || !economy.hasFunds(amount)) {
                return TransactionResult.INSUFFICIENT_FUNDS;
            }

            double newBalance = economy.balance() - amount;
            Transaction transaction = new Transaction(
                factionId,
                actorId,
                TransactionType.WITHDRAW,
                amount,
                newBalance,
                System.currentTimeMillis(),
                description
            );

            FactionEconomy updated = economy.withBalanceAndTransaction(newBalance, transaction);
            economyCache.put(factionId, updated);

            // Log to faction
            String logMessage = String.format("Withdrawal: %s (-%s)", 
                formatCurrency(newBalance), formatCurrency(amount));
            Faction updatedFaction = faction.withLog(
                FactionLog.create(FactionLog.LogType.ECONOMY, logMessage, actorId)
            );
            factionManager.updateFaction(updatedFaction);

            Logger.debug("Withdrawal from %s: %s (new balance: %s)", 
                faction.name(), formatCurrency(amount), formatCurrency(newBalance));

            return TransactionResult.SUCCESS;
        });
    }

    @Override
    @NotNull
    public CompletableFuture<TransactionResult> transfer(
        @NotNull UUID fromFactionId,
        @NotNull UUID toFactionId,
        double amount,
        @Nullable UUID actorId,
        @NotNull String description
    ) {
        return CompletableFuture.supplyAsync(() -> {
            if (amount <= 0) {
                return TransactionResult.INVALID_AMOUNT;
            }

            Faction fromFaction = factionManager.getFaction(fromFactionId);
            Faction toFaction = factionManager.getFaction(toFactionId);

            if (fromFaction == null || toFaction == null) {
                return TransactionResult.FACTION_NOT_FOUND;
            }

            FactionEconomy fromEconomy = economyCache.get(fromFactionId);
            if (fromEconomy == null || !fromEconomy.hasFunds(amount)) {
                return TransactionResult.INSUFFICIENT_FUNDS;
            }

            FactionEconomy toEconomy = economyCache.get(toFactionId);
            if (toEconomy == null) {
                toEconomy = FactionEconomy.empty();
            }

            // Perform transfer
            double fromNewBalance = fromEconomy.balance() - amount;
            double toNewBalance = toEconomy.balance() + amount;

            Transaction fromTransaction = new Transaction(
                fromFactionId,
                actorId,
                TransactionType.TRANSFER_OUT,
                amount,
                fromNewBalance,
                System.currentTimeMillis(),
                "Transfer to " + toFaction.name() + ": " + description
            );

            Transaction toTransaction = new Transaction(
                toFactionId,
                actorId,
                TransactionType.TRANSFER_IN,
                amount,
                toNewBalance,
                System.currentTimeMillis(),
                "Transfer from " + fromFaction.name() + ": " + description
            );

            // Update both economies
            economyCache.put(fromFactionId, fromEconomy.withBalanceAndTransaction(fromNewBalance, fromTransaction));
            economyCache.put(toFactionId, toEconomy.withBalanceAndTransaction(toNewBalance, toTransaction));

            Logger.debug("Transfer from %s to %s: %s", 
                fromFaction.name(), toFaction.name(), formatCurrency(amount));

            return TransactionResult.SUCCESS;
        });
    }

    @Override
    @NotNull
    public List<Transaction> getTransactionHistory(@NotNull UUID factionId, int limit) {
        FactionEconomy economy = economyCache.get(factionId);
        if (economy == null) {
            return Collections.emptyList();
        }
        return economy.getRecentTransactions(limit);
    }

    @Override
    @NotNull
    public String getCurrencyName() {
        return ConfigManager.get().getEconomyCurrencyName();
    }

    @Override
    @NotNull
    public String getCurrencyNamePlural() {
        return ConfigManager.get().getEconomyCurrencyNamePlural();
    }

    @Override
    @NotNull
    public String formatCurrency(double amount) {
        String symbol = ConfigManager.get().getEconomyCurrencySymbol();
        return String.format("%s%.2f", symbol, amount);
    }

    @Override
    public boolean isEnabled() {
        return ConfigManager.get().isEconomyEnabled();
    }

    /**
     * Performs a system deposit (no actor, for rewards/adjustments).
     *
     * @param factionId   the faction ID
     * @param amount      the amount
     * @param type        the transaction type
     * @param description description
     * @return the result
     */
    @NotNull
    public TransactionResult systemDeposit(
        @NotNull UUID factionId,
        double amount,
        @NotNull TransactionType type,
        @NotNull String description
    ) {
        if (amount <= 0) {
            return TransactionResult.INVALID_AMOUNT;
        }

        Faction faction = factionManager.getFaction(factionId);
        if (faction == null) {
            return TransactionResult.FACTION_NOT_FOUND;
        }

        FactionEconomy economy = economyCache.getOrDefault(factionId, FactionEconomy.empty());
        double newBalance = economy.balance() + amount;

        Transaction transaction = new Transaction(
            factionId,
            null, // System
            type,
            amount,
            newBalance,
            System.currentTimeMillis(),
            description
        );

        economyCache.put(factionId, economy.withBalanceAndTransaction(newBalance, transaction));
        return TransactionResult.SUCCESS;
    }

    /**
     * Performs a system withdrawal (no actor, for upkeep/costs).
     *
     * @param factionId   the faction ID
     * @param amount      the amount
     * @param type        the transaction type
     * @param description description
     * @return the result
     */
    @NotNull
    public TransactionResult systemWithdraw(
        @NotNull UUID factionId,
        double amount,
        @NotNull TransactionType type,
        @NotNull String description
    ) {
        if (amount <= 0) {
            return TransactionResult.INVALID_AMOUNT;
        }

        FactionEconomy economy = economyCache.get(factionId);
        if (economy == null || !economy.hasFunds(amount)) {
            return TransactionResult.INSUFFICIENT_FUNDS;
        }

        double newBalance = economy.balance() - amount;
        Transaction transaction = new Transaction(
            factionId,
            null, // System
            type,
            amount,
            newBalance,
            System.currentTimeMillis(),
            description
        );

        economyCache.put(factionId, economy.withBalanceAndTransaction(newBalance, transaction));
        return TransactionResult.SUCCESS;
    }
}
