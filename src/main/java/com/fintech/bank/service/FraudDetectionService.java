package com.fintech.bank.service;

import com.fintech.bank.dto.FraudAlertResponse;
import com.fintech.bank.dto.FraudEvaluationResult;
import com.fintech.bank.dto.FraudReviewRequest;
import com.fintech.bank.dto.TransactionCompletedEventPayload;
import com.fintech.bank.entity.FraudStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for fraud evaluation, risk alerting, and administrative investigation review.
 */
public interface FraudDetectionService {

    /**
     * Evaluates a completed transaction event against fraud heuristic rules,
     * persists FraudAlert if risk threshold is exceeded, and returns evaluation results.
     */
    FraudEvaluationResult processTransactionEvent(TransactionCompletedEventPayload payload);

    /**
     * Retrieves paged fraud alerts filtered optionally by review status.
     */
    Page<FraudAlertResponse> getFraudAlerts(FraudStatus status, Pageable pageable);

    /**
     * Retrieves single fraud alert detail.
     */
    FraudAlertResponse getAlertById(UUID alertId);

    /**
     * Updates the status and reviewer notes of a fraud alert during administrative investigation.
     */
    FraudAlertResponse reviewAlert(UUID alertId, FraudReviewRequest request, UUID reviewerAdminId);

    /**
     * Counts open (FLAGGED + UNDER_REVIEW) fraud alerts for compliance dashboards.
     */
    long getOpenAlertsCount();
}
