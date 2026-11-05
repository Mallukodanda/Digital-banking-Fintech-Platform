package com.fintech.bank.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.bank.dto.IdempotencyValidationResult;
import com.fintech.bank.entity.IdempotencyRecord;
import com.fintech.bank.entity.IdempotencyStatus;
import com.fintech.bank.exception.IdempotencyConflictException;
import com.fintech.bank.exception.IdempotentRequestInProgressException;
import com.fintech.bank.repository.IdempotencyRecordRepository;
import com.fintech.bank.service.impl.IdempotencyServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IdempotencyService Unit Tests")
@SuppressWarnings("null")
class IdempotencyServiceTest {

    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private IdempotencyServiceImpl idempotencyService;

    private final String testKey = "idemp-test-uuid-1234";
    private final String testPath = "/api/v1/transfers";
    private final String testHash = "a1b2c3d4e5f67890abcdef1234567890abcdef1234567890abcdef1234567890";
    private final Duration testTtl = Duration.ofHours(24);

    @Nested
    @DisplayName("Lock Acquisition & State Evaluation")
    class LockAcquisitionTests {

        @Test
        @DisplayName("Should acquire new PROCESSING lock when key does not exist")
        void shouldAcquireNewLockWhenKeyNotExists() {
            when(idempotencyRecordRepository.findByIdWithLock(testKey)).thenReturn(Optional.empty());
            when(idempotencyRecordRepository.save(any(IdempotencyRecord.class))).thenAnswer(i -> i.getArgument(0));

            IdempotencyValidationResult result = idempotencyService.validateOrAcquireLock(testKey, testPath, testHash, testTtl);

            assertThat(result.alreadyCompleted()).isFalse();
            assertThat(result.record()).isNotNull();
            assertThat(result.record().getStatus()).isEqualTo(IdempotencyStatus.PROCESSING);
            verify(idempotencyRecordRepository, times(1)).save(any(IdempotencyRecord.class));
        }

