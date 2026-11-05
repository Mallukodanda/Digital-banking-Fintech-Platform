package com.fintech.bank.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.bank.dto.*;
import com.fintech.bank.entity.*;
import com.fintech.bank.repository.*;
import com.fintech.bank.service.*;
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
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Phase 26: Enterprise End-to-End System Integration Test")
@SuppressWarnings("null")
class EnterpriseSystemIntegrationTest {

    @Mock private UserAuthRepository userAuthRepository;
    @Mock private CustomerProfileRepository customerProfileRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private BeneficiaryRepository beneficiaryRepository;
    @Mock private JournalEntryRepository journalEntryRepository;
    @Mock private TransactionLegRepository transactionLegRepository;
    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private IdempotencyRecordRepository idempotencyRecordRepository;
    @Mock private FraudAlertRepository fraudAlertRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private FraudRuleEngine fraudRuleEngine;
    @Mock private EventPublisher eventPublisher;

    private ObjectMapper objectMapper;
    private AccountServiceImpl accountService;
    private BeneficiaryServiceImpl beneficiaryService;
    private TransferServiceImpl transferService;
    private NotificationServiceImpl notificationService;
    private FraudDetectionServiceImpl fraudDetectionService;
    private AuditServiceImpl auditService;
    private IdempotencyServiceImpl idempotencyService;

    // Test Data State
    private UUID customerId;
    private UUID adminId;
    private UUID sourceAccId;
    private UUID destAccId;
    private String sourceAccNum = "1000000001";
    private String destAccNum = "1000000002";

    private UserAuth customerUser;
    private CustomerProfile customerProfile;
    private Account sourceAccount;
    private Account destAccount;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        customerId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        sourceAccId = UUID.randomUUID();
        destAccId = UUID.randomUUID();

        customerUser = new UserAuth("alice@bank.com", "encodedHash", UserRole.ROLE_CUSTOMER);
        customerUser.setId(customerId);
        customerProfile = new CustomerProfile(customerUser, "Alice", "Smith", "+1234567890");

        sourceAccount = new Account(customerProfile, sourceAccNum, AccountType.CHECKING, "INR");
        sourceAccount.setId(sourceAccId);
        sourceAccount.setCachedBalance(new BigDecimal("50000.0000"));

        UserAuth destUser = new UserAuth("bob@bank.com", "encodedHash", UserRole.ROLE_CUSTOMER);
        destUser.setId(UUID.randomUUID());
        CustomerProfile destProfile = new CustomerProfile(destUser, "Bob", "Jones", "+1987654321");

        destAccount = new Account(destProfile, destAccNum, AccountType.SAVINGS, "INR");
        destAccount.setId(destAccId);
        destAccount.setCachedBalance(new BigDecimal("10000.0000"));

