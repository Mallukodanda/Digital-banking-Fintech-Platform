package com.fintech.bank.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Real-time Account Balance Response DTO.
 */
public record AccountBalanceResponse(
        UUID accountId,
        String accountNumber,
        String currency,
        BigDecimal availableBalance,
        BigDecimal cachedBalance,
        BigDecimal holdBalance,
        Instant timestamp
) {}
