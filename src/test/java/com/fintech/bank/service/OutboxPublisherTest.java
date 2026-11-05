package com.fintech.bank.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.bank.config.BankingTopics;
import com.fintech.bank.entity.OutboxEvent;
import com.fintech.bank.entity.OutboxStatus;
import com.fintech.bank.repository.OutboxEventRepository;
import com.fintech.bank.service.impl.OutboxPublisherImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Transactional Outbox Publisher Unit Tests")
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxPublisherImpl outboxPublisher;

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("Should poll pending outbox events, publish to Kafka, and mark them PROCESSED")
    void shouldPublishPendingOutboxEventsSuccessfully() throws Exception {
        OutboxEvent event = new OutboxEvent(
                "TRANSFER",
                "TXN-1712567890-A8F2BC1D",
                "TRANSFER_COMPLETED",
                "{\"transactionReference\":\"TXN-1712567890-A8F2BC1D\",\"amount\":8000.00}"
        );

        when(outboxEventRepository.findPendingEvents(eq(OutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(event));
        when(objectMapper.readValue(any(String.class), eq(Object.class))).thenReturn("parsedPayload");
        when(eventPublisher.publishEvent(eq(BankingTopics.TRANSACTION_COMPLETED), eq("TXN-1712567890-A8F2BC1D"), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        int processed = outboxPublisher.processPendingOutboxEvents();

        assertThat(processed).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(event.getProcessedAt()).isNotNull();
        verify(outboxEventRepository, times(1)).save(event);
    }

    @Test
    @DisplayName("Should increment retryCount when Kafka publish fails")
    void shouldIncrementRetryCountOnPublishFailure() throws Exception {
        OutboxEvent event = new OutboxEvent(
                "TRANSFER",
                "TXN-FAIL-001",
                "TRANSFER_COMPLETED",
                "{\"amount\":1000.00}"
        );

        when(outboxEventRepository.findPendingEvents(eq(OutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(event));
        when(objectMapper.readValue(any(String.class), eq(Object.class))).thenReturn("parsedPayload");

        CompletableFuture<Void> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka Broker Unavailable"));
        when(eventPublisher.publishEvent(any(), any(), any())).thenReturn(failedFuture);

        int processed = outboxPublisher.processPendingOutboxEvents();

        assertThat(processed).isEqualTo(0);
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        verify(outboxEventRepository, times(1)).save(event);
    }
}
