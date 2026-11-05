package com.fintech.bank.controller;

import com.fintech.bank.dto.AccountResponse;
import com.fintech.bank.dto.AccountStatusUpdateRequest;
import com.fintech.bank.dto.ApiResponse;
import com.fintech.bank.security.CorrelationIdFilter;
import com.fintech.bank.security.UserPrincipal;
import com.fintech.bank.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Administrative REST Controller for bank-wide account management and status controls (Freeze / Unfreeze / Close).
 */
@RestController
@RequestMapping("/api/v1/admin/accounts")
@Tag(name = "Administration & Auditing", description = "Privileged endpoints for bank operators and auditors")
@SecurityRequirement(name = "BearerAuthentication")
public class AdminAccountController {

    private final AccountService accountService;

    public AdminAccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_AUDITOR')")
    @Operation(summary = "List All Bank Accounts (Admin)", description = "Returns paginated list of all accounts across all bank customers")
    public ResponseEntity<ApiResponse<Page<AccountResponse>>> getAllAccounts(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<AccountResponse> accounts = accountService.getAllAccountsForAdmin(pageable);
        return ResponseEntity.ok(ApiResponse.success("All bank accounts retrieved", accounts));
    }

    @PatchMapping("/{accountId}/status")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Update Account Status (Freeze / Unfreeze / Close)", description = "Administratively modifies account status with mandatory audit trail logging")
    public ResponseEntity<ApiResponse<AccountResponse>> updateAccountStatus(
            @PathVariable UUID accountId,
            @Valid @RequestBody AccountStatusUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {

        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
        String clientIp = httpRequest.getRemoteAddr();

        AccountResponse response = accountService.updateAccountStatus(
                accountId,
                request,
                principal.getId(),
                principal.getRole().name(),
                correlationId,
                clientIp
        );

        return ResponseEntity.ok(ApiResponse.success("Account status updated to " + request.status(), response));
    }
}
