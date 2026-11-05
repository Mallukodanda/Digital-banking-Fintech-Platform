package com.fintech.bank.service.impl;

import com.fintech.bank.dto.*;
import com.fintech.bank.entity.*;
import com.fintech.bank.exception.BusinessRuleViolationException;
import com.fintech.bank.exception.ResourceNotFoundException;
import com.fintech.bank.repository.AccountRepository;
import com.fintech.bank.repository.CustomerProfileRepository;
import com.fintech.bank.service.AccountService;
import com.fintech.bank.service.AuditService;
import com.fintech.bank.util.AccountNumberGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Production implementation of AccountService enforcing account creation,
 * balance inquiries, account state machine rules, and audit logging.
 */
@Service
public class AccountServiceImpl implements AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);

    private final AccountRepository accountRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final AuditService auditService;

    public AccountServiceImpl(AccountRepository accountRepository,
                              CustomerProfileRepository customerProfileRepository,
                              AuditService auditService) {
        this.accountRepository = accountRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public AccountResponse createAccount(UUID userId, CreateAccountRequest request) {
        CustomerProfile customerProfile = customerProfileRepository.findByUserAuthId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("CustomerProfile", "userId", userId));

        // Generate unique 10-digit Luhn account number with collision protection
        String accountNumber;
        int attempts = 0;
        do {
            accountNumber = AccountNumberGenerator.generateAccountNumber();
            attempts++;
            if (attempts > 10) {
                throw new BusinessRuleViolationException("Unable to generate unique account number. Please retry.", "ACC_NUMBER_GENERATION_FAILED");
            }
        } while (accountRepository.existsByAccountNumber(accountNumber));

        Account account = new Account(
                customerProfile,
                accountNumber,
                request.accountType(),
                request.currency()
        );

        Account savedAccount = accountRepository.save(account);
        log.info("New deposit account opened [accountId: {}, accountNumber: {}, customerId: {}]",
                savedAccount.getId(), savedAccount.getAccountNumber(), customerProfile.getId());

        return AccountResponse.fromEntity(savedAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(UUID accountId) {
        Account account = accountRepository.findById(java.util.Objects.requireNonNull(accountId, "accountId cannot be null"))
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId));
        return AccountResponse.fromEntity(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(java.util.Objects.requireNonNull(accountNumber, "accountNumber cannot be null"))
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", accountNumber));
        return AccountResponse.fromEntity(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByUserId(UUID userId) {
        return accountRepository.findAllByUserId(java.util.Objects.requireNonNull(userId, "userId cannot be null")).stream()
                .map(AccountResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AccountResponse> getAccountsByUserId(UUID userId, Pageable pageable) {
        return accountRepository.findAllByUserId(
                java.util.Objects.requireNonNull(userId, "userId cannot be null"),
                java.util.Objects.requireNonNull(pageable, "pageable cannot be null")
        ).map(AccountResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountBalanceResponse getAccountBalance(UUID accountId) {
        Account account = accountRepository.findById(java.util.Objects.requireNonNull(accountId, "accountId cannot be null"))
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId));

        return new AccountBalanceResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getCurrency(),
                account.getAvailableBalance(),
                account.getCachedBalance(),
                account.getHoldBalance(),
                Instant.now()
        );
    }

    @Override
    @Transactional
    public AccountResponse updateAccountStatus(UUID accountId,
                                               AccountStatusUpdateRequest request,
                                               UUID actorId,
                                               String actorRole,
                                               String correlationId,
                                               String clientIp) {
        Account account = accountRepository.findById(java.util.Objects.requireNonNull(accountId, "accountId cannot be null"))
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId));

        AccountStatus currentStatus = account.getStatus();
        AccountStatus targetStatus = request.status();

        if (currentStatus == targetStatus) {
            return AccountResponse.fromEntity(account);
        }

        // Financial Invariant: Cannot close an account with non-zero balance
        if (targetStatus == AccountStatus.CLOSED) {
            if (account.getCachedBalance().compareTo(BigDecimal.ZERO) != 0) {
                log.warn("Account closure rejected: non-zero balance [{}] on account [{}]",
                        account.getCachedBalance(), account.getAccountNumber());
                throw new BusinessRuleViolationException(
                        "Cannot close an account with a non-zero balance. Current balance: " + account.getCachedBalance(),
                        "ACC_NON_ZERO_BALANCE_ON_CLOSE"
                );
            }
        }

        account.setStatus(targetStatus);
        Account updatedAccount = accountRepository.save(account);

        // Record Audit Trail
        String actionName = "ACCOUNT_STATUS_CHANGE_" + currentStatus + "_TO_" + targetStatus;
        auditService.recordAction(
                actorId,
                actorRole,
                actionName,
                "ACCOUNT",
                accountId.toString(),
                correlationId,
                clientIp
        );

        log.info("Account status updated [accountId: {}, from: {}, to: {}, reason: '{}', actorId: {}]",
                accountId, currentStatus, targetStatus, request.reason(), actorId);

        return AccountResponse.fromEntity(updatedAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AccountResponse> getAllAccountsForAdmin(Pageable pageable) {
        return accountRepository.findAll(java.util.Objects.requireNonNull(pageable, "pageable cannot be null"))
                .map(AccountResponse::fromEntity);
    }
}
