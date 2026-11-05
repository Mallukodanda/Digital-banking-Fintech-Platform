package com.fintech.bank.service;

/**
 * Interface for the Transactional Outbox background polling worker.
 */
public interface OutboxPublisher {

    /**
     * Polls pending outbox records, publishes them to Kafka, and updates their state.
     *
     * @return Number of successfully processed events
     */
    int processPendingOutboxEvents();
}
