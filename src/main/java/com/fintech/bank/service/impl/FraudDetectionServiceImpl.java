package com.fintech.bank.service.impl;

import com.fintech.bank.dto.FraudAlertResponse;
import com.fintech.bank.dto.FraudEvaluationResult;
import com.fintech.bank.dto.FraudReviewRequest;
import com.fintech.bank.dto.TransactionCompletedEventPayload;
import com.fintech.bank.entity.*;
import com.fintech.bank.exception.ResourceNotFoundException;
import com.fintech.bank.repository.AccountRepository;
import com.fintech.bank.repository.FraudAlertRepository;
import com.fintech.bank.repository.JournalEntryRepository;
import com.fintech.bank.service.AuditService;
import com.fintech.bank.service.FraudDetectionService;
import com.fintech.bank.service.NotificationService;
import com.fintech.bank.service.fraud.FraudRuleEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Production implementation of FraudDetectionService.
 * Coordinates heuristic evaluation, alert persistence, security notifications,
 * and administrative compliance review workflows.
 */
@Service
@SuppressWarnings("null")
public class FraudDetectionServiceImpl implements FraudDetectionService {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionServiceImpl.class);

    private final FraudAlertRepository fraudAlertRepository;
    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final FraudRuleEngine fraudRuleEngine;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public FraudDetectionServiceImpl(FraudAlertRepository fraudAlertRepository,
                                     AccountRepository accountRepository,
                                     JournalEntryRepository journalEntryRepository,
                                     FraudRuleEngine fraudRuleEngine,
                                     NotificationService notificationService,
                                     AuditService auditService) {
        this.fraudAlertRepository = fraudAlertRepository;
        this.accountRepository = accountRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.fraudRuleEngine = fraudRuleEngine;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public FraudEvaluationResult processTransactionEvent(TransactionCompletedEventPayload payload) {
        FraudEvaluationResult result = fraudRuleEngine.evaluate(payload);

        if (result.alertTriggered()) {
            log.warn("High-risk transaction detected! [Ref: {}, Score: {}, Rules: {}]",
                    payload.getTransactionReference(), result.riskScore(), result.triggeredRules());

            Optional<Account> accountOpt = accountRepository.findByAccountNumber(payload.getSourceAccountNumber());
            Optional<JournalEntry> journalOpt = journalEntryRepository.findByTransactionReference(payload.getTransactionReference());

            if (accountOpt.isPresent()) {
                Account account = accountOpt.get();
                String ruleSummary = String.join(" | ", result.triggeredRules());

                FraudAlert alert = new FraudAlert(
                        journalOpt.orElse(null),
                        account,
                        result.triggeredRules().isEmpty() ? "HIGH_RISK_SCORE" : result.triggeredRules().get(0),
                        result.riskScore(),
                        ruleSummary
                );

                FraudAlert saved = fraudAlertRepository.save(alert);
                log.info("Persisted FraudAlert id: {} for account: {}", saved.getId(), account.getAccountNumber());

                // Dispatch security alert notification to customer
                UUID userId = account.getCustomerProfile().getUserAuth().getId();
                notificationService.createNotification(
                        userId,
                        NotificationType.FRAUD_ALERT,
                        "Security Warning: Suspicious Activity Flagged",
                        "A transfer of " + payload.getAmount() + " " + payload.getCurrency() + " from your account was flagged by our automated security systems for verification [Ref: " + payload.getTransactionReference() + "].",
                        NotificationChannel.IN_APP,
                        payload.getTransactionReference(),
                        "FRAUD-" + payload.getTransactionReference()
                );

                // Record compliance audit log
                auditService.recordAction(
                        null,
                        "SYSTEM_FRAUD_ENGINE",
                        "FRAUD_ALERT_GENERATED",
                        "FRAUD_ALERT",
                        saved.getId().toString(),
                        payload.getTransactionReference(),
                        "FRAUD_WORKER"
                );
            }
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FraudAlertResponse> getFraudAlerts(FraudStatus status, Pageable pageable) {
        if (status != null) {
            return fraudAlertRepository.findByStatusOrderByCreatedAtDesc(status, pageable).map(this::mapToResponse);
        }
        return fraudAlertRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public FraudAlertResponse getAlertById(UUID alertId) {
        FraudAlert alert = fraudAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("FraudAlert not found with ID: " + alertId));
        return mapToResponse(alert);
    }

    @Override
    @Transactional
    public FraudAlertResponse reviewAlert(UUID alertId, FraudReviewRequest request, UUID reviewerAdminId) {
        FraudAlert alert = fraudAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("FraudAlert not found with ID: " + alertId));

        FraudStatus previousStatus = alert.getStatus();
        alert.setStatus(request.getStatus());
        alert.setReviewerNotes(request.getReviewerNotes());

        FraudAlert updated = fraudAlertRepository.save(alert);
        log.info("FraudAlert '{}' reviewed by Admin '{}': Status changed from {} to {}",
                alertId, reviewerAdminId, previousStatus, updated.getStatus());

        auditService.recordAction(
                reviewerAdminId,
                "ROLE_ADMIN",
                "FRAUD_ALERT_RESOLVED",
                "FRAUD_ALERT",
                alertId.toString(),
                null,
                null
        );

        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public long getOpenAlertsCount() {
        return fraudAlertRepository.countByStatus(FraudStatus.FLAGGED) +
                fraudAlertRepository.countByStatus(FraudStatus.UNDER_REVIEW);
    }

    private FraudAlertResponse mapToResponse(FraudAlert alert) {
        return new FraudAlertResponse(
                alert.getId(),
                alert.getJournalEntry() != null ? alert.getJournalEntry().getTransactionReference() : "N/A",
                alert.getAccount().getId(),
                alert.getAccount().getAccountNumber(),
                alert.getRuleType(),
                alert.getRiskScore(),
                RiskLevel.fromScore(alert.getRiskScore()),
                alert.getStatus(),
                alert.getReviewerNotes(),
                alert.getCreatedAt(),
                alert.getUpdatedAt()
        );
    }
}
