package com.fintech.bank.dto;

import com.fintech.bank.entity.AccountStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for administrative account status modification (Freeze / Unfreeze / Close).
 */
public record AccountStatusUpdateRequest(
        @NotNull(message = "New account status is required")
        AccountStatus status,

        @NotBlank(message = "Audit justification reason is required")
        @Size(min = 5, max = 255, message = "Reason must be between 5 and 255 characters")
        String reason
) {}
