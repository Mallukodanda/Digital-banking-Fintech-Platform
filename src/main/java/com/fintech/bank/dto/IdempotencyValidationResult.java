package com.fintech.bank.dto;

import com.fintech.bank.entity.IdempotencyRecord;

/**
 * Result envelope indicating whether the idempotency check succeeded,
 * hit a cached completed response, or reserved processing lock.
 */
public record IdempotencyValidationResult(
        boolean alreadyCompleted,
        Integer responseCode,
        String responseBody,
        IdempotencyRecord record
) {
    public static IdempotencyValidationResult freshLock(IdempotencyRecord record) {
        return new IdempotencyValidationResult(false, null, null, record);
    }

    public static IdempotencyValidationResult cached(Integer responseCode, String responseBody, IdempotencyRecord record) {
        return new IdempotencyValidationResult(true, responseCode, responseBody, record);
    }
}
