package com.fintech.bank.dto;

import com.fintech.bank.entity.AuditResult;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.util.UUID;

/**
 * Filter parameters for searching and paginating audit logs.
 */
public record AuditLogFilterRequest(
        UUID actorId,
        String actorUsername,
        String actorRole,
        String action,
        String resourceType,
        String resourceId,
        AuditResult result,
        String correlationId,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate
) {
}
