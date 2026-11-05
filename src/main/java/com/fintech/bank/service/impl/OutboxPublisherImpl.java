package com.fintech.bank.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.bank.config.BankingTopics;
import com.fintech.bank.dto.DomainEvent;
import com.fintech.bank.entity.OutboxEvent;
import com.fintech.bank.entity.OutboxStatus;
import com.fintech.bank.repository.OutboxEventRepository;
import com.fintech.bank.service.EventPublisher;
import com.fintech.bank.service.OutboxPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Transactional Outbox Worker.
 * Periodically polls pending events written during database transactions,
 * guarantees At-Least-Once event delivery to Kafka, and prevents dual-write anomalies.
 */
@Service
public class OutboxPublisherImpl implements OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherImpl.class);
    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRIES = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public OutboxPublisherImpl(OutboxEventRepository outboxEventRepository,
                               EventPublisher eventPublisher,
                               ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Override
    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:2000}")
    @Transactional(propagation = Propagation.REQUIRED)
    public int processPendingOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findPendingEvents(
                OutboxStatus.PENDING,
                PageRequest.of(0, BATCH_SIZE)
        );

        if (pendingEvents.isEmpty()) {
            return 0;
        }

        log.info("Processing {} pending Transactional Outbox events for Kafka publication", pendingEvents.size());
        int processedCount = 0;

        for (OutboxEvent event : pendingEvents) {
            try {
                // Construct target topic and payload envelope
                String topic = resolveTopic(event.getEventType());
                String partitionKey = event.getAggregateId();

                DomainEvent<Object> domainEvent = DomainEvent.of(
                        event.getEventType(),
                        event.getAggregateType(),
                        event.getAggregateId(),
                        1,
                        null,
                        objectMapper.readValue(event.getPayload(), Object.class)
                );

                // Publish asynchronously to Kafka broker
                eventPublisher.publishEvent(topic, partitionKey, domainEvent).get();

                // Mark event as PUBLISHED
                event.setStatus(OutboxStatus.PUBLISHED);
                event.setProcessedAt(Instant.now());
                outboxEventRepository.save(event);
                processedCount++;

                log.debug("Outbox event '{}' marked PUBLISHED [AggregateId: {}]", event.getId(), event.getAggregateId());
            } catch (Exception ex) {
                log.error("Failed to publish Outbox event '{}' to Kafka [Retry: {}/{}]: {}",
                        event.getId(), event.getRetryCount() + 1, MAX_RETRIES, ex.getMessage());

                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() >= MAX_RETRIES) {
                    event.setStatus(OutboxStatus.FAILED);
                    log.error("Outbox event '{}' exceeded max retries. Marked as FAILED.", event.getId());
                }
                outboxEventRepository.save(event);
            }
        }

        return processedCount;
    }

    private String resolveTopic(String eventType) {
        if ("TRANSFER_COMPLETED".equalsIgnoreCase(eventType)) {
            return BankingTopics.TRANSACTION_COMPLETED;
        }
        return "banking.events.generic";
    }
}
