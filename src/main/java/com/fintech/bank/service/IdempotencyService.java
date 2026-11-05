package com.fintech.bank.service;

import com.fintech.bank.dto.IdempotencyValidationResult;

import java.time.Duration;

/**
 * Service managing API request idempotency records, fingerprint verification,
 * processing locks, response caching, and TTL lifecycle management.
 */
public interface IdempotencyService {

    /**
     * Atomically validates an idempotency key or reserves a processing lock.
     *
     * @param idempotencyKey Client-supplied unique token
     * @param requestPath    HTTP endpoint path
     * @param requestHash    SHA-256 fingerprint of the request payload and actor
     * @param ttl            Time to live duration before the key expires
     * @return IdempotencyValidationResult containing cached response or fresh lock indicator
     */
    IdempotencyValidationResult validateOrAcquireLock(String idempotencyKey, String requestPath, String requestHash, Duration ttl);

    /**
     * Marks the idempotency record as COMPLETED and caches the resulting payload.
     *
     * @param idempotencyKey  Client-supplied unique token
     * @param responseCode    HTTP status code (e.g., 200, 201)
     * @param responsePayload Response DTO object to serialize and store
     */
    void complete(String idempotencyKey, int responseCode, Object responsePayload);

    /**
     * Marks the idempotency record as FAILED when an unrecoverable exception occurs.
     *
     * @param idempotencyKey Client-supplied unique token
     * @param responseCode   HTTP error status code
     * @param errorMessage   Error message description
     */
    void fail(String idempotencyKey, int responseCode, String errorMessage);

    /**
     * Unlocks or removes an in-flight idempotency lock upon rollback or transient retryable error.
     *
     * @param idempotencyKey Client-supplied unique token
     */
    void release(String idempotencyKey);

    /**
     * Purges expired idempotency records past their TTL timestamp.
     *
     * @return Number of deleted records
     */
    int purgeExpiredRecords();
}
