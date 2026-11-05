package com.fintech.bank.entity;

import com.fintech.bank.util.MoneyUtils;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Immutable Transaction Leg representing a single Debit or Credit movement.
 */
@Entity
@Table(name = "transaction_legs")
public class TransactionLeg extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id", nullable = false)
    private JournalEntry journalEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "leg_type", nullable = false, length = 10)
    private LegType legType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    public TransactionLeg() {
    }

    public TransactionLeg(JournalEntry journalEntry, Account account, LegType legType, BigDecimal amount, String currency, BigDecimal balanceAfter) {
        this.journalEntry = journalEntry;
        this.account = account;
        this.legType = legType;
        this.amount = MoneyUtils.formatInternal(amount);
        this.currency = currency != null ? currency.toUpperCase() : "USD";
        this.balanceAfter = MoneyUtils.formatInternal(balanceAfter);
    }

    public JournalEntry getJournalEntry() {
        return journalEntry;
    }

    public void setJournalEntry(JournalEntry journalEntry) {
        this.journalEntry = journalEntry;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
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
        this.amount = MoneyUtils.formatInternal(amount);
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
        this.balanceAfter = MoneyUtils.formatInternal(balanceAfter);
    }
}
