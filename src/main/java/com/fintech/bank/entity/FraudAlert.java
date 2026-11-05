package com.fintech.bank.entity;

import jakarta.persistence.*;

/**
 * FraudAlert entity mapping fraud_alerts table.
 */
@Entity
@Table(name = "fraud_alerts")
public class FraudAlert extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id")
    private JournalEntry journalEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "rule_type", nullable = false, length = 50)
    private String ruleType;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private FraudStatus status = FraudStatus.FLAGGED;

    @Column(name = "reviewer_notes", columnDefinition = "TEXT")
    private String reviewerNotes;

    public FraudAlert() {
    }

    public FraudAlert(JournalEntry journalEntry, Account account, String ruleType, int riskScore, String reviewerNotes) {
        this.journalEntry = journalEntry;
        this.account = account;
        this.ruleType = ruleType;
        this.riskScore = Math.min(100, Math.max(0, riskScore));
        this.status = FraudStatus.FLAGGED;
        this.reviewerNotes = reviewerNotes;
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

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
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
