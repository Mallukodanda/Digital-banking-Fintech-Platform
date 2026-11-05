package com.fintech.bank.entity;

/**
 * State machine statuses for financial transactions.
 */
public enum TransactionStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED,
    REVERSED
}
