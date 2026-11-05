package com.fintech.bank.service.consumer;

import com.fintech.bank.config.BankingTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

/**
 * Dead Letter Topic (DLT) Monitoring & Poison Pill Consumer.
 * Ingests unrecoverable / corrupted messages forwarded after retry exhaustion.
 */
@Service
public class DeadLetterQueueConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterQueueConsumer.class);

    @KafkaListener(topics = BankingTopics.TRANSACTION_COMPLETED_DLT, groupId = BankingTopics.GROUP_DLQ_MONITOR)
    public void handleDeadLetterMessage(
            Object payload,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String originalTopic,
            @Header(value = KafkaHeaders.RECEIVED_PARTITION, required = false) Integer partition,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset,
            @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String exceptionMessage) {

        log.error("[CRITICAL DLQ ALERT] Unrecoverable poison message captured in DLT topic! " +
                "Source Topic: {}, Partition: {}, Offset: {}, Error: {}, Payload: {}",
                originalTopic, partition, offset, exceptionMessage, payload);

        // In production: forward alert to PagerDuty/Slack and persist in dead_letter_records table
    }
}
