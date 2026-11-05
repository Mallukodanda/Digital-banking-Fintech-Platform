package com.fintech.bank.service;

import com.fintech.bank.dto.FraudAlertResponse;
import com.fintech.bank.dto.FraudEvaluationResult;
import com.fintech.bank.dto.FraudReviewRequest;
import com.fintech.bank.dto.TransactionCompletedEventPayload;
import com.fintech.bank.entity.*;
import com.fintech.bank.repository.AccountRepository;
import com.fintech.bank.repository.FraudAlertRepository;
import com.fintech.bank.repository.JournalEntryRepository;
import com.fintech.bank.service.fraud.FraudRuleEngine;
import com.fintech.bank.service.impl.FraudDetectionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FraudDetectionService Unit Tests")
@SuppressWarnings("null")
class FraudDetectionServiceTest {

    @Mock
    private FraudAlertRepository fraudAlertRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private JournalEntryRepository journalEntryRepository;

    @Mock
    private FraudRuleEngine fraudRuleEngine;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private FraudDetectionServiceImpl fraudDetectionService;

    private Account sourceAccount;
    private UserAuth user;
    private final UUID alertId = UUID.randomUUID();
    private final UUID accountId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        user = new UserAuth("alice@bank.com", "hash", UserRole.ROLE_CUSTOMER);
        user.setId(UUID.randomUUID());
        CustomerProfile profile = new CustomerProfile(user, "Alice", "Smith", "+10000000001");

        sourceAccount = new Account(profile, "100000000001", AccountType.CHECKING, "INR");
        sourceAccount.setId(accountId);
    }

    @Nested
    @DisplayName("Transaction Event Risk Processing")
    class EventProcessingTests {

        @Test
        @DisplayName("Should create FraudAlert and dispatch customer notification when risk threshold exceeded")
        void shouldPersistAlertWhenTriggered() {
            TransactionCompletedEventPayload payload = new TransactionCompletedEventPayload(
                    UUID.randomUUID(), "TXN-FRAUD-1", "idemp-1", TransactionType.P2P_TRANSFER,
                    accountId, "100000000001", UUID.randomUUID(), "100000000002",
                    new BigDecimal("85000.00"), "INR", TransactionStatus.COMPLETED, "Alert test", Instant.now()
            );

            FraudEvaluationResult evalResult = FraudEvaluationResult.flagged(
                    75, List.of("LARGE_TRANSACTION_AMOUNT", "ROUND_AMOUNT_STRUCTURING"), "High risk transfer"
            );

            when(fraudRuleEngine.evaluate(payload)).thenReturn(evalResult);
            when(accountRepository.findByAccountNumber("100000000001")).thenReturn(Optional.of(sourceAccount));
            when(journalEntryRepository.findByTransactionReference("TXN-FRAUD-1")).thenReturn(Optional.empty());

            FraudAlert alert = new FraudAlert(null, sourceAccount, "LARGE_TRANSACTION_AMOUNT", 75, "High risk transfer");
            alert.setId(alertId);
            when(fraudAlertRepository.save(any(FraudAlert.class))).thenReturn(alert);

            FraudEvaluationResult result = fraudDetectionService.processTransactionEvent(payload);

            assertThat(result.alertTriggered()).isTrue();
            verify(fraudAlertRepository, times(1)).save(any(FraudAlert.class));
            verify(notificationService, times(1)).createNotification(
                    eq(user.getId()),
                    eq(NotificationType.FRAUD_ALERT),
                    anyString(),
                    anyString(),
                    eq(NotificationChannel.IN_APP),
                    eq("TXN-FRAUD-1"),
                    eq("FRAUD-TXN-FRAUD-1")
            );
            verify(auditService, times(1)).recordAction(
                    isNull(), eq("SYSTEM_FRAUD_ENGINE"), eq("FRAUD_ALERT_GENERATED"), eq("FRAUD_ALERT"), any(), eq("TXN-FRAUD-1"), eq("FRAUD_WORKER")
            );
        }

        @Test
        @DisplayName("Should not create FraudAlert when risk score is below threshold")
        void shouldNotPersistAlertWhenClean() {
            TransactionCompletedEventPayload payload = new TransactionCompletedEventPayload(
                    UUID.randomUUID(), "TXN-CLEAN-1", "idemp-2", TransactionType.P2P_TRANSFER,
                    accountId, "100000000001", UUID.randomUUID(), "100000000002",
                    new BigDecimal("500.00"), "INR", TransactionStatus.COMPLETED, "Clean test", Instant.now()
            );

            when(fraudRuleEngine.evaluate(payload)).thenReturn(FraudEvaluationResult.clean(10));

            FraudEvaluationResult result = fraudDetectionService.processTransactionEvent(payload);

            assertThat(result.alertTriggered()).isFalse();
            verify(fraudAlertRepository, never()).save(any());
            verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Administrative Investigation & Review")
    class AdminReviewTests {

        @Test
        @DisplayName("Should update FraudAlert status and reviewer notes on resolution")
        void shouldReviewAndDismissAlert() {
            FraudAlert alert = new FraudAlert(null, sourceAccount, "LARGE_TRANSACTION_AMOUNT", 75, "Auto flagged");
            alert.setId(alertId);

            when(fraudAlertRepository.findById(alertId)).thenReturn(Optional.of(alert));
            when(fraudAlertRepository.save(any(FraudAlert.class))).thenAnswer(i -> i.getArgument(0));

            UUID adminId = UUID.randomUUID();
            FraudReviewRequest request = new FraudReviewRequest(FraudStatus.DISMISSED, "Customer confirmed valid vendor payment via phone call.");

            FraudAlertResponse response = fraudDetectionService.reviewAlert(alertId, request, adminId);

            assertThat(response.status()).isEqualTo(FraudStatus.DISMISSED);
            assertThat(response.reviewerNotes()).isEqualTo("Customer confirmed valid vendor payment via phone call.");
            verify(auditService, times(1)).recordAction(
                    eq(adminId), eq("ROLE_ADMIN"), eq("FRAUD_ALERT_RESOLVED"), eq("FRAUD_ALERT"), eq(alertId.toString()), isNull(), isNull()
            );
        }

        @Test
        @DisplayName("Should retrieve paged fraud alerts filtered by status")
        void shouldRetrievePagedAlerts() {
            FraudAlert alert = new FraudAlert(null, sourceAccount, "LARGE_AMOUNT", 80, "Notes");
            alert.setId(alertId);

            Pageable pageable = PageRequest.of(0, 10);
            when(fraudAlertRepository.findByStatusOrderByCreatedAtDesc(FraudStatus.FLAGGED, pageable))
                    .thenReturn(new PageImpl<>(List.of(alert), pageable, 1));

            Page<FraudAlertResponse> page = fraudDetectionService.getFraudAlerts(FraudStatus.FLAGGED, pageable);

            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).status()).isEqualTo(FraudStatus.FLAGGED);
        }
    }
}
