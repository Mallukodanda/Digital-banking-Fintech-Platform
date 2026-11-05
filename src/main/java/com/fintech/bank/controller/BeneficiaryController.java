package com.fintech.bank.controller;

import com.fintech.bank.dto.*;
import com.fintech.bank.security.UserPrincipal;
import com.fintech.bank.service.BeneficiaryService;
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
 * REST Controller for managing verified Payees and Beneficiaries.
 */
@RestController
@RequestMapping("/api/v1/beneficiaries")
@Tag(name = "Beneficiary Management", description = "Endpoints for managing customer payees and pre-transfer verification")
@SecurityRequirement(name = "BearerAuthentication")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    public BeneficiaryController(BeneficiaryService beneficiaryService) {
        this.beneficiaryService = beneficiaryService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Add New Beneficiary", description = "Validates and saves a new destination account in the customer's payee address book")
    public ResponseEntity<ApiResponse<BeneficiaryResponse>> addBeneficiary(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateBeneficiaryRequest request) {

        BeneficiaryResponse response = beneficiaryService.addBeneficiary(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Beneficiary added successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "List Saved Beneficiaries", description = "Retrieves all active beneficiaries saved by the authenticated customer")
    public ResponseEntity<ApiResponse<List<BeneficiaryResponse>>> getMyBeneficiaries(
            @AuthenticationPrincipal UserPrincipal principal) {

        List<BeneficiaryResponse> beneficiaries = beneficiaryService.getBeneficiariesByUserId(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Beneficiaries retrieved successfully", beneficiaries));
    }

    @GetMapping("/paged")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "List Saved Beneficiaries (Paginated)", description = "Retrieves paginated active beneficiaries for the authenticated customer")
    public ResponseEntity<ApiResponse<Page<BeneficiaryResponse>>> getMyBeneficiariesPaged(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<BeneficiaryResponse> beneficiaries = beneficiaryService.getBeneficiariesByUserId(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Beneficiaries retrieved successfully", beneficiaries));
    }

    @GetMapping("/verify/{accountNumber}")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Pre-Transfer Payee Verification", description = "Verifies destination account existence, active eligibility, and returns masked account holder name")
    public ResponseEntity<ApiResponse<VerifyBeneficiaryResponse>> verifyBeneficiary(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String accountNumber) {

        VerifyBeneficiaryResponse response = beneficiaryService.verifyBeneficiary(principal.getId(), accountNumber);
        return ResponseEntity.ok(ApiResponse.success("Payee verification successful", response));
    }

    @DeleteMapping("/{beneficiaryId}")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Remove Beneficiary", description = "Removes a saved payee from the customer's address book")
    public ResponseEntity<ApiResponse<Void>> removeBeneficiary(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID beneficiaryId) {

        beneficiaryService.removeBeneficiary(principal.getId(), beneficiaryId);
        return ResponseEntity.ok(ApiResponse.success("Beneficiary removed successfully", null));
    }
}
