package com.fintech.bank.dto;

import com.fintech.bank.entity.Beneficiary;
import com.fintech.bank.entity.BeneficiaryStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Beneficiary response payload.
 */
public record BeneficiaryResponse(
        UUID beneficiaryId,
        UUID customerId,
        String beneficiaryName,
        String destinationAccountNumber,
        String nickname,
        BeneficiaryStatus status,
        Instant createdAt
) {
    public static BeneficiaryResponse fromEntity(Beneficiary beneficiary) {
        return new BeneficiaryResponse(
                beneficiary.getId(),
                beneficiary.getCustomerProfile().getId(),
                beneficiary.getBeneficiaryName(),
                beneficiary.getDestinationAccountNumber(),
                beneficiary.getNickname(),
                beneficiary.getStatus(),
                beneficiary.getCreatedAt()
        );
    }
}
