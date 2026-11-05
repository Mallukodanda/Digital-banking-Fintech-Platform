package com.fintech.bank.entity;

/**
 * Educational rule-based fraud detection rules and their default risk weights.
 */
public enum FraudRule {
    LARGE_TRANSACTION_AMOUNT("Unusually large single transfer amount exceeding baseline threshold", 50),
    RAPID_VELOCITY_TRANSFERS("Multiple rapid transfers executed in a short time window", 40),
    DORMANT_ACCOUNT_REACTIVATION("Sudden high-volume transaction on historically dormant account", 35),
    ROUND_AMOUNT_STRUCTURING("Suspicious recurring round amount transfer pattern", 25),
    KYC_TIER_LIMIT_BREACH("Transaction amount breaches customer KYC tier operational limits", 45);

    private final String description;
    private final int defaultWeight;

    FraudRule(String description, int defaultWeight) {
        this.description = description;
        this.defaultWeight = defaultWeight;
    }

    public String getDescription() {
        return description;
    }

    public int getDefaultWeight() {
        return defaultWeight;
    }
}
