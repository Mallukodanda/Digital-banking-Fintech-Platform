package com.fintech.bank.service;

import com.fintech.bank.dto.AuditLogFilterRequest;
import com.fintech.bank.dto.AuditLogResponse;
import com.fintech.bank.entity.AuditLog;
import com.fintech.bank.entity.AuditResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for recording and querying immutable, append-only enterprise audit logs.
 */
public interface AuditService {

    /**
     * Record a standard audit action with default SUCCESS status.
     */
    void recordAction(UUID actorId, String actorRole, String action, String resourceType, String resourceId, String correlationId, String clientIp);

    /**
     * Record a comprehensive enterprise audit action with actor username, explicit result, and metadata.
     */
    void recordAction(UUID actorId, String actorUsername, String actorRole, String action, String resourceType, 
                      String resourceId, AuditResult result, String correlationId, String clientIp, String metadata);

    /**
     * Query audit logs with dynamic multi-criteria filtering and pagination.
     */
    Page<AuditLogResponse> getAuditLogs(AuditLogFilterRequest filter, Pageable pageable);

    /**
     * Retrieve a specific audit log by its unique Event ID.
     */
    AuditLogResponse getAuditLogById(UUID id);

    /**
     * Query audit logs by resource type.
     */
    Page<AuditLog> getAuditLogsByResource(String resourceType, Pageable pageable);
}
