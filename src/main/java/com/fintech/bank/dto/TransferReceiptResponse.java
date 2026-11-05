package com.fintech.bank.dto;

import com.fintech.bank.entity.TransactionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Detailed Transfer Receipt response returned upon successful transaction execution or idempotent replay.
 */
@Schema(description = "Financial transaction receipt containing immutable ledger reference and execution status")
public class TransferReceiptResponse {

    @Schema(description = "Internal unique UUID of the journal entry")
    private UUID transactionId;

    @Schema(description = "External human-readable transaction reference code", example = "TXN-1712567890-A8F2BC1D")
    private String transactionReference;

    @Schema(description = "Client provided idempotency tracking key")
    private String idempotencyKey;

    @Schema(description = "Originating account number", example = "100000000001")
    private String sourceAccountNumber;

    @Schema(description = "Beneficiary destination account number", example = "100000000002")
    private String destinationAccountNumber;

    @Schema(description = "Transaction execution amount", example = "8000.00")
    private BigDecimal amount;

    @Schema(description = "Currency of the transfer", example = "INR")
    private String currency;

    @Schema(description = "Ledger transaction status", example = "COMPLETED")
    private TransactionStatus status;

    @Schema(description = "Timestamp when transaction was completed")
    private Instant timestamp;

    @Schema(description = "Transaction memo or description")
    private String description;

    @Schema(description = "Remaining available balance in source account after transfer (formatted for user view)")
    private BigDecimal sourceRemainingBalance;

    public TransferReceiptResponse() {
    }

    public TransferReceiptResponse(UUID transactionId, String transactionReference, String idempotencyKey,
                                   String sourceAccountNumber, String destinationAccountNumber, BigDecimal amount,
                                   String currency, TransactionStatus status, Instant timestamp,
                                   String description, BigDecimal sourceRemainingBalance) {
        this.transactionId = transactionId;
        this.transactionReference = transactionReference;
        this.idempotencyKey = idempotencyKey;
        this.sourceAccountNumber = sourceAccountNumber;
        this.destinationAccountNumber = destinationAccountNumber;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.timestamp = timestamp;
        this.description = description;
        this.sourceRemainingBalance = sourceRemainingBalance;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getSourceAccountNumber() {
        return sourceAccountNumber;
    }

    public void setSourceAccountNumber(String sourceAccountNumber) {
        this.sourceAccountNumber = sourceAccountNumber;
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

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getSourceRemainingBalance() {
        return sourceRemainingBalance;
    }

    public void setSourceRemainingBalance(BigDecimal sourceRemainingBalance) {
        this.sourceRemainingBalance = sourceRemainingBalance;
    }
}
