package com.fintech.bank.service;

import com.fintech.bank.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Account Management Service Interface.
 */
public interface AccountService {

    AccountResponse createAccount(UUID userId, CreateAccountRequest request);

    AccountResponse getAccountById(UUID accountId);

    AccountResponse getAccountByNumber(String accountNumber);

    List<AccountResponse> getAccountsByUserId(UUID userId);

    Page<AccountResponse> getAccountsByUserId(UUID userId, Pageable pageable);

    AccountBalanceResponse getAccountBalance(UUID accountId);

    AccountResponse updateAccountStatus(UUID accountId, AccountStatusUpdateRequest request, UUID actorId, String actorRole, String correlationId, String clientIp);

    Page<AccountResponse> getAllAccountsForAdmin(Pageable pageable);
}
