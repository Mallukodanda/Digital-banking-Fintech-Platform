package com.fintech.bank.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.bank.dto.TransferReceiptResponse;
import com.fintech.bank.dto.TransferRequest;
import com.fintech.bank.entity.*;
import com.fintech.bank.exception.InsufficientFundsException;
import com.fintech.bank.repository.AccountRepository;
import com.fintech.bank.repository.JournalEntryRepository;
import com.fintech.bank.repository.OutboxEventRepository;
import com.fintech.bank.repository.TransactionLegRepository;
import com.fintech.bank.service.impl.TransferServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Concurrency Test Suite simulating simultaneous competing transfer requests.
 *
 * Scenario:
 * - Starting Source Account Balance: ₹10,000
 * - Request A: Transfer ₹8,000
 * - Request B: Transfer ₹8,000
 * Both dispatched simultaneously across separate OS threads.
 *
 * Proves that deterministic pessimistic row-level locking eliminates race conditions,
 * prevents lost updates, eliminates negative balances, and guarantees strict ACID consistency.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Concurrent High-Volume Financial Transfer Test")
@SuppressWarnings("null")
class ConcurrentTransferTest {

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
    private IdempotencyService idempotencyService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
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

    // Simulates database row-level locking (SELECT ... FOR UPDATE) across threads
    private final ReentrantLock dbRowLock = new ReentrantLock();

    @BeforeEach
    void setUp() {
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

        when(idempotencyService.validateOrAcquireLock(any(), any(), any(), any()))
                .thenAnswer(inv -> com.fintech.bank.dto.IdempotencyValidationResult.freshLock(
                        new IdempotencyRecord(inv.getArgument(0), "/api/v1/transfers", "hash", java.time.Instant.now().plusSeconds(3600))
                ));

        // When locking source account, simulate database mutual exclusion
        when(accountRepository.findByIdWithLock(sourceAccountId)).thenAnswer(inv -> {
            dbRowLock.lock();
            return Optional.of(sourceAccount);
        });

        when(accountRepository.findByIdWithLock(destAccountId)).thenReturn(Optional.of(destAccount));

        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(inv -> {
            JournalEntry j = inv.getArgument(0);
            j.setId(UUID.randomUUID());
            return j;
        });

        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
            Account acc = inv.getArgument(0);
            // Once account mutation is committed, unlock row lock
            if (dbRowLock.isHeldByCurrentThread()) {
                dbRowLock.unlock();
            }
            return acc;
        });
    }

    @Test
    @DisplayName("Simultaneous ₹8,000 withdrawals on ₹10,000 balance must result in exactly 1 success and 1 InsufficientFunds failure")
    void testConcurrentDualWithdrawalRaceCondition() throws InterruptedException {
        int numberOfThreads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger insufficientFundsCount = new AtomicInteger(0);
        AtomicInteger unexpectedErrorCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadIdx = i;
            executorService.submit(() -> {
                readyLatch.countDown();
                try {
                    startGun.await(); // Both threads wait here and start simultaneously

                    TransferRequest request = new TransferRequest(
                            sourceAccountId,
                            destAccNum,
                            new BigDecimal("8000.00"),
                            "INR",
                            "Concurrent Test Transfer #" + threadIdx
                    );
                    String idempotencyKey = "idemp-concurrent-" + threadIdx + "-" + UUID.randomUUID();

                    TransferReceiptResponse receipt = transferService.transferFunds(
                            request,
                            idempotencyKey,
                            sourceUser.getId(),
                            false
                    );

                    if (receipt != null && receipt.getStatus() == TransactionStatus.COMPLETED) {
                        successCount.incrementAndGet();
                    }
                } catch (InsufficientFundsException e) {
                    insufficientFundsCount.incrementAndGet();
                } catch (Exception e) {
                    unexpectedErrorCount.incrementAndGet();
                } finally {
                    if (dbRowLock.isHeldByCurrentThread()) {
                        dbRowLock.unlock();
                    }
                    finishLatch.countDown();
                }
            });
        }

        // Wait until both threads are initialized and waiting at the starting gate
        readyLatch.await(5, TimeUnit.SECONDS);

        // Fire starting gun
        startGun.countDown();

        // Wait for both threads to finish
        boolean completed = finishLatch.await(5, TimeUnit.SECONDS);
        executorService.shutdown();

        assertThat(completed).isTrue();
        assertThat(unexpectedErrorCount.get()).isEqualTo(0);

        // Crucial Financial Assertions:
        // Exactly ONE transaction must succeed, and the second must be rejected with InsufficientFundsException
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(insufficientFundsCount.get()).isEqualTo(1);

        // Final state verification
        assertThat(sourceAccount.getCachedBalance()).isEqualByComparingTo(new BigDecimal("2000.0000"));
        assertThat(destAccount.getCachedBalance()).isEqualByComparingTo(new BigDecimal("8000.0000"));
    }
}
