package com.fintech.bank.service.fraud;

import com.fintech.bank.dto.FraudEvaluationResult;
import com.fintech.bank.dto.TransactionCompletedEventPayload;
import com.fintech.bank.entity.FraudRule;
import com.fintech.bank.entity.TransactionLeg;
import com.fintech.bank.repository.TransactionLegRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Educational Rule-Based Fraud Detection Engine.
 * Evaluates configurable heuristic rules against transaction events
 * to calculate a normalized risk score (0 to 100).
 */
@Component
public class FraudRuleEngine {

    private static final Logger log = LoggerFactory.getLogger(FraudRuleEngine.class);

    @Value("${app.fraud.large-amount-threshold:50000.00}")
    private BigDecimal largeAmountThreshold = new BigDecimal("50000.00");

    @Value("${app.fraud.alert-threshold-score:60}")
    private int alertThresholdScore = 60;

    private final TransactionLegRepository transactionLegRepository;

    public FraudRuleEngine(TransactionLegRepository transactionLegRepository) {
        this.transactionLegRepository = transactionLegRepository;
    }

    /**
     * Evaluates all configured fraud detection rules on a completed transaction event.
     */
    public FraudEvaluationResult evaluate(TransactionCompletedEventPayload payload) {
        if (payload == null || payload.getAmount() == null) {
            return FraudEvaluationResult.clean(0);
        }

        int score = 0;
        List<String> triggeredRules = new ArrayList<>();
        BigDecimal amount = payload.getAmount();

        // Rule 1: Unusually Large Transaction
        if (amount.compareTo(largeAmountThreshold) > 0) {
            score += FraudRule.LARGE_TRANSACTION_AMOUNT.getDefaultWeight();
            triggeredRules.add(FraudRule.LARGE_TRANSACTION_AMOUNT.name() + " (Amount: " + amount + " > " + largeAmountThreshold + ")");
            log.debug("Rule triggered: LARGE_TRANSACTION_AMOUNT for txn: {}", payload.getTransactionReference());
        }

        // Rule 2: Round Amount Structuring Detection (e.g. 10000, 20000, 50000)
        if (isRoundStructuringAmount(amount)) {
            score += FraudRule.ROUND_AMOUNT_STRUCTURING.getDefaultWeight();
            triggeredRules.add(FraudRule.ROUND_AMOUNT_STRUCTURING.name() + " (Exact round amount: " + amount + ")");
            log.debug("Rule triggered: ROUND_AMOUNT_STRUCTURING for txn: {}", payload.getTransactionReference());
        }

        // Rule 3: Velocity Check (Recent activity on source account)
        if (payload.getSourceAccountId() != null) {
            List<TransactionLeg> recentLegs = transactionLegRepository.findByAccountIdOrderByCreatedAtDesc(payload.getSourceAccountId());
            Instant fiveMinutesAgo = Instant.now().minusSeconds(300);
            long rapidTransfersCount = recentLegs.stream()
                    .filter(l -> l.getCreatedAt() != null && l.getCreatedAt().isAfter(fiveMinutesAgo))
                    .count();

            if (rapidTransfersCount >= 3) {
                score += FraudRule.RAPID_VELOCITY_TRANSFERS.getDefaultWeight();
                triggeredRules.add(FraudRule.RAPID_VELOCITY_TRANSFERS.name() + " (" + rapidTransfersCount + " transfers in past 5 minutes)");
                log.debug("Rule triggered: RAPID_VELOCITY_TRANSFERS for txn: {}", payload.getTransactionReference());
            }
        }

        // Clamp final score between 0 and 100
        int finalScore = Math.min(100, Math.max(0, score));
        boolean alertTriggered = finalScore >= alertThresholdScore;
        String summary = triggeredRules.isEmpty()
                ? "Transaction evaluated normal (Score: " + finalScore + ")"
                : "Rules triggered: " + String.join(", ", triggeredRules);

        log.info("Fraud evaluation complete for txn '{}': Risk Score = {}/100, Alert Triggered = {}",
                payload.getTransactionReference(), finalScore, alertTriggered);

        return new FraudEvaluationResult(finalScore, com.fintech.bank.entity.RiskLevel.fromScore(finalScore), alertTriggered, triggeredRules, summary);
    }

    private boolean isRoundStructuringAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(new BigDecimal("10000.00")) < 0) {
            return false;
        }
        return amount.remainder(new BigDecimal("10000")).compareTo(BigDecimal.ZERO) == 0;
    }

    public void setLargeAmountThreshold(BigDecimal threshold) {
        this.largeAmountThreshold = threshold;
    }

    public void setAlertThresholdScore(int score) {
        this.alertThresholdScore = score;
    }
}
