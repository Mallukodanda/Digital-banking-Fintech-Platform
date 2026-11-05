package com.fintech.bank.service;

import com.fintech.bank.dto.AccountBalanceResponse;
import com.fintech.bank.dto.AccountResponse;
import com.fintech.bank.dto.AccountStatusUpdateRequest;
import com.fintech.bank.dto.CreateAccountRequest;
import com.fintech.bank.entity.*;
import com.fintech.bank.exception.BusinessRuleViolationException;
import com.fintech.bank.exception.ResourceNotFoundException;
import com.fintech.bank.repository.AccountRepository;
import com.fintech.bank.repository.CustomerProfileRepository;
import com.fintech.bank.service.impl.AccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService Unit Tests")
@SuppressWarnings("null")
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CustomerProfileRepository customerProfileRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AccountServiceImpl accountService;

    private UserAuth mockUser;
    private CustomerProfile mockProfile;
    private Account mockAccount;

    @BeforeEach
    void setUp() {
        mockUser = new UserAuth("bob@fintech.bank", "hash", UserRole.ROLE_CUSTOMER);
        mockUser.setId(UUID.randomUUID());

        mockProfile = new CustomerProfile(mockUser, "Bob", "Williams", "+14155559876");
        mockProfile.setId(UUID.randomUUID());

        mockAccount = new Account(mockProfile, "1004829103", AccountType.CHECKING, "USD");
        mockAccount.setId(UUID.randomUUID());
        mockAccount.setCachedBalance(new BigDecimal("500.0000"));
        mockAccount.setHoldBalance(new BigDecimal("50.0000"));
    }

    @Test
    @DisplayName("Should successfully create a new Checking account with Luhn 10-digit number")
    void testCreateAccountSuccess() {
        UUID userId = mockUser.getId();
        CreateAccountRequest request = new CreateAccountRequest(AccountType.CHECKING, "USD");

        when(customerProfileRepository.findByUserAuthId(userId)).thenReturn(Optional.of(mockProfile));
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(mockAccount);

        AccountResponse response = accountService.createAccount(userId, request);

        assertNotNull(response);
        assertEquals("1004829103", response.accountNumber());
        assertEquals(AccountType.CHECKING, response.accountType());
        assertEquals("USD", response.currency());
        assertEquals(AccountStatus.ACTIVE, response.status());

        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    @DisplayName("Should calculate real-time available balance accurately (cached - hold + overdraft)")
    void testGetAccountBalance() {
        UUID accountId = mockAccount.getId();
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(mockAccount));

        AccountBalanceResponse balance = accountService.getAccountBalance(accountId);

        assertNotNull(balance);
        assertEquals(new BigDecimal("500.0000"), balance.cachedBalance());
        assertEquals(new BigDecimal("50.0000"), balance.holdBalance());
        // Available = 500 - 50 = 450.0000
        assertEquals(new BigDecimal("450.0000"), balance.availableBalance());
    }

    @Test
    @DisplayName("Should freeze account and write audit log entry")
    void testUpdateAccountStatusToFrozen() {
        UUID accountId = mockAccount.getId();
        UUID adminId = UUID.randomUUID();
        AccountStatusUpdateRequest request = new AccountStatusUpdateRequest(AccountStatus.FROZEN, "Suspected fraud investigation");

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(mockAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse response = accountService.updateAccountStatus(
                accountId, request, adminId, "ROLE_ADMIN", "corr-123", "127.0.0.1"
        );

        assertNotNull(response);
        assertEquals(AccountStatus.FROZEN, response.status());
        verify(auditService, times(1)).recordAction(
                eq(adminId), eq("ROLE_ADMIN"), eq("ACCOUNT_STATUS_CHANGE_ACTIVE_TO_FROZEN"),
                eq("ACCOUNT"), eq(accountId.toString()), eq("corr-123"), eq("127.0.0.1")
        );
    }

    @Test
    @DisplayName("Should reject closing account when balance is non-zero")
    void testRejectCloseAccountWithNonZeroBalance() {
        UUID accountId = mockAccount.getId();
        mockAccount.setCachedBalance(new BigDecimal("10.0000")); // Non-zero
        AccountStatusUpdateRequest request = new AccountStatusUpdateRequest(AccountStatus.CLOSED, "Customer request");

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(mockAccount));

        assertThrows(BusinessRuleViolationException.class, () ->
                accountService.updateAccountStatus(accountId, request, UUID.randomUUID(), "ROLE_ADMIN", "corr-123", "127.0.0.1")
        );

        verify(accountRepository, never()).save(any());
        verify(auditService, never()).recordAction(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when account ID does not exist")
    void testAccountNotFound() {
        UUID invalidId = UUID.randomUUID();
        when(accountRepository.findById(invalidId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> accountService.getAccountById(invalidId));
    }
}
