package com.fintech.bank.controller;

import com.fintech.bank.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Health and diagnostics endpoint for system verification and container probes.
 */
@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health & Diagnostics", description = "Endpoints for platform liveness and system diagnostics")
public class HealthCheckController {

    @GetMapping
    @Operation(summary = "System Health Check", description = "Returns system operational status, version, and current server UTC timestamp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHealthStatus() {
        Map<String, Object> healthInfo = Map.of(
                "status", "UP",
                "service", "Digital Banking & FinTech Platform",
                "version", "1.0.0-PROD",
                "javaVersion", System.getProperty("java.version"),
                "timestamp", Instant.now().toString()
        );

        return ResponseEntity.ok(ApiResponse.success("System operational", healthInfo));
    }
}
