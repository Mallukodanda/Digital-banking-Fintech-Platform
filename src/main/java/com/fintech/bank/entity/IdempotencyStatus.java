package com.fintech.bank.entity;

/**
 * Processing state for API request idempotency tracking.
 */
public enum IdempotencyStatus {
    PROCESSING,
    COMPLETED,
    FAILED
}
