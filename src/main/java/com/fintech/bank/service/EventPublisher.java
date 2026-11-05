package com.fintech.bank.service;

import com.fintech.bank.dto.DomainEvent;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for publishing domain events to the distributed event stream.
 */
public interface EventPublisher {

    /**
     * Publishes a versioned domain event to a target Kafka topic.
     *
     * @param topic Target Kafka topic
     * @param partitionKey Key used to route message to specific partition (e.g., accountId)
     * @param event The DomainEvent envelope
     * @return CompletableFuture completing when broker acknowledges receipt
     */
    <T> CompletableFuture<Void> publishEvent(String topic, String partitionKey, DomainEvent<T> event);
}
