package com.fintech.bank.service.impl;

import com.fintech.bank.dto.DomainEvent;
import com.fintech.bank.service.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Production Kafka Event Publisher dispatching domain events asynchronously with broker callbacks.
 */
@Service
@SuppressWarnings("null")
public class KafkaEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public <T> CompletableFuture<Void> publishEvent(String topic, String partitionKey, DomainEvent<T> event) {
        log.info("Publishing event to topic '{}' [Key: {}, EventType: {}, EventId: {}]",
                topic, partitionKey, event.getEventType(), event.getEventId());

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, partitionKey, event);

        return future.thenAccept(result -> log.info("Successfully published event '{}' to topic '{}' [Partition: {}, Offset: {}]",
                event.getEventType(),
                result.getRecordMetadata().topic(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset()))
                .exceptionally(ex -> {
                    log.error("Failed to publish event '{}' to topic '{}' [Key: {}]: {}",
                            event.getEventType(), topic, partitionKey, ex.getMessage(), ex);
                    throw new RuntimeException("Kafka event publish failure for event: " + event.getEventId(), ex);
                });
    }
}
