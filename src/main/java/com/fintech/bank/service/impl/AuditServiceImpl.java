package com.fintech.bank.service.impl;

import com.fintech.bank.dto.AuditLogFilterRequest;
import com.fintech.bank.dto.AuditLogResponse;
import com.fintech.bank.entity.AuditLog;
import com.fintech.bank.entity.AuditResult;
import com.fintech.bank.exception.ResourceNotFoundException;
import com.fintech.bank.repository.AuditLogRepository;
import com.fintech.bank.repository.AuditLogSpecification;
import com.fintech.bank.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of AuditService writing append-only immutable records directly to the audit_logs table.
 * Uses REQUIRES_NEW propagation so audit records persist even if an outer business transaction fails or rolls back.
 */
@Service
@SuppressWarnings("null")
public class AuditServiceImpl implements AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);

    private final AuditLogRepository auditLogRepository;

    public AuditServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAction(UUID actorId, String actorRole, String action, String resourceType, String resourceId, String correlationId, String clientIp) {
        recordAction(actorId, null, actorRole, action, resourceType, resourceId, AuditResult.SUCCESS, correlationId, clientIp, null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAction(UUID actorId, String actorUsername, String actorRole, String action, String resourceType, 
                             String resourceId, AuditResult result, String correlationId, String clientIp, String metadata) {
        AuditLog auditLog = new AuditLog(
                actorId,
                actorUsername,
                actorRole,
                action,
                resourceType,
                resourceId,
                result != null ? result : AuditResult.SUCCESS,
                correlationId,
                clientIp,
                metadata
        );

        auditLogRepository.save(auditLog);
        log.info("Audit logged [ID: {}]: action=[{}] result=[{}] resource=[{}:{}] actor=[{}] correlationId=[{}]",
                auditLog.getId(), action, result, resourceType, resourceId, actorUsername != null ? actorUsername : actorId, correlationId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(AuditLogFilterRequest filter, Pageable pageable) {
        return auditLogRepository.findAll(AuditLogSpecification.withFilter(filter), pageable)
                .map(AuditLogResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogResponse getAuditLogById(UUID id) {
        AuditLog logEntry = auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AuditLog entry not found with ID: " + id));
        return AuditLogResponse.fromEntity(logEntry);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByResource(String resourceType, Pageable pageable) {
        return auditLogRepository.findByResourceType(resourceType, pageable);
    }
}
