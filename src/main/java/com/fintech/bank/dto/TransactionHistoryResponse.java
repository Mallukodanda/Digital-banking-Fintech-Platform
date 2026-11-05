package com.fintech.bank.dto;

import com.fintech.bank.entity.LegType;
import com.fintech.bank.entity.TransactionStatus;
import com.fintech.bank.entity.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Historical transaction line item returned for account ledger statement queries.
 */
@Schema(description = "Transaction leg representation for customer account statement")
public class TransactionHistoryResponse {

    @Schema(description = "Transaction leg unique identifier")
    private UUID legId;

    @Schema(description = "Master journal transaction reference code")
    private String transactionReference;

    @Schema(description = "Transaction category", example = "P2P_TRANSFER")
    private TransactionType transactionType;

    @Schema(description = "Debit or Credit leg type", example = "DEBIT")
    private LegType legType;

    @Schema(description = "Transaction amount", example = "8000.00")
    private BigDecimal amount;

    @Schema(description = "Currency", example = "INR")
    private String currency;

    @Schema(description = "Account balance immediately following leg posting", example = "2000.00")
    private BigDecimal balanceAfter;

    @Schema(description = "Transaction description memo")
    private String description;

    @Schema(description = "Timestamp when the transaction was executed")
    private Instant timestamp;

    @Schema(description = "Transaction completion status", example = "COMPLETED")
    private TransactionStatus status;

    public TransactionHistoryResponse() {
    }

    public TransactionHistoryResponse(UUID legId, String transactionReference, TransactionType transactionType,
                                      LegType legType, BigDecimal amount, String currency,
                                      BigDecimal balanceAfter, String description, Instant timestamp,
                                      TransactionStatus status) {
        this.legId = legId;
        this.transactionReference = transactionReference;
        this.transactionType = transactionType;
        this.legType = legType;
        this.amount = amount;
        this.currency = currency;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.timestamp = timestamp;
        this.status = status;
    }

    public UUID getLegId() {
        return legId;
    }

    public void setLegId(UUID legId) {
        this.legId = legId;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public LegType getLegType() {
        return legType;
    }

    public void setLegType(LegType legType) {
        this.legType = legType;
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

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }
}
