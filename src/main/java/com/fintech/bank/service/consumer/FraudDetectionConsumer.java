package com.fintech.bank.service.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.bank.config.BankingTopics;
import com.fintech.bank.dto.DomainEvent;
import com.fintech.bank.dto.FraudEvaluationResult;
import com.fintech.bank.dto.TransactionCompletedEventPayload;
import com.fintech.bank.service.FraudDetectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Real-time Fraud Detection Engine Consumer.
 * Ingests TransactionCompleted events and executes rule evaluation through FraudDetectionService.
 */
@Service
public class FraudDetectionConsumer {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionConsumer.class);

    private final FraudDetectionService fraudDetectionService;
    private final ObjectMapper objectMapper;

    public FraudDetectionConsumer(FraudDetectionService fraudDetectionService, ObjectMapper objectMapper) {
        this.fraudDetectionService = fraudDetectionService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = BankingTopics.TRANSACTION_COMPLETED, groupId = BankingTopics.GROUP_FRAUD_DETECTION)
    public void evaluateTransactionRisk(DomainEvent<Object> event) {
        log.info("[FRAUD DETECTION] Consuming event for risk evaluation: {}", event.getEventId());

        try {
            TransactionCompletedEventPayload payload;
            if (event.getPayload() instanceof Map) {
                payload = objectMapper.convertValue(event.getPayload(), TransactionCompletedEventPayload.class);
            } else {
                payload = objectMapper.readValue(objectMapper.writeValueAsString(event.getPayload()), TransactionCompletedEventPayload.class);
            }

            FraudEvaluationResult result = fraudDetectionService.processTransactionEvent(payload);
            log.info("[FRAUD DETECTION] Result for txn '{}': Score = {}/100, Tier = {}, Triggered = {}",
                    payload.getTransactionReference(), result.riskScore(), result.riskLevel(), result.alertTriggered());

        } catch (Exception e) {
            log.error("[FRAUD DETECTION] Evaluation failed for event: {}", event.getEventId(), e);
            throw new RuntimeException("Fraud evaluation failed for event: " + event.getEventId(), e);
        }
    }
}
