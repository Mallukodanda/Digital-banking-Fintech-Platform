package com.fintech.bank.service;

import com.fintech.bank.dto.BeneficiaryResponse;
import com.fintech.bank.dto.CreateBeneficiaryRequest;
import com.fintech.bank.dto.VerifyBeneficiaryResponse;
import com.fintech.bank.entity.*;
import com.fintech.bank.exception.BusinessRuleViolationException;
import com.fintech.bank.exception.DuplicateResourceException;
import com.fintech.bank.exception.UnauthorizedException;
import com.fintech.bank.repository.AccountRepository;
import com.fintech.bank.repository.BeneficiaryRepository;
import com.fintech.bank.repository.CustomerProfileRepository;
import com.fintech.bank.service.impl.BeneficiaryServiceImpl;
import com.fintech.bank.util.AccountNumberGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BeneficiaryService Unit Tests")
@SuppressWarnings("null")
class BeneficiaryServiceTest {

    @Mock
    private BeneficiaryRepository beneficiaryRepository;

    @Mock
    private CustomerProfileRepository customerProfileRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private BeneficiaryServiceImpl beneficiaryService;

    private UserAuth senderUser;
    private CustomerProfile senderProfile;

    private UserAuth recipientUser;
    private CustomerProfile recipientProfile;
    private Account destinationAccount;
    private String validDestinationAccNumber;

