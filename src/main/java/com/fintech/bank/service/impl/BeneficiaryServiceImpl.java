package com.fintech.bank.service.impl;

import com.fintech.bank.dto.BeneficiaryResponse;
import com.fintech.bank.dto.CreateBeneficiaryRequest;
import com.fintech.bank.dto.VerifyBeneficiaryResponse;
import com.fintech.bank.entity.*;
import com.fintech.bank.exception.BusinessRuleViolationException;
import com.fintech.bank.exception.DuplicateResourceException;
import com.fintech.bank.exception.ResourceNotFoundException;
import com.fintech.bank.exception.UnauthorizedException;
import com.fintech.bank.repository.AccountRepository;
import com.fintech.bank.repository.BeneficiaryRepository;
import com.fintech.bank.repository.CustomerProfileRepository;
import com.fintech.bank.service.BeneficiaryService;
import com.fintech.bank.util.AccountNumberGenerator;
import com.fintech.bank.config.CacheNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Production implementation of BeneficiaryService enforcing destination validation,
 * anti-self addition guards, duplicate checking, and account eligibility.
 */
@Service
public class BeneficiaryServiceImpl implements BeneficiaryService {

    private static final Logger log = LoggerFactory.getLogger(BeneficiaryServiceImpl.class);

    private final BeneficiaryRepository beneficiaryRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final AccountRepository accountRepository;

    public BeneficiaryServiceImpl(BeneficiaryRepository beneficiaryRepository,
                                  CustomerProfileRepository customerProfileRepository,
                                  AccountRepository accountRepository) {
        this.beneficiaryRepository = beneficiaryRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheNames.BENEFICIARIES, key = "#userId")
    public BeneficiaryResponse addBeneficiary(UUID userId, CreateBeneficiaryRequest request) {
        CustomerProfile customerProfile = customerProfileRepository.findByUserAuthId(Objects.requireNonNull(userId, "userId cannot be null"))
                .orElseThrow(() -> new ResourceNotFoundException("CustomerProfile", "userId", userId));

        String destAccountNumber = request.destinationAccountNumber().trim();

        // 1. Format & Luhn Check
        if (!AccountNumberGenerator.isValidAccountNumber(destAccountNumber)) {
            throw new BusinessRuleViolationException("Destination account number is invalid (Luhn check failed)", "BENEFICIARY_INVALID_ACCOUNT_FORMAT");
        }

        // 2. Destination Account Lookup
        Account destinationAccount = accountRepository.findByAccountNumber(destAccountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Destination Account", "accountNumber", destAccountNumber));

        // 3. Anti-Self Addition Guard
        if (destinationAccount.getCustomerProfile().getId().equals(customerProfile.getId())) {
            log.warn("Self-beneficiary addition rejected [userId: {}, accountNumber: {}]", userId, destAccountNumber);
            throw new BusinessRuleViolationException("You cannot add your own bank account as a beneficiary", "BENEFICIARY_CANNOT_BE_SELF");
        }

        // 4. Account Eligibility & Status Check
        if (destinationAccount.getStatus() != AccountStatus.ACTIVE) {
            log.warn("Beneficiary addition rejected: destination account is not active [accountNumber: {}, status: {}]",
                    destAccountNumber, destinationAccount.getStatus());
            throw new BusinessRuleViolationException("Destination account is " + destinationAccount.getStatus() + " and cannot receive transfers", "BENEFICIARY_ACCOUNT_NOT_ACTIVE");
        }

        // 5. Duplicate Guard
        if (beneficiaryRepository.existsActiveByCustomerIdAndAccountNumber(customerProfile.getId(), destAccountNumber)) {
            log.warn("Duplicate beneficiary rejected [customerId: {}, accountNumber: {}]", customerProfile.getId(), destAccountNumber);
            throw new DuplicateResourceException("Beneficiary", "destinationAccountNumber", destAccountNumber);
        }

        Beneficiary beneficiary = new Beneficiary(
                customerProfile,
                request.beneficiaryName().trim(),
                destAccountNumber,
                request.nickname() != null ? request.nickname().trim() : null
        );

        Beneficiary savedBeneficiary = beneficiaryRepository.save(beneficiary);
        log.info("Beneficiary successfully saved [beneficiaryId: {}, customerId: {}, destAcc: {}]",
                savedBeneficiary.getId(), customerProfile.getId(), destAccountNumber);

        return BeneficiaryResponse.fromEntity(savedBeneficiary);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.BENEFICIARIES, key = "#userId")
    public List<BeneficiaryResponse> getBeneficiariesByUserId(UUID userId) {
        return beneficiaryRepository.findAllByUserIdAndStatus(Objects.requireNonNull(userId, "userId cannot be null"), BeneficiaryStatus.ACTIVE)
                .stream()
                .map(BeneficiaryResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BeneficiaryResponse> getBeneficiariesByUserId(UUID userId, Pageable pageable) {
        return beneficiaryRepository.findAllByUserIdAndStatus(
                Objects.requireNonNull(userId, "userId cannot be null"),
                BeneficiaryStatus.ACTIVE,
                Objects.requireNonNull(pageable, "pageable cannot be null")
        ).map(BeneficiaryResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public VerifyBeneficiaryResponse verifyBeneficiary(UUID userId, String accountNumber) {
        CustomerProfile currentCustomer = customerProfileRepository.findByUserAuthId(Objects.requireNonNull(userId, "userId cannot be null"))
                .orElseThrow(() -> new ResourceNotFoundException("CustomerProfile", "userId", userId));

        String destAccountNumber = accountNumber.trim();

        if (!AccountNumberGenerator.isValidAccountNumber(destAccountNumber)) {
            throw new BusinessRuleViolationException("Account number is invalid format", "ACCOUNT_INVALID_FORMAT");
        }

        Account account = accountRepository.findByAccountNumber(destAccountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", destAccountNumber));

        boolean isSelf = account.getCustomerProfile().getId().equals(currentCustomer.getId());
        boolean isEligible = account.getStatus() == AccountStatus.ACTIVE && !isSelf;

        String recipientFullName = account.getCustomerProfile().getFullName();
        String maskedName = maskPersonName(recipientFullName);

        return new VerifyBeneficiaryResponse(
                account.getAccountNumber(),
                maskedName,
                account.getAccountType(),
                account.getCurrency(),
                account.getStatus(),
                isEligible
        );
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheNames.BENEFICIARIES, key = "#userId")
    public void removeBeneficiary(UUID userId, UUID beneficiaryId) {
        Beneficiary beneficiary = beneficiaryRepository.findById(Objects.requireNonNull(beneficiaryId, "beneficiaryId cannot be null"))
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary", "id", beneficiaryId));

        UUID ownerUserId = beneficiary.getCustomerProfile().getUserAuth().getId();
        if (!ownerUserId.equals(userId)) {
            log.warn("Unauthorized beneficiary removal attempt [userId: {}, beneficiaryId: {}]", userId, beneficiaryId);
            throw new UnauthorizedException("You are not authorized to delete this beneficiary", "AUTH_FORBIDDEN_ACCESS");
        }

        beneficiary.setStatus(BeneficiaryStatus.DELETED);
        beneficiaryRepository.save(beneficiary);
        log.info("Beneficiary removed [beneficiaryId: {}, userId: {}]", beneficiaryId, userId);
    }

    private String maskPersonName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "***";
        }
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.length() <= 2) {
                sb.append(part.charAt(0)).append("* ");
            } else {
                sb.append(part.charAt(0)).append("*".repeat(part.length() - 2)).append(part.charAt(part.length() - 1)).append(" ");
            }
        }
        return sb.toString().trim();
    }
}
