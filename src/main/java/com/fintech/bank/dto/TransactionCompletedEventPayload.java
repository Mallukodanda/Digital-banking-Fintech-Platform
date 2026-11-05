package com.fintech.bank.dto;

import com.fintech.bank.entity.TransactionStatus;
import com.fintech.bank.entity.TransactionType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event Payload published upon successful completion of a financial transaction.
 */
public class TransactionCompletedEventPayload implements Serializable {

    private UUID transactionId;
    private String transactionReference;
    private String idempotencyKey;
    private TransactionType transactionType;
    private UUID sourceAccountId;
    private String sourceAccountNumber;
    private UUID destinationAccountId;
    private String destinationAccountNumber;
    private BigDecimal amount;
    private String currency;
    private TransactionStatus status;
    private String description;
    private Instant completedAt;

    public TransactionCompletedEventPayload() {
    }

    public TransactionCompletedEventPayload(UUID transactionId, String transactionReference, String idempotencyKey,
                                           TransactionType transactionType, UUID sourceAccountId, String sourceAccountNumber,
                                           UUID destinationAccountId, String destinationAccountNumber, BigDecimal amount,
                                           String currency, TransactionStatus status, String description, Instant completedAt) {
        this.transactionId = transactionId;
        this.transactionReference = transactionReference;
        this.idempotencyKey = idempotencyKey;
        this.transactionType = transactionType;
        this.sourceAccountId = sourceAccountId;
        this.sourceAccountNumber = sourceAccountNumber;
        this.destinationAccountId = destinationAccountId;
        this.destinationAccountNumber = destinationAccountNumber;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.description = description;
        this.completedAt = completedAt;
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

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public void setSourceAccountId(UUID sourceAccountId) {
        this.sourceAccountId = sourceAccountId;
    }

    public String getSourceAccountNumber() {
        return sourceAccountNumber;
    }

    public void setSourceAccountNumber(String sourceAccountNumber) {
        this.sourceAccountNumber = sourceAccountNumber;
    }

    public UUID getDestinationAccountId() {
        return destinationAccountId;
    }

    public void setDestinationAccountId(UUID destinationAccountId) {
        this.destinationAccountId = destinationAccountId;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