    @BeforeEach
    void setUp() {
        senderUser = new UserAuth("sender@bank.com", "hash", UserRole.ROLE_CUSTOMER);
        senderUser.setId(UUID.randomUUID());

        senderProfile = new CustomerProfile(senderUser, "Sender", "User", "+14155551111");
        senderProfile.setId(UUID.randomUUID());

        recipientUser = new UserAuth("recipient@bank.com", "hash", UserRole.ROLE_CUSTOMER);
        recipientUser.setId(UUID.randomUUID());

        recipientProfile = new CustomerProfile(recipientUser, "Alice", "Smith", "+14155552222");
        recipientProfile.setId(UUID.randomUUID());

        // Generate a valid 10-digit Luhn account number
        validDestinationAccNumber = AccountNumberGenerator.generateAccountNumber();

        destinationAccount = new Account(recipientProfile, validDestinationAccNumber, AccountType.CHECKING, "USD");
        destinationAccount.setId(UUID.randomUUID());
        destinationAccount.setStatus(AccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should successfully add a valid counterparty beneficiary")
    void testAddBeneficiarySuccess() {
        CreateBeneficiaryRequest request = new CreateBeneficiaryRequest("Alice Smith", validDestinationAccNumber, "Alice Work");

        when(customerProfileRepository.findByUserAuthId(senderUser.getId())).thenReturn(Optional.of(senderProfile));
        when(accountRepository.findByAccountNumber(validDestinationAccNumber)).thenReturn(Optional.of(destinationAccount));
        when(beneficiaryRepository.existsActiveByCustomerIdAndAccountNumber(senderProfile.getId(), validDestinationAccNumber)).thenReturn(false);

        Beneficiary savedBeneficiary = new Beneficiary(senderProfile, "Alice Smith", validDestinationAccNumber, "Alice Work");
        savedBeneficiary.setId(UUID.randomUUID());
        when(beneficiaryRepository.save(any(Beneficiary.class))).thenReturn(savedBeneficiary);

        BeneficiaryResponse response = beneficiaryService.addBeneficiary(senderUser.getId(), request);

        assertNotNull(response);
        assertEquals("Alice Smith", response.beneficiaryName());
        assertEquals(validDestinationAccNumber, response.destinationAccountNumber());
        assertEquals(BeneficiaryStatus.ACTIVE, response.status());

        verify(beneficiaryRepository, times(1)).save(any(Beneficiary.class));
    }

    @Test
    @DisplayName("Should reject self-addition when destination account belongs to the sender")
    void testRejectSelfBeneficiaryAddition() {
        Account senderAccount = new Account(senderProfile, validDestinationAccNumber, AccountType.CHECKING, "USD");
        CreateBeneficiaryRequest request = new CreateBeneficiaryRequest("Self", validDestinationAccNumber, "My Other Account");

        when(customerProfileRepository.findByUserAuthId(senderUser.getId())).thenReturn(Optional.of(senderProfile));
        when(accountRepository.findByAccountNumber(validDestinationAccNumber)).thenReturn(Optional.of(senderAccount));

        BusinessRuleViolationException exception = assertThrows(
                BusinessRuleViolationException.class,
                () -> beneficiaryService.addBeneficiary(senderUser.getId(), request)
        );

        assertEquals("BENEFICIARY_CANNOT_BE_SELF", exception.getErrorCode());
        verify(beneficiaryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject beneficiary addition when destination account is FROZEN")
    void testRejectFrozenDestinationAccount() {
        destinationAccount.setStatus(AccountStatus.FROZEN);
        CreateBeneficiaryRequest request = new CreateBeneficiaryRequest("Alice Smith", validDestinationAccNumber, "Alice");

        when(customerProfileRepository.findByUserAuthId(senderUser.getId())).thenReturn(Optional.of(senderProfile));
        when(accountRepository.findByAccountNumber(validDestinationAccNumber)).thenReturn(Optional.of(destinationAccount));

        BusinessRuleViolationException exception = assertThrows(
                BusinessRuleViolationException.class,
                () -> beneficiaryService.addBeneficiary(senderUser.getId(), request)
        );

        assertEquals("BENEFICIARY_ACCOUNT_NOT_ACTIVE", exception.getErrorCode());
        verify(beneficiaryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject duplicate active beneficiary addition")
    void testRejectDuplicateBeneficiary() {
        CreateBeneficiaryRequest request = new CreateBeneficiaryRequest("Alice Smith", validDestinationAccNumber, "Alice");

        when(customerProfileRepository.findByUserAuthId(senderUser.getId())).thenReturn(Optional.of(senderProfile));
        when(accountRepository.findByAccountNumber(validDestinationAccNumber)).thenReturn(Optional.of(destinationAccount));
        when(beneficiaryRepository.existsActiveByCustomerIdAndAccountNumber(senderProfile.getId(), validDestinationAccNumber)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> beneficiaryService.addBeneficiary(senderUser.getId(), request));
        verify(beneficiaryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should verify beneficiary and return masked recipient full name")
    void testVerifyBeneficiary() {
        when(customerProfileRepository.findByUserAuthId(senderUser.getId())).thenReturn(Optional.of(senderProfile));
        when(accountRepository.findByAccountNumber(validDestinationAccNumber)).thenReturn(Optional.of(destinationAccount));

        VerifyBeneficiaryResponse response = beneficiaryService.verifyBeneficiary(senderUser.getId(), validDestinationAccNumber);

        assertNotNull(response);
        assertEquals(validDestinationAccNumber, response.accountNumber());
        assertTrue(response.eligibleForTransfers());
        // "Alice Smith" masked -> "A***e S***h"
        assertEquals("A***e S***h", response.maskedAccountHolderName());
    }

    @Test
    @DisplayName("Should successfully soft-delete beneficiary owned by user")
    void testRemoveBeneficiarySuccess() {
        UUID beneficiaryId = UUID.randomUUID();
        Beneficiary beneficiary = new Beneficiary(senderProfile, "Alice Smith", validDestinationAccNumber, "Alice");
        beneficiary.setId(beneficiaryId);

        when(beneficiaryRepository.findById(beneficiaryId)).thenReturn(Optional.of(beneficiary));

        beneficiaryService.removeBeneficiary(senderUser.getId(), beneficiaryId);

        assertEquals(BeneficiaryStatus.DELETED, beneficiary.getStatus());
        verify(beneficiaryRepository, times(1)).save(beneficiary);
    }

    @Test
    @DisplayName("Should reject removing beneficiary belonging to another customer")
    void testRemoveBeneficiaryUnauthorized() {
        UUID beneficiaryId = UUID.randomUUID();
        Beneficiary otherBeneficiary = new Beneficiary(recipientProfile, "Someone", validDestinationAccNumber, "Tag");
        otherBeneficiary.setId(beneficiaryId);

        when(beneficiaryRepository.findById(beneficiaryId)).thenReturn(Optional.of(otherBeneficiary));

        assertThrows(UnauthorizedException.class, () ->
                beneficiaryService.removeBeneficiary(senderUser.getId(), beneficiaryId)
        );

        verify(beneficiaryRepository, never()).save(any());
    }
}
