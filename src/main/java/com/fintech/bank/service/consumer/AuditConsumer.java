package com.fintech.bank.service.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.bank.config.BankingTopics;
import com.fintech.bank.dto.DomainEvent;
import com.fintech.bank.dto.TransactionCompletedEventPayload;
import com.fintech.bank.entity.AuditAction;
import com.fintech.bank.entity.AuditResult;
import com.fintech.bank.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Asynchronous Audit Log Consumer.
 * Ingests transaction events from Kafka and records immutable audit trail entries
 * with full metadata, correlation ID, and execution status.
 */
@Service
public class AuditConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditConsumer.class);

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public AuditConsumer(AuditService auditService, ObjectMapper objectMapper) {
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = BankingTopics.TRANSACTION_COMPLETED, groupId = BankingTopics.GROUP_AUDIT)
    public void recordAuditLog(DomainEvent<Object> event) {
        log.info("[AUDIT SERVICE] Consuming transaction event for audit log [EventId: {}]", event.getEventId());

        try {
            TransactionCompletedEventPayload payload;
            if (event.getPayload() instanceof Map) {
                payload = objectMapper.convertValue(event.getPayload(), TransactionCompletedEventPayload.class);
            } else {
                payload = objectMapper.readValue(objectMapper.writeValueAsString(event.getPayload()), TransactionCompletedEventPayload.class);
            }

            String metadataJson = objectMapper.writeValueAsString(Map.of(
                    "sourceAccountNumber", payload.getSourceAccountNumber(),
                    "destinationAccountNumber", payload.getDestinationAccountNumber(),
                    "amount", payload.getAmount(),
                    "currency", payload.getCurrency(),
                    "transactionType", payload.getTransactionType(),
                    "idempotencyKey", payload.getIdempotencyKey() != null ? payload.getIdempotencyKey() : "N/A"
            ));

            auditService.recordAction(
                    null,
                    "SYSTEM_KAFKA_STREAM",
                    "SYSTEM",
                    AuditAction.TRANSFER_COMPLETED,
                    "TRANSFER",
                    payload.getTransactionReference(),
                    AuditResult.SUCCESS,
                    event.getTraceId(),
                    "KAFKA_BROKER",
                    metadataJson
            );

            log.debug("[AUDIT SERVICE] Recorded audit log for transaction reference: {}", payload.getTransactionReference());
        } catch (Exception e) {
            log.error("[AUDIT SERVICE] Failed to record audit log for event: {}", event.getEventId(), e);
            throw new RuntimeException("Audit recording failed for event: " + event.getEventId(), e);
        }
    }
}
