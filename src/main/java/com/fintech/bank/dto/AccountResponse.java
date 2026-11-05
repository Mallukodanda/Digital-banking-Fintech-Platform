package com.fintech.bank.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fintech.bank.entity.Account;
import com.fintech.bank.entity.AccountStatus;
import com.fintech.bank.entity.AccountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Detailed Account Response DTO.
 */
public record AccountResponse(
        UUID accountId,
        UUID customerId,
        String accountNumber,
        AccountType accountType,
        String currency,
        AccountStatus status,
        BigDecimal cachedBalance,
        BigDecimal availableBalance,
        BigDecimal holdBalance,
        BigDecimal overdraftLimit,
        Instant createdAt,
        Instant updatedAt
) {
    @JsonProperty("id")
    public UUID id() {
        return accountId;
    }

    @JsonProperty("balance")
    public BigDecimal balance() {
        return availableBalance != null ? availableBalance : cachedBalance;
    }

    public static AccountResponse fromEntity(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getCustomerProfile().getId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getCurrency(),
                account.getStatus(),
                account.getCachedBalance(),
                account.getAvailableBalance(),
                account.getHoldBalance(),
                account.getOverdraftLimit(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}
