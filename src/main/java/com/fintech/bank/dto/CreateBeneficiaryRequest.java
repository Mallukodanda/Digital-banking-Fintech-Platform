package com.fintech.bank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating and validating a new beneficiary.
 */
public record CreateBeneficiaryRequest(
        @NotBlank(message = "Beneficiary name is required")
        @Size(min = 2, max = 100, message = "Beneficiary name must be between 2 and 100 characters")
        String beneficiaryName,

        @NotBlank(message = "Destination account number is required")
        @Pattern(regexp = "^\\d{10}$", message = "Destination account number must be exactly 10 digits")
        String destinationAccountNumber,

        @Size(max = 50, message = "Nickname cannot exceed 50 characters")
        String nickname
) {}
