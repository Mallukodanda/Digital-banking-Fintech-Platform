package com.fintech.bank.entity;

/**
 * Status of Transactional Outbox events.
 */
public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
