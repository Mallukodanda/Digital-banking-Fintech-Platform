package com.fintech.bank.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Standard CloudEvents-inspired Versioned Event Envelope for all Domain Events.
 *
 * @param <T> Domain Payload Data Type
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DomainEvent<T> implements Serializable {

    private UUID eventId;
    private String eventType;
    private String aggregateType;
    private String aggregateId;
    private int eventVersion;
    private Instant timestamp;
    private String traceId;
    private T payload;

    public DomainEvent() {
    }

    public DomainEvent(String eventType, String aggregateType, String aggregateId, int eventVersion, String traceId, T payload) {
        this.eventId = UUID.randomUUID();
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventVersion = eventVersion;
        this.timestamp = Instant.now();
        this.traceId = traceId;
        this.payload = payload;
    }

    public static <T> DomainEvent<T> of(String eventType, String aggregateType, String aggregateId, int version, String traceId, T payload) {
        return new DomainEvent<>(eventType, aggregateType, aggregateId, version, traceId, payload);
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public void setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public int getEventVersion() {
        return eventVersion;
    }

    public void setEventVersion(int eventVersion) {
        this.eventVersion = eventVersion;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }
}
