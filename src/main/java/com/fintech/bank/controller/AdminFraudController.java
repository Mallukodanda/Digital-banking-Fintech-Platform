package com.fintech.bank.controller;

import com.fintech.bank.dto.ApiResponse;
import com.fintech.bank.dto.FraudAlertResponse;
import com.fintech.bank.dto.FraudReviewRequest;
import com.fintech.bank.entity.FraudStatus;
import com.fintech.bank.security.UserPrincipal;
import com.fintech.bank.service.FraudDetectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Administrative REST Controller for Fraud & AML Operations.
 * Enables compliance officers and system administrators to review, investigate,
 * dismiss, or confirm fraud alerts.
 */
@RestController
@RequestMapping("/api/v1/admin/fraud")
@PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
@Tag(name = "Admin Fraud & Compliance", description = "Endpoints for compliance officers to review and resolve fraud alerts")
@SecurityRequirement(name = "BearerAuth")
public class AdminFraudController {

    private final FraudDetectionService fraudDetectionService;

    public AdminFraudController(FraudDetectionService fraudDetectionService) {
        this.fraudDetectionService = fraudDetectionService;
    }

    @GetMapping("/alerts")
    @Operation(summary = "Get Fraud Alerts", description = "Returns a paged list of fraud alerts optionally filtered by review status")
    public ResponseEntity<ApiResponse<Page<FraudAlertResponse>>> getFraudAlerts(
            @RequestParam(required = false) FraudStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<FraudAlertResponse> alerts = fraudDetectionService.getFraudAlerts(status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Fraud alerts retrieved successfully", alerts));
    }

    @GetMapping("/alerts/{alertId}")
    @Operation(summary = "Get Fraud Alert Detail", description = "Retrieves full investigation details for a specific fraud alert")
    public ResponseEntity<ApiResponse<FraudAlertResponse>> getAlertById(
            @PathVariable UUID alertId) {

        FraudAlertResponse alert = fraudDetectionService.getAlertById(alertId);
        return ResponseEntity.ok(ApiResponse.success("Fraud alert details retrieved successfully", alert));
    }

    @PatchMapping("/alerts/{alertId}/review")
    @Operation(summary = "Review and Resolve Fraud Alert", description = "Updates the review status and investigation findings for an alert")
    public ResponseEntity<ApiResponse<FraudAlertResponse>> reviewAlert(
            @PathVariable UUID alertId,
            @Valid @RequestBody FraudReviewRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        UUID adminId = principal != null ? principal.getId() : null;
        FraudAlertResponse response = fraudDetectionService.reviewAlert(alertId, request, adminId);
        return ResponseEntity.ok(ApiResponse.success("Fraud alert review completed successfully", response));
    }

    @GetMapping("/open-count")
    @Operation(summary = "Get Open Fraud Alerts Count", description = "Returns the count of active unresolved alerts for compliance dashboards")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getOpenAlertsCount() {
        long count = fraudDetectionService.getOpenAlertsCount();
        return ResponseEntity.ok(ApiResponse.success("Open fraud alerts count retrieved", Map.of("openAlertsCount", count)));
    }
}
