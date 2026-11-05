package com.fintech.bank.controller;

import com.fintech.bank.dto.ApiResponse;
import com.fintech.bank.dto.AuditLogFilterRequest;
import com.fintech.bank.dto.AuditLogResponse;
import com.fintech.bank.entity.AuditResult;
import com.fintech.bank.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Administrative REST Controller for viewing enterprise audit logs.
 * Append-only, read-only query interface protected by strict RBAC (ROLE_ADMIN, ROLE_AUDITOR).
 */
@RestController
@RequestMapping({"/api/v1/admin/audit-logs", "/api/admin/audit-logs"})
@PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
@Tag(name = "Admin Audit Logs", description = "Privileged endpoints for compliance and security auditing")
@SecurityRequirement(name = "BearerAuthentication")
public class AdminAuditLogController {

    private final AuditService auditService;

    public AdminAuditLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(summary = "Search and Paginate Audit Logs", 
               description = "Retrieves an immutable audit trail filtered by actor, action, resource, result, date range, or correlation ID")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditLogs(
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String actorUsername,
            @RequestParam(required = false) String actorRole,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) AuditResult result,
            @RequestParam(required = false) String correlationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        AuditLogFilterRequest filter = new AuditLogFilterRequest(
                actorId,
                actorUsername,
                actorRole,
                action,
                resourceType,
                resourceId,
                result,
                correlationId,
                fromDate,
                toDate
        );

        Page<AuditLogResponse> logs = auditService.getAuditLogs(filter, pageable);
        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved successfully", logs));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Audit Log by Event ID", description = "Retrieves full details for an individual immutable audit event")
    public ResponseEntity<ApiResponse<AuditLogResponse>> getAuditLogById(@PathVariable UUID id) {
        AuditLogResponse auditLog = auditService.getAuditLogById(id);
        return ResponseEntity.ok(ApiResponse.success("Audit log entry retrieved successfully", auditLog));
    }
}
