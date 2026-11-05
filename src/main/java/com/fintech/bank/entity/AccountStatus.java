package com.fintech.bank.entity;

/**
 * Lifecycle status of a bank account.
 */
public enum AccountStatus {
    PENDING_ACTIVATION,
    ACTIVE,
    FROZEN,
    DORMANT,
    CLOSED
}
