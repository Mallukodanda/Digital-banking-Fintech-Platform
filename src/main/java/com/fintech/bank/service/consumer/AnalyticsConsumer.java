package com.fintech.bank.service.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.bank.config.BankingTopics;
import com.fintech.bank.dto.DomainEvent;
import com.fintech.bank.dto.TransactionCompletedEventPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * Real-time Financial Analytics Consumer.
 * Computes stream aggregates, transaction throughput, and total transferred volumes.
 */
@Service
public class AnalyticsConsumer {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsConsumer.class);

    private final ObjectMapper objectMapper;
    private final AtomicLong totalTransactionCount = new AtomicLong(0);
    private final DoubleAdder totalTransactionVolume = new DoubleAdder();

    public AnalyticsConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = BankingTopics.TRANSACTION_COMPLETED, groupId = BankingTopics.GROUP_ANALYTICS)
    public void aggregateMetrics(DomainEvent<Object> event) {
        log.info("[ANALYTICS SERVICE] Ingesting transaction metrics for event: {}", event.getEventId());

        try {
            TransactionCompletedEventPayload payload;
            if (event.getPayload() instanceof Map) {
                payload = objectMapper.convertValue(event.getPayload(), TransactionCompletedEventPayload.class);
            } else {
                payload = objectMapper.readValue(objectMapper.writeValueAsString(event.getPayload()), TransactionCompletedEventPayload.class);
            }

            long totalCount = totalTransactionCount.incrementAndGet();
            if (payload.getAmount() != null) {
                totalTransactionVolume.add(payload.getAmount().doubleValue());
            }

            log.info("[ANALYTICS METRICS] Live Stream Aggregates -> Total Completed Transactions: {}, Cumulative Volume: ~{} {}",
                    totalCount, (long) totalTransactionVolume.sum(), payload.getCurrency());

        } catch (Exception e) {
            log.error("[ANALYTICS SERVICE] Metric aggregation failed for event: {}", event.getEventId(), e);
            throw new RuntimeException("Analytics aggregation failed for event: " + event.getEventId(), e);
        }
    }

    public long getTotalTransactionCount() {
        return totalTransactionCount.get();
    }

    public double getTotalTransactionVolume() {
        return totalTransactionVolume.sum();
    }
}
