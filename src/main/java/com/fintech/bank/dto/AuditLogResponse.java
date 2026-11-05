package com.fintech.bank.dto;

import com.fintech.bank.entity.AuditLog;
import com.fintech.bank.entity.AuditResult;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for audit log retrieval.
 */
public record AuditLogResponse(
        UUID id,
        UUID actorId,
        String actorUsername,
        String actorRole,
        String action,
        String resourceType,
        String resourceId,
        AuditResult result,
        String correlationId,
        String clientIp,
        String metadata,
        Instant createdAt
) {
    public static AuditLogResponse fromEntity(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getActorId(),
                auditLog.getActorUsername(),
                auditLog.getActorRole(),
                auditLog.getAction(),
                auditLog.getResourceType(),
                auditLog.getResourceId(),
                auditLog.getResult(),
                auditLog.getCorrelationId(),
                auditLog.getClientIp(),
                auditLog.getMetadata(),
                auditLog.getCreatedAt()
        );
    }
}