        // Instantiate Domain Services with exact constructors
        auditService = new AuditServiceImpl(auditLogRepository);
        idempotencyService = new IdempotencyServiceImpl(idempotencyRecordRepository, objectMapper);
        notificationService = new NotificationServiceImpl(notificationRepository, userAuthRepository);
        fraudDetectionService = new FraudDetectionServiceImpl(fraudAlertRepository, accountRepository, journalEntryRepository, fraudRuleEngine, notificationService, auditService);
        transferService = new TransferServiceImpl(accountRepository, journalEntryRepository, transactionLegRepository, outboxEventRepository, auditService, idempotencyService, objectMapper);
        beneficiaryService = new BeneficiaryServiceImpl(beneficiaryRepository, customerProfileRepository, accountRepository);
        accountService = new AccountServiceImpl(accountRepository, customerProfileRepository, auditService);
    }

    @Test
    @DisplayName("Complete Flow 1: Customer Banking Lifecycle (Account -> Beneficiary -> Transfer -> Outbox -> Notification -> Fraud -> Audit)")
    void testCompleteCustomerBankingLifecycle() throws Exception {
        // Step 1: Account Balance Inspection
        when(accountRepository.findById(sourceAccId)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumber(destAccNum)).thenReturn(Optional.of(destAccount));
        when(accountRepository.findByIdWithLock(sourceAccId)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByIdWithLock(destAccId)).thenReturn(Optional.of(destAccount));

        // Step 2: Add Beneficiary
        when(customerProfileRepository.findByUserAuthId(customerId)).thenReturn(Optional.of(customerProfile));
        CreateBeneficiaryRequest benRequest = new CreateBeneficiaryRequest("Bob Jones", destAccNum, "Bob Personal");
        Beneficiary beneficiary = new Beneficiary(customerProfile, "Bob Jones", destAccNum, "Bob Personal");
        when(beneficiaryRepository.save(any(Beneficiary.class))).thenReturn(beneficiary);

        BeneficiaryResponse benResponse = beneficiaryService.addBeneficiary(customerId, benRequest);
        assertThat(benResponse).isNotNull();
        assertThat(benResponse.destinationAccountNumber()).isEqualTo(destAccNum);

        // Step 3: Initiate Double-Entry Funds Transfer
        String idempotencyKey = "idemp-flow1-" + UUID.randomUUID();
        TransferRequest transferRequest = new TransferRequest(sourceAccId, destAccNum, new BigDecimal("25000.00"), "INR", "Consulting Fee");

        when(idempotencyRecordRepository.findByIdWithLock(idempotencyKey)).thenReturn(Optional.empty());
        when(idempotencyRecordRepository.save(any(IdempotencyRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(inv -> {
            JournalEntry j = inv.getArgument(0);
            j.setId(UUID.randomUUID());
            return j;
        });

        TransferReceiptResponse receipt = transferService.transferFunds(transferRequest, idempotencyKey, customerId, false);

        // Assert Step 3: Ledger Invariance & Balance Integrity
        assertThat(receipt).isNotNull();
        assertThat(receipt.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(receipt.getAmount()).isEqualByComparingTo(new BigDecimal("25000.00"));
        assertThat(sourceAccount.getCachedBalance()).isEqualByComparingTo(new BigDecimal("25000.0000"));
        assertThat(destAccount.getCachedBalance()).isEqualByComparingTo(new BigDecimal("35000.0000"));

        // Step 4: Verify Transactional Outbox Event Created Atomically
        verify(outboxEventRepository, times(1)).save(any(OutboxEvent.class));

        // Step 5: Simulate Asynchronous Event Streaming to Kafka
        TransactionCompletedEventPayload eventPayload = new TransactionCompletedEventPayload(
                receipt.getTransactionId(), receipt.getTransactionReference(), idempotencyKey,
                TransactionType.P2P_TRANSFER, sourceAccId, sourceAccNum, destAccId, destAccNum,
                new BigDecimal("25000.00"), "INR", TransactionStatus.COMPLETED, "Consulting Fee", Instant.now()
        );

        // Step 6: Fraud Engine Evaluation
        when(fraudRuleEngine.evaluate(any())).thenReturn(FraudEvaluationResult.clean(5));
        FraudEvaluationResult fraudResult = fraudDetectionService.processTransactionEvent(eventPayload);
        assertThat(fraudResult.alertTriggered()).isFalse();

        // Step 7: Notification Dispatch to Destination Account Holder
        when(userAuthRepository.findById(destAccount.getCustomerProfile().getUserAuth().getId()))
                .thenReturn(Optional.of(destAccount.getCustomerProfile().getUserAuth()));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponse notif = notificationService.createNotification(
                destAccount.getCustomerProfile().getUserAuth().getId(),
                NotificationType.TRANSFER_SUCCESSFUL,
                "Money Received",
                "You received 25000.00 INR from Alice Smith",
                NotificationChannel.IN_APP,
                receipt.getTransactionReference(),
                "evt-123"
        );
        assertThat(notif).isNotNull();
        assertThat(notif.notificationType()).isEqualTo(NotificationType.TRANSFER_SUCCESSFUL);

        // Step 8: WORM Audit Log Verification
        verify(auditLogRepository, atLeastOnce()).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Complete Flow 2: Admin Operations (Dashboard -> High Risk Fraud Alert -> Review -> Freeze Account)")
    void testCompleteAdminOperationsLifecycle() {
        // Step 1: Fraud Engine Flags Structuring / High Risk Transfer
        TransactionCompletedEventPayload suspiciousPayload = new TransactionCompletedEventPayload(
                UUID.randomUUID(), "TXN-SUSPICIOUS-99", "idemp-fraud-1",
                TransactionType.P2P_TRANSFER, sourceAccId, sourceAccNum, destAccId, destAccNum,
                new BigDecimal("990000.00"), "INR", TransactionStatus.COMPLETED, "AML Structuring Test", Instant.now()
        );

        FraudEvaluationResult highRiskResult = FraudEvaluationResult.flagged(
                95, List.of("LARGE_AMOUNT_THRESHOLD", "VELOCITY_SPIKE"), "Potential Money Laundering / Structuring"
        );
        when(fraudRuleEngine.evaluate(any())).thenReturn(highRiskResult);
        when(accountRepository.findByAccountNumber(sourceAccNum)).thenReturn(Optional.of(sourceAccount));

        UUID alertId = UUID.randomUUID();
        FraudAlert persistedAlert = new FraudAlert(null, sourceAccount, "VELOCITY_SPIKE", 95, "Potential Money Laundering");
        persistedAlert.setId(alertId);
        when(fraudAlertRepository.save(any(FraudAlert.class))).thenReturn(persistedAlert);
        when(fraudAlertRepository.findById(alertId)).thenReturn(Optional.of(persistedAlert));

        FraudEvaluationResult alertOutcome = fraudDetectionService.processTransactionEvent(suspiciousPayload);
        assertThat(alertOutcome.alertTriggered()).isTrue();
        assertThat(alertOutcome.riskScore()).isEqualTo(95);

        // Step 2: Admin Reviews and Confirms Fraud Alert
        FraudReviewRequest reviewRequest = new FraudReviewRequest(FraudStatus.CONFIRMED_FRAUD, "Verified fraudulent structuring by compliance team.");
        FraudAlertResponse reviewedAlert = fraudDetectionService.reviewAlert(alertId, reviewRequest, adminId);

        assertThat(reviewedAlert.status()).isEqualTo(FraudStatus.CONFIRMED_FRAUD);
        assertThat(reviewedAlert.reviewerNotes()).contains("Verified fraudulent");

        // Step 3: Admin Freezes the Compromised Source Account
        when(accountRepository.findById(sourceAccId)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountStatusUpdateRequest freezeRequest = new AccountStatusUpdateRequest(AccountStatus.FROZEN, "AML Compliance Investigation");
        AccountResponse accountResponse = accountService.updateAccountStatus(sourceAccId, freezeRequest, adminId, "ROLE_ADMIN", "corr-test-1", "127.0.0.1");

        assertThat(accountResponse.status()).isEqualTo(AccountStatus.FROZEN);
        assertThat(sourceAccount.getStatus()).isEqualTo(AccountStatus.FROZEN);

        // Verify Audit Log Logged Account Freeze Action
        verify(auditLogRepository, atLeastOnce()).save(any(AuditLog.class));
    }
}
