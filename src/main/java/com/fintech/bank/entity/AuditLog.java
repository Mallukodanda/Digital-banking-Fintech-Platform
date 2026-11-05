package com.fintech.bank.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable AuditLog entity mapping to audit_logs table.
 * Designed for append-only regulatory compliance and security tracking.
 */
@Entity
@Table(name = "audit_logs")
@EntityListeners(AuditingEntityListener.class)
public class AuditLog implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_username", length = 100)
    private String actorUsername;

    @Column(name = "actor_role", length = 50)
    private String actorRole;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "resource_type", nullable = false, length = 50)
    private String resourceType;

    @Column(name = "resource_id", nullable = false, length = 64)
    private String resourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 30)
    private AuditResult result = AuditResult.SUCCESS;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AuditLog() {
    }

    public AuditLog(UUID actorId, String actorRole, String action, String resourceType, String resourceId, String correlationId, String clientIp) {
        this(actorId, null, actorRole, action, resourceType, resourceId, AuditResult.SUCCESS, correlationId, clientIp, null);
    }

    public AuditLog(UUID actorId, String actorUsername, String actorRole, String action, String resourceType, 
                    String resourceId, AuditResult result, String correlationId, String clientIp, String metadata) {
        this.actorId = actorId;
        this.actorUsername = actorUsername;
        this.actorRole = actorRole;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.result = result != null ? result : AuditResult.SUCCESS;
        this.correlationId = correlationId;
        this.clientIp = clientIp;
        this.metadata = metadata;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public String getActorRole() {
        return actorRole;
    }

    public String getAction() {
        return action;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public AuditResult getResult() {
        return result;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getClientIp() {
        return clientIp;
    }

    public String getMetadata() {
        return metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
