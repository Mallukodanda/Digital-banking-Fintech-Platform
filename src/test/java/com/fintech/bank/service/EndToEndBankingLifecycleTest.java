package com.fintech.bank.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.bank.dto.*;
import com.fintech.bank.entity.*;
import com.fintech.bank.repository.*;
import com.fintech.bank.service.fraud.FraudRuleEngine;
import com.fintech.bank.service.impl.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Layer 10: End-to-End Core Banking Lifecycle Test")
@SuppressWarnings("null")
class EndToEndBankingLifecycleTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private JournalEntryRepository journalEntryRepository;

    @Mock
    private TransactionLegRepository transactionLegRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Mock
    private FraudAlertRepository fraudAlertRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private FraudRuleEngine fraudRuleEngine;

    private ObjectMapper objectMapper;
    private IdempotencyServiceImpl idempotencyService;
    private TransferServiceImpl transferService;
    private AuditServiceImpl auditService;
    private NotificationServiceImpl notificationService;
    private FraudDetectionServiceImpl fraudDetectionService;

    private UserAuth customerUser;
    private CustomerProfile customerProfile;
    private Account sourceAccount;
    private Account destinationAccount;
    private final UUID sourceAccountId = UUID.randomUUID();
    private final UUID destAccountId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        auditService = new AuditServiceImpl(auditLogRepository);
        notificationService = new NotificationServiceImpl(notificationRepository, mock(UserAuthRepository.class));
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
        fraudDetectionService = new FraudDetectionServiceImpl(
                fraudAlertRepository,
                accountRepository,
                journalEntryRepository,
                fraudRuleEngine,
                notificationService,
                auditService
        );

        customerUser = new UserAuth("e2e_user@bank.com", "hash", UserRole.ROLE_CUSTOMER);
        customerUser.setId(UUID.randomUUID());
        customerProfile = new CustomerProfile(customerUser, "E2E", "Customer", "+14155559090");

        sourceAccount = new Account(customerProfile, "100000000001", AccountType.CHECKING, "INR");
        sourceAccount.setId(sourceAccountId);
        sourceAccount.setCachedBalance(new BigDecimal("100000.0000"));

        destinationAccount = new Account(customerProfile, "100000000002", AccountType.SAVINGS, "INR");
        destinationAccount.setId(destAccountId);
        destinationAccount.setCachedBalance(new BigDecimal("10000.0000"));

        when(accountRepository.findById(sourceAccountId)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumber("100000000002")).thenReturn(Optional.of(destinationAccount));
        when(accountRepository.findByIdWithLock(sourceAccountId)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByIdWithLock(destAccountId)).thenReturn(Optional.of(destinationAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));

        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(i -> {
            JournalEntry j = i.getArgument(0);
            j.setId(UUID.randomUUID());
            return j;
        });

        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(i -> i.getArgument(0));
        when(idempotencyRecordRepository.save(any(IdempotencyRecord.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("Complete E2E Cycle: Transfer Initiation -> Double-Entry Settlement -> Outbox Enqueue -> Fraud Rule Evaluation")
    void testCompleteBankingLifecycle() {
        String idempotencyKey = UUID.randomUUID().toString();
        BigDecimal transferAmount = new BigDecimal("85000.00");
        TransferRequest request = new TransferRequest(sourceAccountId, "100000000002", transferAmount, "INR", "Large E2E Transfer");

        when(idempotencyRecordRepository.findByIdWithLock(idempotencyKey)).thenReturn(Optional.empty());

        // 1. Execute Core Transfer
        TransferReceiptResponse receipt = transferService.transferFunds(request, idempotencyKey, customerUser.getId(), false);

        // Assert Step 1: Balances & Receipt
        assertThat(receipt).isNotNull();
        assertThat(receipt.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(sourceAccount.getCachedBalance()).isEqualByComparingTo(new BigDecimal("15000.0000"));
        assertThat(destinationAccount.getCachedBalance()).isEqualByComparingTo(new BigDecimal("95000.0000"));

        // Assert Step 2: Double-Entry Conservation of Money
        assertThat(sourceAccount.getCachedBalance().add(destinationAccount.getCachedBalance()))
                .isEqualByComparingTo(new BigDecimal("110000.0000"));

        // 2. Outbox Event Verification
        verify(outboxEventRepository, times(1)).save(any(OutboxEvent.class));

        // 3. Simulate Kafka Consumer Ingestion into Fraud Engine
        TransactionCompletedEventPayload eventPayload = new TransactionCompletedEventPayload(
                UUID.randomUUID(),
                receipt.getTransactionReference(),
                idempotencyKey,
                TransactionType.P2P_TRANSFER,
                sourceAccountId,
                "100000000001",
                destAccountId,
                "100000000002",
                transferAmount,
                "INR",
                TransactionStatus.COMPLETED,
                "Large E2E Transfer",
                Instant.now()
        );

        when(fraudRuleEngine.evaluate(any())).thenReturn(
                FraudEvaluationResult.flagged(80, java.util.List.of("LARGE_TRANSACTION_AMOUNT"), "Exceeds ₹50,000 threshold")
        );
        when(accountRepository.findByAccountNumber("100000000001")).thenReturn(Optional.of(sourceAccount));

        FraudEvaluationResult fraudResult = fraudDetectionService.processTransactionEvent(eventPayload);

        // Assert Step 4: Fraud Engine flagged the transfer and created an alert
        assertThat(fraudResult.alertTriggered()).isTrue();
        assertThat(fraudResult.riskScore()).isEqualTo(80);
        verify(fraudAlertRepository, times(1)).save(any(FraudAlert.class));
    }
}
