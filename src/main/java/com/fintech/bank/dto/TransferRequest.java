package com.fintech.bank.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Transfer request payload for initiating funds transfer between accounts.
 */
@Schema(description = "Funds transfer initiation request payload")
public class TransferRequest {

    @NotNull(message = "Source account ID is required")
    @Schema(description = "UUID of the originating customer account", example = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d")
    private UUID sourceAccountId;

    @NotBlank(message = "Destination account number is required")
    @Size(min = 10, max = 20, message = "Account number must be between 10 and 20 characters")
    @Schema(description = "10-20 digit destination bank account number", example = "100000000002")
    private String destinationAccountNumber;

    @NotNull(message = "Transfer amount is required")
    @DecimalMin(value = "0.01", message = "Transfer amount must be strictly greater than 0")
    @Schema(description = "Amount to transfer with up to 4 decimal precision", example = "8000.0000")
    private BigDecimal amount;

    @Size(min = 3, max = 3, message = "Currency code must be exactly 3 characters")
    @Schema(description = "ISO 4217 Currency Code", example = "INR")
    private String currency = "INR";

    @Size(max = 255, message = "Description must not exceed 255 characters")
    @Schema(description = "Optional transaction memo or remark", example = "Vendor invoice payment")
    private String description;

    public TransferRequest() {
    }

    public TransferRequest(UUID sourceAccountId, String destinationAccountNumber, BigDecimal amount, String currency, String description) {
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountNumber = destinationAccountNumber;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
    }

    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public void setSourceAccountId(UUID sourceAccountId) {
        this.sourceAccountId = sourceAccountId;
    }

    public String getDestinationAccountNumber() {
        return destinationAccountNumber;
    }

    public void setDestinationAccountNumber(String destinationAccountNumber) {
        this.destinationAccountNumber = destinationAccountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
