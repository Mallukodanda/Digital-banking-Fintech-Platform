package com.fintech.bank.service;

import com.fintech.bank.dto.BeneficiaryResponse;
import com.fintech.bank.dto.CreateBeneficiaryRequest;
import com.fintech.bank.dto.VerifyBeneficiaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Beneficiary Management Service Interface.
 */
public interface BeneficiaryService {

    BeneficiaryResponse addBeneficiary(UUID userId, CreateBeneficiaryRequest request);

    List<BeneficiaryResponse> getBeneficiariesByUserId(UUID userId);

    Page<BeneficiaryResponse> getBeneficiariesByUserId(UUID userId, Pageable pageable);

    VerifyBeneficiaryResponse verifyBeneficiary(UUID userId, String accountNumber);

    void removeBeneficiary(UUID userId, UUID beneficiaryId);
}
