package com.fintech.bank.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility class providing exact decimal arithmetic and Banker's Rounding standard.
 */
public final class MoneyUtils {

    public static final int INTERNAL_SCALE = 4;
    public static final int DISPLAY_SCALE = 2;
    public static final RoundingMode FINANCIAL_ROUNDING = RoundingMode.HALF_EVEN;

    private MoneyUtils() {
        // Prevent instantiation
    }

    /**
     * Formats and scales an amount to the internal banking calculation standard (4 decimal places).
     */
    public static BigDecimal formatInternal(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(INTERNAL_SCALE, FINANCIAL_ROUNDING);
        }
        return amount.setScale(INTERNAL_SCALE, FINANCIAL_ROUNDING);
    }

    /**
     * Formats an amount to customer display standard (2 decimal places).
     */
    public static BigDecimal formatDisplay(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(DISPLAY_SCALE, FINANCIAL_ROUNDING);
        }
        return amount.setScale(DISPLAY_SCALE, FINANCIAL_ROUNDING);
    }

    /**
     * Checks if amount is strictly positive (> 0.0000).
     */
    public static boolean isPositive(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if available balance is sufficient for requested debit.
     */
    public static boolean isSufficient(BigDecimal availableBalance, BigDecimal requestedAmount) {
        if (availableBalance == null || requestedAmount == null) {
            return false;
        }
        return availableBalance.compareTo(requestedAmount) >= 0;
    }
}
