package com.fintech.bank.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fintech.bank.dto.DomainEvent;
import com.fintech.bank.dto.TransactionCompletedEventPayload;
import com.fintech.bank.entity.*;
import com.fintech.bank.repository.AccountRepository;
import com.fintech.bank.service.consumer.AnalyticsConsumer;
import com.fintech.bank.service.consumer.AuditConsumer;
import com.fintech.bank.service.consumer.FraudDetectionConsumer;
import com.fintech.bank.service.consumer.NotificationConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kafka Consumers Unit & Flow Tests")
class KafkaConsumerIntegrationTest {

    @Mock
    private FraudDetectionService fraudDetectionService;

    @Mock
    private AuditService auditService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AccountRepository accountRepository;

    private ObjectMapper objectMapper;
    private NotificationConsumer notificationConsumer;
    private FraudDetectionConsumer fraudDetectionConsumer;
    private AuditConsumer auditConsumer;
    private AnalyticsConsumer analyticsConsumer;

    private TransactionCompletedEventPayload samplePayload;
    private DomainEvent<Object> sampleDomainEvent;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        notificationConsumer = new NotificationConsumer(objectMapper, notificationService, accountRepository);
        fraudDetectionConsumer = new FraudDetectionConsumer(fraudDetectionService, objectMapper);
        auditConsumer = new AuditConsumer(auditService, objectMapper);
        analyticsConsumer = new AnalyticsConsumer(objectMapper);

        samplePayload = new TransactionCompletedEventPayload(
                UUID.randomUUID(),
                "TXN-1712567890-TEST",
                "idemp-1111",
                TransactionType.P2P_TRANSFER,
                UUID.randomUUID(),
                "100000000001",
                UUID.randomUUID(),
                "100000000002",
                new BigDecimal("8000.00"),
                "INR",
                TransactionStatus.COMPLETED,
                "Payment",
                Instant.now()
        );

        sampleDomainEvent = DomainEvent.of(
                "TRANSFER_COMPLETED",
                "TRANSFER",
                "TXN-1712567890-TEST",
                1,
                "corr-123",
                samplePayload
        );
    }

    @Nested
    @DisplayName("Notification Service Consumer")
    class NotificationConsumerTests {
        @Test
        @DisplayName("Should successfully consume event and log alerts")
        void shouldProcessNotification() {
            notificationConsumer.handleTransactionCompleted(sampleDomainEvent);
            // Verify no exceptions thrown
        }
    }

    @Nested
    @DisplayName("Fraud Detection Consumer")
    class FraudDetectionConsumerTests {

        @Test
        @DisplayName("Should delegate transaction completed event to FraudDetectionService")
        void shouldDelegateToFraudDetectionService() {
            fraudDetectionConsumer.evaluateTransactionRisk(sampleDomainEvent);

            verify(fraudDetectionService, times(1)).processTransactionEvent(any(TransactionCompletedEventPayload.class));
        }
    }

    @Nested
    @DisplayName("Audit Service Consumer")
    class AuditConsumerTests {
        @Test
        @DisplayName("Should record audit action asynchronously")
        void shouldRecordAuditLog() {
            auditConsumer.recordAuditLog(sampleDomainEvent);

            verify(auditService, times(1)).recordAction(
                    isNull(),
                    eq("SYSTEM_KAFKA_STREAM"),
                    eq("SYSTEM"),
                    eq(AuditAction.TRANSFER_COMPLETED),
                    eq("TRANSFER"),
                    eq("TXN-1712567890-TEST"),
                    eq(AuditResult.SUCCESS),
                    eq("corr-123"),
                    eq("KAFKA_BROKER"),
                    any(String.class)
            );
        }
    }

    @Nested
    @DisplayName("Analytics Service Consumer")
    class AnalyticsConsumerTests {
        @Test
        @DisplayName("Should accumulate live transaction throughput and volume metrics")
        void shouldAccumulateMetrics() {
            analyticsConsumer.aggregateMetrics(sampleDomainEvent);

            assertThat(analyticsConsumer.getTotalTransactionCount()).isEqualTo(1);
            assertThat(analyticsConsumer.getTotalTransactionVolume()).isEqualTo(8000.0);
        }
    }
}
