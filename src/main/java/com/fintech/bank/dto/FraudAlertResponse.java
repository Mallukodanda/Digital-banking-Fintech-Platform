package com.fintech.bank.dto;

import com.fintech.bank.entity.FraudStatus;
import com.fintech.bank.entity.RiskLevel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Detailed representation of a fraud alert for administrative review.
 */
@Schema(description = "Fraud alert incident record for compliance and AML review")
public record FraudAlertResponse(
        UUID alertId,
        String transactionReference,
        UUID accountId,
        String accountNumber,
        String ruleType,
        int riskScore,
        RiskLevel riskLevel,
        FraudStatus status,
        String reviewerNotes,
        Instant createdAt,
        Instant updatedAt
) {
}
