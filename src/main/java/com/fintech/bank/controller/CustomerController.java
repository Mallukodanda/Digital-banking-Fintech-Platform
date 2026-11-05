package com.fintech.bank.controller;

import com.fintech.bank.dto.ApiResponse;
import com.fintech.bank.dto.UserProfileResponse;
import com.fintech.bank.security.UserPrincipal;
import com.fintech.bank.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer Profile REST Controller for customer-scoped operations.
 */
@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customer Management", description = "Customer profile and KYC endpoints")
@SecurityRequirement(name = "BearerAuthentication")
public class CustomerController {

    private final AuthService authService;

    public CustomerController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('ROLE_CUSTOMER') or hasRole('ROLE_ADMIN')")
    @Operation(summary = "Get Current User Profile", description = "Fetches the profile and KYC status of the currently authenticated customer")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        UserProfileResponse profile = authService.getCurrentUserProfile(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", profile));
    }
}
