package com.fintech.bank.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fintech.bank.entity.TransactionStatus;
import com.fintech.bank.entity.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Event Schema & Serialization Unit Tests")
class EventSchemaSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Should serialize and deserialize DomainEvent envelope correctly")
    void shouldSerializeAndDeserializeDomainEvent() throws Exception {
        UUID txnId = UUID.randomUUID();
        TransactionCompletedEventPayload payload = new TransactionCompletedEventPayload(
                txnId,
                "TXN-1712567890-ABCD",
                "idemp-12345",
                TransactionType.P2P_TRANSFER,
                UUID.randomUUID(),
                "100000000001",
                UUID.randomUUID(),
                "100000000002",
                new BigDecimal("8000.00"),
                "INR",
                TransactionStatus.COMPLETED,
                "Rent transfer",
                Instant.now()
        );

        DomainEvent<TransactionCompletedEventPayload> domainEvent = DomainEvent.of(
                "TRANSACTION_COMPLETED",
                "TRANSFER",
                "TXN-1712567890-ABCD",
                1,
                "corr-trace-999",
                payload
        );

        String json = objectMapper.writeValueAsString(domainEvent);
        assertThat(json).contains("TRANSACTION_COMPLETED");
        assertThat(json).contains("TXN-1712567890-ABCD");
        assertThat(json).contains("8000.00");

        @SuppressWarnings("unchecked")
        DomainEvent<TransactionCompletedEventPayload> deserialized = objectMapper.readValue(json, DomainEvent.class);

        assertThat(deserialized).isNotNull();
        assertThat(deserialized.getEventType()).isEqualTo("TRANSACTION_COMPLETED");
        assertThat(deserialized.getEventVersion()).isEqualTo(1);
        assertThat(deserialized.getTraceId()).isEqualTo("corr-trace-999");
    }
}