        @Test
        @DisplayName("Should return cached COMPLETED response when matching key and payload exist")
        void shouldReturnCachedResponseWhenCompleted() {
            IdempotencyRecord completedRecord = new IdempotencyRecord(testKey, testPath, testHash, Instant.now().plus(testTtl));
            completedRecord.setStatus(IdempotencyStatus.COMPLETED);
            completedRecord.setResponseCode(201);
            completedRecord.setResponseBody("{\"status\":\"COMPLETED\"}");

            when(idempotencyRecordRepository.findByIdWithLock(testKey)).thenReturn(Optional.of(completedRecord));

            IdempotencyValidationResult result = idempotencyService.validateOrAcquireLock(testKey, testPath, testHash, testTtl);

            assertThat(result.alreadyCompleted()).isTrue();
            assertThat(result.responseCode()).isEqualTo(201);
            assertThat(result.responseBody()).isEqualTo("{\"status\":\"COMPLETED\"}");
            verify(idempotencyRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw IdempotencyConflictException when key exists with different payload hash")
        void shouldThrowConflictWhenPayloadHashDiffers() {
            String differentHash = "0000000000000000000000000000000000000000000000000000000000000000";
            IdempotencyRecord existingRecord = new IdempotencyRecord(testKey, testPath, testHash, Instant.now().plus(testTtl));
            existingRecord.setStatus(IdempotencyStatus.COMPLETED);

            when(idempotencyRecordRepository.findByIdWithLock(testKey)).thenReturn(Optional.of(existingRecord));

            assertThatThrownBy(() -> idempotencyService.validateOrAcquireLock(testKey, testPath, differentHash, testTtl))
                    .isInstanceOf(IdempotencyConflictException.class)
                    .hasMessageContaining("different request payload");
        }

        @Test
        @DisplayName("Should throw IdempotentRequestInProgressException when concurrent request is in PROCESSING state")
        void shouldThrowInProgressWhenConcurrentProcessing() {
            IdempotencyRecord inProgressRecord = new IdempotencyRecord(testKey, testPath, testHash, Instant.now().plus(testTtl));
            inProgressRecord.setStatus(IdempotencyStatus.PROCESSING);

            when(idempotencyRecordRepository.findByIdWithLock(testKey)).thenReturn(Optional.of(inProgressRecord));

            assertThatThrownBy(() -> idempotencyService.validateOrAcquireLock(testKey, testPath, testHash, testTtl))
                    .isInstanceOf(IdempotentRequestInProgressException.class)
                    .hasMessageContaining("currently being processed");
        }

        @Test
        @DisplayName("Should allow retry and reset lock when existing record is in FAILED state")
        void shouldAllowRetryWhenPreviousFailed() {
            IdempotencyRecord failedRecord = new IdempotencyRecord(testKey, testPath, testHash, Instant.now().plus(testTtl));
            failedRecord.setStatus(IdempotencyStatus.FAILED);
            failedRecord.setResponseCode(500);

            when(idempotencyRecordRepository.findByIdWithLock(testKey)).thenReturn(Optional.of(failedRecord));
            when(idempotencyRecordRepository.save(any(IdempotencyRecord.class))).thenAnswer(i -> i.getArgument(0));

            IdempotencyValidationResult result = idempotencyService.validateOrAcquireLock(testKey, testPath, testHash, testTtl);

            assertThat(result.alreadyCompleted()).isFalse();
            assertThat(failedRecord.getStatus()).isEqualTo(IdempotencyStatus.PROCESSING);
            verify(idempotencyRecordRepository, times(1)).save(failedRecord);
        }

        @Test
        @DisplayName("Should reclaim and overwrite lock when existing record is past its TTL expiration")
        void shouldReclaimLockWhenExpired() {
            IdempotencyRecord expiredRecord = new IdempotencyRecord(testKey, testPath, testHash, Instant.now().minusSeconds(3600));
            expiredRecord.setStatus(IdempotencyStatus.COMPLETED);

            when(idempotencyRecordRepository.findByIdWithLock(testKey)).thenReturn(Optional.of(expiredRecord));
            when(idempotencyRecordRepository.save(any(IdempotencyRecord.class))).thenAnswer(i -> i.getArgument(0));

            IdempotencyValidationResult result = idempotencyService.validateOrAcquireLock(testKey, testPath, testHash, testTtl);

            assertThat(result.alreadyCompleted()).isFalse();
            assertThat(expiredRecord.getStatus()).isEqualTo(IdempotencyStatus.PROCESSING);
            verify(idempotencyRecordRepository, times(1)).save(expiredRecord);
        }
    }

    @Nested
    @DisplayName("Lifecycle & Completion Management")
    class LifecycleTests {

        @Test
        @DisplayName("Should mark record as COMPLETED and persist response payload")
        void shouldMarkCompleted() throws Exception {
            IdempotencyRecord record = new IdempotencyRecord(testKey, testPath, testHash, Instant.now().plus(testTtl));
            when(idempotencyRecordRepository.findById(testKey)).thenReturn(Optional.of(record));
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"receiptId\":\"123\"}");

            idempotencyService.complete(testKey, 201, "payloadObj");

            assertThat(record.getStatus()).isEqualTo(IdempotencyStatus.COMPLETED);
            assertThat(record.getResponseCode()).isEqualTo(201);
            assertThat(record.getResponseBody()).isEqualTo("{\"receiptId\":\"123\"}");
            verify(idempotencyRecordRepository, times(1)).save(record);
        }

        @Test
        @DisplayName("Should release lock by deleting record")
        void shouldReleaseLock() {
            idempotencyService.release(testKey);
            verify(idempotencyRecordRepository, times(1)).deleteById(testKey);
        }

        @Test
        @DisplayName("Should purge expired records via scheduled method")
        void shouldPurgeExpiredRecords() {
            when(idempotencyRecordRepository.deleteExpiredRecords(any(Instant.class))).thenReturn(5);

            int purged = idempotencyService.purgeExpiredRecords();

            assertThat(purged).isEqualTo(5);
            verify(idempotencyRecordRepository, times(1)).deleteExpiredRecords(any(Instant.class));
        }
    }
}
