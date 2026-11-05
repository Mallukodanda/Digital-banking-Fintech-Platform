package com.fintech.bank.dto;

import com.fintech.bank.entity.AccountStatus;
import com.fintech.bank.entity.AccountType;

/**
 * Pre-transfer verification payload confirming account validity and recipient name.
 */
public record VerifyBeneficiaryResponse(
        String accountNumber,
        String maskedAccountHolderName,
        AccountType accountType,
        String currency,
        AccountStatus status,
        boolean eligibleForTransfers
) {}
