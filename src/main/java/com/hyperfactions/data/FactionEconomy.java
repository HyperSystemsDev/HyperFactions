package com.hyperfactions.data;

import com.hyperfactions.api.EconomyAPI;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Represents a faction's economy data.
 */
public record FactionEconomy(
    double balance,
    @NotNull List<EconomyAPI.Transaction> transactionHistory
) {
    /**
     * Maximum number of transactions to keep in history.
     */
    public static final int MAX_HISTORY = 50;

    /**
     * Creates an empty economy with zero balance.
     *
     * @return a new empty FactionEconomy
     */
    public static FactionEconomy empty() {
        return new FactionEconomy(0.0, new ArrayList<>());
    }

    /**
     * Creates an economy with a starting balance.
     *
     * @param startingBalance the starting balance
     * @return a new FactionEconomy
     */
    public static FactionEconomy withStartingBalance(double startingBalance) {
        return new FactionEconomy(startingBalance, new ArrayList<>());
    }

    /**
     * Defensive copy constructor.
     */
    public FactionEconomy {
        // Defensive copy
        transactionHistory = new ArrayList<>(transactionHistory);
    }

    /**
     * Returns a copy with updated balance.
     *
     * @param newBalance the new balance
     * @return a new FactionEconomy with the updated balance
     */
    public FactionEconomy withBalance(double newBalance) {
        return new FactionEconomy(newBalance, transactionHistory);
    }

    /**
     * Returns a copy with added transaction.
     *
     * @param transaction the transaction to add
     * @return a new FactionEconomy with the transaction added
     */
    public FactionEconomy withTransaction(@NotNull EconomyAPI.Transaction transaction) {
        List<EconomyAPI.Transaction> newHistory = new ArrayList<>(transactionHistory);
        newHistory.add(0, transaction); // Add to front (most recent first)
        
        // Trim if exceeds max history
        while (newHistory.size() > MAX_HISTORY) {
            newHistory.remove(newHistory.size() - 1);
        }
        
        return new FactionEconomy(balance, newHistory);
    }

    /**
     * Returns a copy with updated balance and added transaction.
     *
     * @param newBalance  the new balance
     * @param transaction the transaction to add
     * @return a new FactionEconomy
     */
    public FactionEconomy withBalanceAndTransaction(double newBalance, 
                                                     @NotNull EconomyAPI.Transaction transaction) {
        return withBalance(newBalance).withTransaction(transaction);
    }

    /**
     * Checks if there are sufficient funds for a withdrawal.
     *
     * @param amount the amount to check
     * @return true if balance >= amount
     */
    public boolean hasFunds(double amount) {
        return balance >= amount;
    }

    /**
     * Gets the recent transaction history.
     *
     * @param limit maximum number of transactions to return
     * @return list of transactions (most recent first)
     */
    @NotNull
    public List<EconomyAPI.Transaction> getRecentTransactions(int limit) {
        if (limit <= 0) return Collections.emptyList();
        if (limit >= transactionHistory.size()) {
            return Collections.unmodifiableList(transactionHistory);
        }
        return Collections.unmodifiableList(transactionHistory.subList(0, limit));
    }

    /**
     * Gets an unmodifiable view of the transaction history.
     *
     * @return unmodifiable list of transactions
     */
    @NotNull
    public List<EconomyAPI.Transaction> transactionHistory() {
        return Collections.unmodifiableList(transactionHistory);
    }
}
