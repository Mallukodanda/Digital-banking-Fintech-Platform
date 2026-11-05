package com.fintech.bank.entity;

import com.fintech.bank.util.MoneyUtils;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Master Financial Ledger Journal Entry mapping to journal_entries table.
 */
@Entity
@Table(name = "journal_entries")
public class JournalEntry extends BaseAuditableEntity {

    @Column(name = "transaction_reference", nullable = false, unique = true, length = 64)
    private String transactionReference;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private TransactionType transactionType = TransactionType.P2P_TRANSFER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "initiated_at", nullable = false)
    private Instant initiatedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TransactionLeg> legs = new ArrayList<>();

    public JournalEntry() {
    }

    public JournalEntry(String transactionReference, String idempotencyKey, TransactionType transactionType, BigDecimal totalAmount, String currency, String description) {
        this.transactionReference = transactionReference;
        this.idempotencyKey = idempotencyKey;
        this.transactionType = transactionType;
        this.totalAmount = MoneyUtils.formatInternal(totalAmount);
        this.currency = currency != null ? currency.toUpperCase() : "USD";
        this.description = description;
        this.status = TransactionStatus.PENDING;
        this.initiatedAt = Instant.now();
    }

    public void addLeg(TransactionLeg leg) {
        legs.add(leg);
        leg.setJournalEntry(this);
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

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = MoneyUtils.formatInternal(totalAmount);
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

    public Instant getInitiatedAt() {
        return initiatedAt;
    }

    public void setInitiatedAt(Instant initiatedAt) {
        this.initiatedAt = initiatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public List<TransactionLeg> getLegs() {
        return legs;
    }

    public void setLegs(List<TransactionLeg> legs) {
        this.legs = legs;
    }
}
