package com.fintech.bank.controller;

import com.fintech.bank.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Administrative REST Controller restricted to ROLE_ADMIN and ROLE_AUDITOR.
 */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Administration & Auditing", description = "Privileged endpoints for system administrators and auditors")
@SecurityRequirement(name = "BearerAuthentication")
public class AdminController {

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Admin Dashboard Overview", description = "Privileged diagnostic summary for system administrators")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAdminDashboard() {
        Map<String, Object> adminData = Map.of(
                "status", "HEALTHY",
                "securityLevel", "HIGH",
                "adminAccessVerified", true,
                "timestamp", Instant.now().toString()
        );
        return ResponseEntity.ok(ApiResponse.success("Admin dashboard data retrieved", adminData));
    }
}
