package com.fintech.bank.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.bank.dto.IdempotencyValidationResult;
import com.fintech.bank.entity.IdempotencyRecord;
import com.fintech.bank.entity.IdempotencyStatus;
import com.fintech.bank.exception.IdempotencyConflictException;
import com.fintech.bank.exception.IdempotentRequestInProgressException;
import com.fintech.bank.repository.IdempotencyRecordRepository;
import com.fintech.bank.service.IdempotencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Production implementation of IdempotencyService.
 * Guarantees exactly-once execution semantics for financial endpoints,
 * prevents concurrent race conditions, detects payload tampering, and caches responses.
 */
@Service
@SuppressWarnings("null")
public class IdempotencyServiceImpl implements IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyServiceImpl.class);

    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ObjectMapper objectMapper;

    public IdempotencyServiceImpl(IdempotencyRecordRepository idempotencyRecordRepository, ObjectMapper objectMapper) {
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public IdempotencyValidationResult validateOrAcquireLock(String idempotencyKey, String requestPath, String requestHash, Duration ttl) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Idempotency-Key must not be null or empty");
        }

        String sanitizedKey = idempotencyKey.trim();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl != null ? ttl : Duration.ofHours(24));

        Optional<IdempotencyRecord> existingOpt = idempotencyRecordRepository.findByIdWithLock(sanitizedKey);

        if (existingOpt.isPresent()) {
            IdempotencyRecord record = existingOpt.get();

            // 1. Check if record has expired past TTL
            if (record.isExpired(now)) {
                log.info("Idempotency key '{}' expired at {}. Reclaiming lock for new execution.", sanitizedKey, record.getExpiresAt());
                record.setRequestPath(requestPath);
                record.setRequestHash(requestHash);
                record.setStatus(IdempotencyStatus.PROCESSING);
                record.setResponseCode(null);
                record.setResponseBody(null);
                record.setExpiresAt(expiresAt);
                idempotencyRecordRepository.save(record);
                return IdempotencyValidationResult.freshLock(record);
            }

            // 2. Check Payload Fingerprint Match
            if (!record.getRequestHash().equals(requestHash)) {
                log.warn("Idempotency key conflict: Key '{}' reused with altered payload hash (Expected: {}, Received: {})",
                        sanitizedKey, record.getRequestHash(), requestHash);
                throw new IdempotencyConflictException(
                        "Idempotency key '" + sanitizedKey + "' was previously used with a different request payload or parameters."
                );
            }

            // 3. Evaluate Status Machine
            if (record.getStatus() == IdempotencyStatus.PROCESSING) {
                log.warn("Concurrent duplicate request detected for in-flight key '{}'", sanitizedKey);
                throw new IdempotentRequestInProgressException(
                        "A request with Idempotency-Key '" + sanitizedKey + "' is currently being processed. Please wait for completion."
                );
            }

            if (record.getStatus() == IdempotencyStatus.COMPLETED) {
                log.info("Returning cached completed response for Idempotency-Key '{}'", sanitizedKey);
                return IdempotencyValidationResult.cached(record.getResponseCode(), record.getResponseBody(), record);
            }

            // Status is FAILED: allow immediate retry by resetting to PROCESSING
            if (record.getStatus() == IdempotencyStatus.FAILED) {
                log.info("Retrying previously failed request for Idempotency-Key '{}'", sanitizedKey);
                record.setStatus(IdempotencyStatus.PROCESSING);
                record.setResponseCode(null);
                record.setResponseBody(null);
                record.setExpiresAt(expiresAt);
                idempotencyRecordRepository.save(record);
                return IdempotencyValidationResult.freshLock(record);
            }
        }

        // 4. Record does not exist: create and reserve PROCESSING lock
        try {
            IdempotencyRecord newRecord = new IdempotencyRecord(sanitizedKey, requestPath, requestHash, expiresAt);
            idempotencyRecordRepository.save(newRecord);
            log.debug("Reserved new idempotency processing lock for key '{}'", sanitizedKey);
            return IdempotencyValidationResult.freshLock(newRecord);
        } catch (DataIntegrityViolationException ex) {
            // Race condition fallback if simultaneous insert happened before lock
            log.warn("Race condition on idempotency key insert '{}': {}", sanitizedKey, ex.getMessage());
            throw new IdempotentRequestInProgressException(
                    "A request with Idempotency-Key '" + sanitizedKey + "' is currently being processed."
            );
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(String idempotencyKey, int responseCode, Object responsePayload) {
        String sanitizedKey = idempotencyKey.trim();
        Optional<IdempotencyRecord> recordOpt = idempotencyRecordRepository.findById(sanitizedKey);
        if (recordOpt.isPresent()) {
            IdempotencyRecord record = recordOpt.get();
            record.setStatus(IdempotencyStatus.COMPLETED);
            record.setResponseCode(responseCode);
            try {
                if (responsePayload != null) {
                    record.setResponseBody(objectMapper.writeValueAsString(responsePayload));
                }
            } catch (Exception e) {
                log.error("Failed to serialize response payload for idempotency key '{}'", sanitizedKey, e);
            }
            idempotencyRecordRepository.save(record);
            log.info("Idempotency record '{}' marked COMPLETED with HTTP status {}", sanitizedKey, responseCode);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(String idempotencyKey, int responseCode, String errorMessage) {
        String sanitizedKey = idempotencyKey.trim();
        Optional<IdempotencyRecord> recordOpt = idempotencyRecordRepository.findById(sanitizedKey);
        if (recordOpt.isPresent()) {
            IdempotencyRecord record = recordOpt.get();
            record.setStatus(IdempotencyStatus.FAILED);
            record.setResponseCode(responseCode);
            record.setResponseBody(errorMessage);
            idempotencyRecordRepository.save(record);
            log.info("Idempotency record '{}' marked FAILED with HTTP status {}", sanitizedKey, responseCode);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(String idempotencyKey) {
        String sanitizedKey = idempotencyKey.trim();
        idempotencyRecordRepository.deleteById(sanitizedKey);
        log.info("Released and cleared idempotency processing lock for key '{}'", sanitizedKey);
    }

    @Override
    @Scheduled(fixedRateString = "${app.idempotency.cleanup-rate-ms:3600000}")
    @Transactional
    public int purgeExpiredRecords() {
        Instant now = Instant.now();
        int deletedCount = idempotencyRecordRepository.deleteExpiredRecords(now);
        if (deletedCount > 0) {
            log.info("Purged {} expired idempotency records from database", deletedCount);
        }
        return deletedCount;
    }
}
