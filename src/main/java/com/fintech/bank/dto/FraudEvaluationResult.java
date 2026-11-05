package com.fintech.bank.dto;

import com.fintech.bank.entity.RiskLevel;

import java.util.List;

/**
 * Result of evaluating educational rule-based fraud detection on a transaction.
 */
public record FraudEvaluationResult(
        int riskScore,
        RiskLevel riskLevel,
        boolean alertTriggered,
        List<String> triggeredRules,
        String summary
) {
    public static FraudEvaluationResult clean(int score) {
        return new FraudEvaluationResult(score, RiskLevel.fromScore(score), false, List.of(), "Transaction passed baseline fraud checks");
    }

    public static FraudEvaluationResult flagged(int score, List<String> rules, String summary) {
        return new FraudEvaluationResult(score, RiskLevel.fromScore(score), true, rules, summary);
    }
}
