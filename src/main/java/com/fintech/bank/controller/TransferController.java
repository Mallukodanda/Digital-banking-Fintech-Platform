package com.fintech.bank.controller;

import com.fintech.bank.dto.ApiResponse;
import com.fintech.bank.dto.TransactionHistoryResponse;
import com.fintech.bank.dto.TransferReceiptResponse;
import com.fintech.bank.dto.TransferRequest;
import com.fintech.bank.security.UserPrincipal;
import com.fintech.bank.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

import java.util.UUID;

/**
 * REST Controller for initiating high-integrity financial transactions,
 * querying account transaction histories, and retrieving transfer receipts.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Financial Transfers & Ledger", description = "ACID-compliant double-entry transfer engine and ledger query endpoints")
@SecurityRequirement(name = "BearerAuth")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping("/transfers")
    @Operation(summary = "Initiate Funds Transfer", description = "Executes an atomic, concurrency-safe, double-entry peer-to-peer funds transfer")
    public ResponseEntity<ApiResponse<TransferReceiptResponse>> initiateTransfer(
            @Parameter(description = "Client idempotency key UUID", required = true, example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d")
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        UUID actorId = principal != null ? principal.getId() : null;
        boolean isAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        TransferReceiptResponse receipt = transferService.transferFunds(request, idempotencyKey, actorId, isAdmin);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Transfer executed successfully", receipt));
    }

    @GetMapping("/accounts/{accountId}/transactions")
    @PreAuthorize("@accountSecurity.isAccountOwner(#accountId, authentication) or hasRole('ADMIN')")
    @Operation(summary = "Get Account Transaction History", description = "Returns a paged list of transaction legs and ledger postings for the specified account")
    public ResponseEntity<ApiResponse<Page<TransactionHistoryResponse>>> getAccountTransactions(
            @PathVariable UUID accountId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<TransactionHistoryResponse> history = transferService.getAccountTransactionHistory(accountId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Transaction history retrieved successfully", history));
    }

    @GetMapping("/transfers/{transactionReference}")
    @Operation(summary = "Get Transfer Receipt", description = "Retrieves an immutable transaction receipt by master transaction reference")
    public ResponseEntity<ApiResponse<TransferReceiptResponse>> getReceiptByReference(
            @PathVariable String transactionReference) {

        TransferReceiptResponse receipt = transferService.getReceiptByTransactionReference(transactionReference);
        return ResponseEntity.ok(ApiResponse.success("Transfer receipt retrieved successfully", receipt));
    }
}
