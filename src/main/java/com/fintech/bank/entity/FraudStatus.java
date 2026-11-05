package com.fintech.bank.entity;

/**
 * Lifecycle and review status of a fraud alert.
 */
public enum FraudStatus {
    FLAGGED,
    UNDER_REVIEW,
    DISMISSED,
    CONFIRMED_FRAUD
}
