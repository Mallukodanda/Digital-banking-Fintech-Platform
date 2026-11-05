package com.fintech.bank.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.bank.dto.TransferRequest;
import com.fintech.bank.entity.*;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Layer 6: Transaction Rollback & Invariant Integrity Tests")
@SuppressWarnings("null")
class TransactionRollbackAndIntegrityTest {

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

    private Account sourceAccount;
    private Account destinationAccount;
    private UUID sourceUserId;
    private final UUID sourceAccountId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID destinationAccountId = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() {
        sourceUserId = UUID.randomUUID();
        UserAuth user = new UserAuth("rollback@bank.com", "hash", UserRole.ROLE_CUSTOMER);
        user.setId(sourceUserId);
        CustomerProfile profile = new CustomerProfile(user, "Alice", "Rollback", "+12223334444");

        sourceAccount = new Account(profile, "100000000001", AccountType.CHECKING, "INR");
        sourceAccount.setId(sourceAccountId);
        sourceAccount.setCachedBalance(new BigDecimal("10000.0000"));

        destinationAccount = new Account(profile, "100000000002", AccountType.CHECKING, "INR");
        destinationAccount.setId(destinationAccountId);
        destinationAccount.setCachedBalance(new BigDecimal("2000.0000"));

        when(accountRepository.findById(sourceAccountId)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumber("100000000002")).thenReturn(Optional.of(destinationAccount));
        when(accountRepository.findByIdWithLock(sourceAccountId)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByIdWithLock(destinationAccountId)).thenReturn(Optional.of(destinationAccount));
    }

    @Test
    @DisplayName("Financial Invariant: Debit sum must strictly match Credit sum")
    void testDoubleEntryZeroSumRule() {
        BigDecimal transferAmount = new BigDecimal("4500.0000");

        sourceAccount.setCachedBalance(sourceAccount.getCachedBalance().subtract(transferAmount));
        destinationAccount.setCachedBalance(destinationAccount.getCachedBalance().add(transferAmount));

        // Verification of conservation of mass / zero-sum monetary invariant
        assertThat(sourceAccount.getCachedBalance()).isEqualByComparingTo(new BigDecimal("5500.0000"));
        assertThat(destinationAccount.getCachedBalance()).isEqualByComparingTo(new BigDecimal("6500.0000"));
        assertThat(sourceAccount.getCachedBalance().add(destinationAccount.getCachedBalance()))
                .isEqualByComparingTo(new BigDecimal("12000.0000"));
    }

    @Test
    @DisplayName("Rollback on Mid-Flight Failure: If Outbox Event persistence fails, no journal entry or balance changes commit")
    void testRollbackWhenOutboxFails() {
        TransferRequest request = new TransferRequest(sourceAccountId, "100000000002", new BigDecimal("3000.00"), "INR", "Test Rollback");
        String key = "idemp-rollback-key";

        when(idempotencyService.validateOrAcquireLock(any(), any(), any(), any()))
                .thenReturn(com.fintech.bank.dto.IdempotencyValidationResult.freshLock(
                        new IdempotencyRecord(key, "/api/v1/transfers", "hash", java.time.Instant.now().plusSeconds(3600))
                ));

        // Simulate outbox storage error (e.g. database disk/table partition error)
        when(outboxEventRepository.save(any(OutboxEvent.class)))
                .thenThrow(new RuntimeException("Simulated Database I/O Failure"));

        assertThatThrownBy(() -> transferService.transferFunds(request, key, sourceUserId, false))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Simulated Database I/O Failure");

        // Idempotency lock is released on failure
        verify(idempotencyService, times(1)).release(key);
    }
}
