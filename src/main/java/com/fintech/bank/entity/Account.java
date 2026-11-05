package com.fintech.bank.entity;

import com.fintech.bank.util.MoneyUtils;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Account Aggregate Root mapping to the accounts table.
 */
@Entity
@Table(name = "accounts")
public class Account extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerProfile customerProfile;

    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 30)
    private AccountType accountType = AccountType.CHECKING;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "cached_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal cachedBalance = BigDecimal.ZERO.setScale(MoneyUtils.INTERNAL_SCALE, MoneyUtils.FINANCIAL_ROUNDING);

    @Column(name = "hold_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal holdBalance = BigDecimal.ZERO.setScale(MoneyUtils.INTERNAL_SCALE, MoneyUtils.FINANCIAL_ROUNDING);

    @Column(name = "overdraft_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal overdraftLimit = BigDecimal.ZERO.setScale(MoneyUtils.INTERNAL_SCALE, MoneyUtils.FINANCIAL_ROUNDING);

    public Account() {
    }

    public Account(CustomerProfile customerProfile, String accountNumber, AccountType accountType, String currency) {
        this.customerProfile = customerProfile;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.currency = currency != null ? currency.toUpperCase() : "USD";
        this.status = AccountStatus.ACTIVE;
        this.cachedBalance = BigDecimal.ZERO.setScale(MoneyUtils.INTERNAL_SCALE, MoneyUtils.FINANCIAL_ROUNDING);
        this.holdBalance = BigDecimal.ZERO.setScale(MoneyUtils.INTERNAL_SCALE, MoneyUtils.FINANCIAL_ROUNDING);
        this.overdraftLimit = BigDecimal.ZERO.setScale(MoneyUtils.INTERNAL_SCALE, MoneyUtils.FINANCIAL_ROUNDING);
    }

    public CustomerProfile getCustomerProfile() {
        return customerProfile;
    }

    public void setCustomerProfile(CustomerProfile customerProfile) {
        this.customerProfile = customerProfile;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public BigDecimal getCachedBalance() {
        return cachedBalance;
    }

    public void setCachedBalance(BigDecimal cachedBalance) {
        this.cachedBalance = MoneyUtils.formatInternal(cachedBalance);
    }

    public BigDecimal getHoldBalance() {
        return holdBalance;
    }

    public void setHoldBalance(BigDecimal holdBalance) {
        this.holdBalance = MoneyUtils.formatInternal(holdBalance);
    }

    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }

    public void setOverdraftLimit(BigDecimal overdraftLimit) {
        this.overdraftLimit = MoneyUtils.formatInternal(overdraftLimit);
    }

    /**
     * Mathematical invariant: availableBalance = (cachedBalance - holdBalance) + overdraftLimit
     */
    public BigDecimal getAvailableBalance() {
        return cachedBalance.subtract(holdBalance).add(overdraftLimit);
    }

    public boolean isOperational() {
        return status == AccountStatus.ACTIVE;
    }
}
