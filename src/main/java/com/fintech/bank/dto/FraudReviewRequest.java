package com.fintech.bank.dto;

import com.fintech.bank.entity.FraudStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload submitted by an administrator when reviewing and resolving a fraud alert.
 */
@Schema(description = "Fraud alert resolution payload")
public class FraudReviewRequest {

    @NotNull(message = "Review status is required")
    @Schema(description = "Target fraud status (UNDER_REVIEW, DISMISSED, CONFIRMED_FRAUD)", example = "DISMISSED")
    private FraudStatus status;

    @NotBlank(message = "Reviewer notes are mandatory for compliance audit trail")
    @Size(min = 5, max = 1000, message = "Reviewer notes must be between 5 and 1000 characters")
    @Schema(description = "Investigation summary and rationale", example = "Customer confirmed transaction was authorized via out-of-band verification.")
    private String reviewerNotes;

    public FraudReviewRequest() {
    }

    public FraudReviewRequest(FraudStatus status, String reviewerNotes) {
        this.status = status;
        this.reviewerNotes = reviewerNotes;
    }

    public FraudStatus getStatus() {
        return status;
    }

    public void setStatus(FraudStatus status) {
        this.status = status;
    }

    public String getReviewerNotes() {
        return reviewerNotes;
    }

    public void setReviewerNotes(String reviewerNotes) {
        this.reviewerNotes = reviewerNotes;
    }
}
