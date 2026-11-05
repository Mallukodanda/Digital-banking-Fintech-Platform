package com.fintech.bank.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.bank.dto.TransferReceiptResponse;
import com.fintech.bank.dto.TransferRequest;
import com.fintech.bank.entity.*;
import com.fintech.bank.exception.IdempotencyConflictException;
import com.fintech.bank.exception.IdempotentRequestInProgressException;
import com.fintech.bank.repository.*;
import com.fintech.bank.service.impl.IdempotencyServiceImpl;
import com.fintech.bank.service.impl.TransferServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * End-to-End Idempotency & Distributed Concurrency Test Suite.
 *
 * Covers all failure modes:
 * 1. Same request executed twice (Idempotent replay, 0 double debits)
 * 2. Same key with different request payload (Payload conflict detection)
 * 3. Concurrent duplicate requests (Lock contention & 409 In-Progress)
 * 4. Retry after transient network timeout
 * 5. Already completed request cache reuse
 * 6. Failed request retry recovery
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Transfer Idempotency & Concurrency Engineering Tests")
@SuppressWarnings("null")
class TransferIdempotencyTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private JournalEntryRepository journalEntryRepository;

    @Mock
    private TransactionLegRepository transactionLegRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

    private ObjectMapper objectMapper;
    private IdempotencyServiceImpl idempotencyService;
    private TransferServiceImpl transferService;

    private UserAuth sourceUser;
    private CustomerProfile sourceProfile;
    private Account sourceAccount;

    private UserAuth destUser;
    private CustomerProfile destProfile;
    private Account destAccount;

    private final UUID sourceAccountId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID destAccountId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private final String sourceAccNum = "100000000001";
    private final String destAccNum = "100000000002";
    private final String idempotencyKey = "idemp-txn-fixed-uuid-9999";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        idempotencyService = new IdempotencyServiceImpl(idempotencyRecordRepository, objectMapper);
        transferService = new TransferServiceImpl(
                accountRepository,
                journalEntryRepository,
                transactionLegRepository,
                outboxEventRepository,
                auditService,
                idempotencyService,
                objectMapper
        );

        sourceUser = new UserAuth("alice@bank.com", "hash", UserRole.ROLE_CUSTOMER);
        sourceUser.setId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        sourceProfile = new CustomerProfile(sourceUser, "Alice", "Smith", "+10000000001");
        sourceAccount = new Account(sourceProfile, sourceAccNum, AccountType.CHECKING, "INR");
        sourceAccount.setId(sourceAccountId);
        sourceAccount.setCachedBalance(new BigDecimal("10000.0000"));

        destUser = new UserAuth("bob@bank.com", "hash", UserRole.ROLE_CUSTOMER);
        destUser.setId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
        destProfile = new CustomerProfile(destUser, "Bob", "Jones", "+10000000002");
        destAccount = new Account(destProfile, destAccNum, AccountType.CHECKING, "INR");
        destAccount.setId(destAccountId);
        destAccount.setCachedBalance(new BigDecimal("0.0000"));

        when(accountRepository.findById(sourceAccountId)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumber(destAccNum)).thenReturn(Optional.of(destAccount));
        when(accountRepository.findByIdWithLock(sourceAccountId)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByIdWithLock(destAccountId)).thenReturn(Optional.of(destAccount));

        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(inv -> {
            JournalEntry j = inv.getArgument(0);
            j.setId(UUID.randomUUID());
            return j;
        });

        when(idempotencyRecordRepository.save(any(IdempotencyRecord.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Nested
    @DisplayName("Idempotency Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Scenario 1: Same request submitted twice returns identical receipt and debits balance exactly once")
        void testSameRequestTwiceDeductsBalanceOnce() throws Exception {
            TransferRequest request = new TransferRequest(sourceAccountId, destAccNum, new BigDecimal("4000.00"), "INR", "Invoice 101");

            // First Attempt: No existing record
            when(idempotencyRecordRepository.findByIdWithLock(idempotencyKey)).thenReturn(Optional.empty());

            TransferReceiptResponse firstReceipt = transferService.transferFunds(request, idempotencyKey, sourceUser.getId(), false);

            assertThat(firstReceipt).isNotNull();
            assertThat(firstReceipt.getAmount()).isEqualByComparingTo(new BigDecimal("4000.00"));
            assertThat(sourceAccount.getCachedBalance()).isEqualByComparingTo(new BigDecimal("6000.0000"));
            assertThat(destAccount.getCachedBalance()).isEqualByComparingTo(new BigDecimal("4000.0000"));

            // Second Attempt: Simulating client retry on timeout -> Idempotency record is COMPLETED
            IdempotencyRecord completedRecord = new IdempotencyRecord(
                    idempotencyKey,
                    "/api/v1/transfers",
                    com.fintech.bank.util.IdempotencyUtils.computeRequestFingerprint("POST", "/api/v1/transfers", sourceUser.getId(), request, objectMapper),
                    Instant.now().plus(Duration.ofHours(24))
            );
            completedRecord.setStatus(IdempotencyStatus.COMPLETED);
            completedRecord.setResponseCode(201);
            completedRecord.setResponseBody(objectMapper.writeValueAsString(firstReceipt));

            when(idempotencyRecordRepository.findByIdWithLock(idempotencyKey)).thenReturn(Optional.of(completedRecord));

            TransferReceiptResponse secondReceipt = transferService.transferFunds(request, idempotencyKey, sourceUser.getId(), false);

            // Assert: Returned receipt is identical
            assertThat(secondReceipt.getTransactionReference()).isEqualTo(firstReceipt.getTransactionReference());
            assertThat(secondReceipt.getAmount()).isEqualByComparingTo(new BigDecimal("4000.00"));

            // Critical Financial Invariant: Balances MUST NOT be debited a second time
            assertThat(sourceAccount.getCachedBalance()).isEqualByComparingTo(new BigDecimal("6000.0000"));
            assertThat(destAccount.getCachedBalance()).isEqualByComparingTo(new BigDecimal("4000.0000"));

            // Account save was only called twice (from the 1st request), never for the 2nd
            verify(accountRepository, times(2)).save(any(Account.class));
            verify(journalEntryRepository, times(1)).save(any(JournalEntry.class));
        }

        @Test
        @DisplayName("Scenario 2: Reusing existing Idempotency-Key with different amount/payload throws IdempotencyConflictException (422)")
        void testSameKeyWithDifferentPayloadThrowsConflict() {
            TransferRequest originalRequest = new TransferRequest(sourceAccountId, destAccNum, new BigDecimal("4000.00"), "INR", "Original");
            String originalHash = com.fintech.bank.util.IdempotencyUtils.computeRequestFingerprint("POST", "/api/v1/transfers", sourceUser.getId(), originalRequest, objectMapper);

            IdempotencyRecord existingRecord = new IdempotencyRecord(idempotencyKey, "/api/v1/transfers", originalHash, Instant.now().plus(Duration.ofHours(24)));
            existingRecord.setStatus(IdempotencyStatus.COMPLETED);

            when(idempotencyRecordRepository.findByIdWithLock(idempotencyKey)).thenReturn(Optional.of(existingRecord));

            // Malicious / Accidental client submits ₹9,000 with the SAME key
            TransferRequest alteredRequest = new TransferRequest(sourceAccountId, destAccNum, new BigDecimal("9000.00"), "INR", "Altered Amount");

            assertThatThrownBy(() -> transferService.transferFunds(alteredRequest, idempotencyKey, sourceUser.getId(), false))
                    .isInstanceOf(IdempotencyConflictException.class)
                    .hasMessageContaining("different request payload");

            // No ledger or account writes
            assertThat(sourceAccount.getCachedBalance()).isEqualByComparingTo(new BigDecimal("10000.0000"));
            verify(accountRepository, never()).save(any(Account.class));
        }

        @Test
        @DisplayName("Scenario 3: Concurrent duplicate requests with same key throw IdempotentRequestInProgressException (409)")
        void testConcurrentDuplicateRequestThrowsInProgress() {
            TransferRequest request = new TransferRequest(sourceAccountId, destAccNum, new BigDecimal("2000.00"), "INR", "Concurrent");
            String hash = com.fintech.bank.util.IdempotencyUtils.computeRequestFingerprint("POST", "/api/v1/transfers", sourceUser.getId(), request, objectMapper);

            // Record is currently PROCESSING by another thread
            IdempotencyRecord inProgressRecord = new IdempotencyRecord(idempotencyKey, "/api/v1/transfers", hash, Instant.now().plus(Duration.ofHours(24)));
            inProgressRecord.setStatus(IdempotencyStatus.PROCESSING);

            when(idempotencyRecordRepository.findByIdWithLock(idempotencyKey)).thenReturn(Optional.of(inProgressRecord));

            assertThatThrownBy(() -> transferService.transferFunds(request, idempotencyKey, sourceUser.getId(), false))
                    .isInstanceOf(IdempotentRequestInProgressException.class)
                    .hasMessageContaining("currently being processed");

            assertThat(sourceAccount.getCachedBalance()).isEqualByComparingTo(new BigDecimal("10000.0000"));
            verify(accountRepository, never()).save(any(Account.class));
        }

        @Test
        @DisplayName("Scenario 4: Failed request allows subsequent retry without locking client out")
        void testFailedRequestRetryRecovers() {
            TransferRequest request = new TransferRequest(sourceAccountId, destAccNum, new BigDecimal("3000.00"), "INR", "Retry after fail");
            String hash = com.fintech.bank.util.IdempotencyUtils.computeRequestFingerprint("POST", "/api/v1/transfers", sourceUser.getId(), request, objectMapper);

            // First attempt previously failed
            IdempotencyRecord failedRecord = new IdempotencyRecord(idempotencyKey, "/api/v1/transfers", hash, Instant.now().plus(Duration.ofHours(24)));
            failedRecord.setStatus(IdempotencyStatus.FAILED);
            failedRecord.setResponseCode(500);

            when(idempotencyRecordRepository.findByIdWithLock(idempotencyKey)).thenReturn(Optional.of(failedRecord));
            when(idempotencyRecordRepository.findById(idempotencyKey)).thenReturn(Optional.of(failedRecord));

            // Retry should succeed
            TransferReceiptResponse receipt = transferService.transferFunds(request, idempotencyKey, sourceUser.getId(), false);

            assertThat(receipt).isNotNull();
            assertThat(receipt.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
            assertThat(sourceAccount.getCachedBalance()).isEqualByComparingTo(new BigDecimal("7000.0000"));
            assertThat(failedRecord.getStatus()).isEqualTo(IdempotencyStatus.COMPLETED);
        }
    }
}
