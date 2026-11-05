package com.fintech.bank.service;

import com.fintech.bank.dto.FraudEvaluationResult;
import com.fintech.bank.dto.TransactionCompletedEventPayload;
import com.fintech.bank.entity.*;
import com.fintech.bank.repository.TransactionLegRepository;
import com.fintech.bank.service.fraud.FraudRuleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FraudRuleEngine Unit Tests")
class FraudRuleEngineTest {

    @Mock
    private TransactionLegRepository transactionLegRepository;

    @InjectMocks
    private FraudRuleEngine fraudRuleEngine;

    private final UUID sourceAccountId = UUID.randomUUID();
    private final UUID destAccountId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        fraudRuleEngine.setLargeAmountThreshold(new BigDecimal("50000.00"));
        fraudRuleEngine.setAlertThresholdScore(60);
    }

    @Nested
    @DisplayName("Individual Rule Evaluations")
    class IndividualRuleTests {

        @Test
        @DisplayName("Should evaluate normal low-value transaction as clean")
        void shouldPassNormalTransaction() {
            TransactionCompletedEventPayload payload = new TransactionCompletedEventPayload(
                    UUID.randomUUID(), "TXN-NORMAL-1", "idemp-1", TransactionType.P2P_TRANSFER,
                    sourceAccountId, "100000000001", destAccountId, "100000000002",
                    new BigDecimal("1500.00"), "INR", TransactionStatus.COMPLETED, "Groceries", Instant.now()
            );

            when(transactionLegRepository.findByAccountIdOrderByCreatedAtDesc(sourceAccountId))
                    .thenReturn(List.of());

            FraudEvaluationResult result = fraudRuleEngine.evaluate(payload);

            assertThat(result.riskScore()).isEqualTo(0);
            assertThat(result.riskLevel()).isEqualTo(RiskLevel.LOW);
            assertThat(result.alertTriggered()).isFalse();
        }

        @Test
        @DisplayName("Should trigger LARGE_TRANSACTION_AMOUNT rule when amount exceeds 50,000")
        void shouldDetectLargeAmount() {
            TransactionCompletedEventPayload payload = new TransactionCompletedEventPayload(
                    UUID.randomUUID(), "TXN-LARGE-1", "idemp-2", TransactionType.P2P_TRANSFER,
                    sourceAccountId, "100000000001", destAccountId, "100000000002",
                    new BigDecimal("75000.00"), "INR", TransactionStatus.COMPLETED, "Car purchase", Instant.now()
            );

            when(transactionLegRepository.findByAccountIdOrderByCreatedAtDesc(sourceAccountId))
                    .thenReturn(List.of());

            FraudEvaluationResult result = fraudRuleEngine.evaluate(payload);

            assertThat(result.riskScore()).isEqualTo(50);
            assertThat(result.triggeredRules()).anyMatch(r -> r.contains("LARGE_TRANSACTION_AMOUNT"));
        }

        @Test
        @DisplayName("Should trigger RAPID_VELOCITY_TRANSFERS when 3+ transfers occur within 5 minutes")
        void shouldDetectRapidVelocity() {
            TransactionCompletedEventPayload payload = new TransactionCompletedEventPayload(
                    UUID.randomUUID(), "TXN-VELOCITY-1", "idemp-3", TransactionType.P2P_TRANSFER,
                    sourceAccountId, "100000000001", destAccountId, "100000000002",
                    new BigDecimal("2500.00"), "INR", TransactionStatus.COMPLETED, "Speed test", Instant.now()
            );

            JournalEntry dummyJournal = new JournalEntry();
            Account dummyAccount = new Account();
            dummyAccount.setId(sourceAccountId);

            TransactionLeg leg1 = new TransactionLeg(dummyJournal, dummyAccount, LegType.DEBIT, new BigDecimal("100.00"), "INR", BigDecimal.ZERO);
            leg1.setCreatedAt(Instant.now().minusSeconds(30));
            TransactionLeg leg2 = new TransactionLeg(dummyJournal, dummyAccount, LegType.DEBIT, new BigDecimal("100.00"), "INR", BigDecimal.ZERO);
            leg2.setCreatedAt(Instant.now().minusSeconds(60));
            TransactionLeg leg3 = new TransactionLeg(dummyJournal, dummyAccount, LegType.DEBIT, new BigDecimal("100.00"), "INR", BigDecimal.ZERO);
            leg3.setCreatedAt(Instant.now().minusSeconds(90));

            when(transactionLegRepository.findByAccountIdOrderByCreatedAtDesc(sourceAccountId))
                    .thenReturn(List.of(leg1, leg2, leg3));

            FraudEvaluationResult result = fraudRuleEngine.evaluate(payload);

            assertThat(result.riskScore()).isEqualTo(40);
            assertThat(result.triggeredRules()).anyMatch(r -> r.contains("RAPID_VELOCITY_TRANSFERS"));
        }

        @Test
        @DisplayName("Should trigger alert when multiple rules push cumulative risk score over 60")
        void shouldTriggerAlertOnCombinedRules() {
            // Amount = 100,000 (Triggers Large Amount: +50, and Round Structuring: +25 = 75 Score -> Alert Triggered!)
            TransactionCompletedEventPayload payload = new TransactionCompletedEventPayload(
                    UUID.randomUUID(), "TXN-ALERT-1", "idemp-4", TransactionType.P2P_TRANSFER,
                    sourceAccountId, "100000000001", destAccountId, "100000000002",
                    new BigDecimal("100000.00"), "INR", TransactionStatus.COMPLETED, "Mega transfer", Instant.now()
            );

            when(transactionLegRepository.findByAccountIdOrderByCreatedAtDesc(sourceAccountId))
                    .thenReturn(List.of());

            FraudEvaluationResult result = fraudRuleEngine.evaluate(payload);

            assertThat(result.riskScore()).isEqualTo(75);
            assertThat(result.riskLevel()).isEqualTo(RiskLevel.HIGH);
            assertThat(result.alertTriggered()).isTrue();
            assertThat(result.triggeredRules()).hasSize(2);
        }
    }
}
