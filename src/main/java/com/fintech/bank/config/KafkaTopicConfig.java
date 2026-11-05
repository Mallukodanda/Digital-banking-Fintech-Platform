package com.fintech.bank.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka Topic Initialization with multi-partition layout and Dead Letter Topics.
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic transactionCompletedTopic() {
        return TopicBuilder.name(BankingTopics.TRANSACTION_COMPLETED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic transactionCompletedDltTopic() {
        return TopicBuilder.name(BankingTopics.TRANSACTION_COMPLETED_DLT)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic fraudAlertsTopic() {
        return TopicBuilder.name(BankingTopics.FRAUD_ALERTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic auditEventsTopic() {
        return TopicBuilder.name(BankingTopics.AUDIT_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
