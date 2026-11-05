package com.fintech.bank.controller;

import com.fintech.bank.dto.*;
import com.fintech.bank.security.UserPrincipal;
import com.fintech.bank.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Customer deposit account operations.
 */
@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Account Management", description = "Endpoints for customer account creation, balance checks, and inquiries")
@SecurityRequirement(name = "BearerAuthentication")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Open New Deposit Account", description = "Provisions a new Checking or Savings account with a 10-digit Luhn check account number")
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateAccountRequest request) {

        AccountResponse response = accountService.createAccount(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account created successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "List Owned Accounts", description = "Retrieves all deposit accounts belonging to the authenticated customer")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getMyAccounts(
            @AuthenticationPrincipal UserPrincipal principal) {

        List<AccountResponse> accounts = accountService.getAccountsByUserId(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Accounts retrieved successfully", accounts));
    }

    @GetMapping("/paged")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "List Owned Accounts (Paginated)", description = "Retrieves paginated accounts belonging to the authenticated customer")
    public ResponseEntity<ApiResponse<Page<AccountResponse>>> getMyAccountsPaged(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<AccountResponse> accounts = accountService.getAccountsByUserId(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Accounts retrieved successfully", accounts));
    }

    @GetMapping("/{accountId}")
    @PreAuthorize("@accountSecurity.isAccountOwner(#accountId, authentication)")
    @Operation(summary = "Get Account by ID", description = "Retrieves specific account metadata. Protected by IDOR ownership check.")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccountById(@PathVariable UUID accountId) {
        AccountResponse response = accountService.getAccountById(accountId);
        return ResponseEntity.ok(ApiResponse.success("Account retrieved successfully", response));
    }

    @GetMapping("/{accountId}/balance")
    @PreAuthorize("@accountSecurity.isAccountOwner(#accountId, authentication)")
    @Operation(summary = "Get Account Balance", description = "Fetches real-time available, cached, and hold balance for an owned account.")
    public ResponseEntity<ApiResponse<AccountBalanceResponse>> getAccountBalance(@PathVariable UUID accountId) {
        AccountBalanceResponse balance = accountService.getAccountBalance(accountId);
        return ResponseEntity.ok(ApiResponse.success("Balance retrieved successfully", balance));
    }
}
