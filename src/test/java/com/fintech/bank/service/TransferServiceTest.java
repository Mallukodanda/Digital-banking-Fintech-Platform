package com.fintech.bank.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.bank.dto.TransactionHistoryResponse;
import com.fintech.bank.dto.TransferReceiptResponse;
import com.fintech.bank.dto.TransferRequest;
import com.fintech.bank.entity.*;
import com.fintech.bank.exception.BusinessRuleViolationException;
import com.fintech.bank.exception.InsufficientFundsException;
import com.fintech.bank.exception.UnauthorizedException;
import com.fintech.bank.repository.AccountRepository;
import com.fintech.bank.repository.JournalEntryRepository;
import com.fintech.bank.repository.OutboxEventRepository;
import com.fintech.bank.repository.TransactionLegRepository;
import com.fintech.bank.service.impl.TransferServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransferService Unit Test Suite")
@SuppressWarnings("null")
class TransferServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private JournalEntryRepository journalEntryRepository;

    @Mock
    private TransactionLegRepository transactionLegRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TransferServiceImpl transferService;

    private UserAuth sourceUser;
    private CustomerProfile sourceProfile;
    private Account sourceAccount;

    private UserAuth destUser;
    private CustomerProfile destProfile;
    private Account destAccount;

    private final UUID sourceAccountId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID destAccountId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private final String sourceAccNum = "100000000001";
    private final String destAccNum = "100000000002";
    private final String idempotencyKey = "idemp-key-12345";

    @BeforeEach
    void setUp() {
        sourceUser = new UserAuth("source@bank.com", "hash", UserRole.ROLE_CUSTOMER);
        sourceUser.setId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        sourceProfile = new CustomerProfile(sourceUser, "Alice", "Smith", "+10000000001");
        sourceAccount = new Account(sourceProfile, sourceAccNum, AccountType.CHECKING, "INR");
        sourceAccount.setId(sourceAccountId);
        sourceAccount.setCachedBalance(new BigDecimal("10000.0000"));

        destUser = new UserAuth("dest@bank.com", "hash", UserRole.ROLE_CUSTOMER);
        destUser.setId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
        destProfile = new CustomerProfile(destUser, "Bob", "Jones", "+10000000002");
        destAccount = new Account(destProfile, destAccNum, AccountType.CHECKING, "INR");
        destAccount.setId(destAccountId);
        destAccount.setCachedBalance(new BigDecimal("5000.0000"));

        lenient().when(idempotencyService.validateOrAcquireLock(any(), any(), any(), any()))
                .thenReturn(com.fintech.bank.dto.IdempotencyValidationResult.freshLock(
                        new IdempotencyRecord(idempotencyKey, "/api/v1/transfers", "hash", Instant.now().plusSeconds(3600))
                ));
    }

    @Nested
    @DisplayName("Funds Transfer Execution")
    class TransferExecutionTests {

        @Test
        @DisplayName("Should execute valid transfer between two active accounts successfully")
        void shouldExecuteTransferSuccessfully() {
            TransferRequest request = new TransferRequest(sourceAccountId, destAccNum, new BigDecimal("8000.00"), "INR", "Test payment");

            when(journalEntryRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
            when(accountRepository.findById(sourceAccountId)).thenReturn(Optional.of(sourceAccount));
            when(accountRepository.findByAccountNumber(destAccNum)).thenReturn(Optional.of(destAccount));

            // Locking returns accounts in sorted order (sourceAccountId < destAccountId)
            when(accountRepository.findByIdWithLock(sourceAccountId)).thenReturn(Optional.of(sourceAccount));
            when(accountRepository.findByIdWithLock(destAccountId)).thenReturn(Optional.of(destAccount));

            when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(invocation -> {
                JournalEntry j = invocation.getArgument(0);
                j.setId(UUID.randomUUID());
                return j;
            });

            TransferReceiptResponse receipt = transferService.transferFunds(request, idempotencyKey, sourceUser.getId(), false);

            assertThat(receipt).isNotNull();
            assertThat(receipt.getSourceAccountNumber()).isEqualTo(sourceAccNum);
            assertThat(receipt.getDestinationAccountNumber()).isEqualTo(destAccNum);
            assertThat(receipt.getAmount()).isEqualByComparingTo(new BigDecimal("8000.00"));
            assertThat(receipt.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
            assertThat(receipt.getSourceRemainingBalance()).isEqualByComparingTo(new BigDecimal("2000.00"));

            // Verify account balance updates
            assertThat(sourceAccount.getCachedBalance()).isEqualByComparingTo(new BigDecimal("2000.0000"));
            assertThat(destAccount.getCachedBalance()).isEqualByComparingTo(new BigDecimal("13000.0000"));

            verify(accountRepository, times(2)).save(any(Account.class));
            verify(journalEntryRepository, times(1)).save(any(JournalEntry.class));
            verify(outboxEventRepository, times(1)).save(any(OutboxEvent.class));
            verify(auditService, times(1)).recordAction(eq(sourceUser.getId()), eq("CUSTOMER"), eq("TRANSFER_EXECUTED"), eq("JOURNAL_ENTRY"), any(), isNull(), isNull());
        }

        @Test
        @DisplayName("Should throw InsufficientFundsException when balance is less than transfer amount")
        void shouldThrowInsufficientFundsException() {
            TransferRequest request = new TransferRequest(sourceAccountId, destAccNum, new BigDecimal("15000.00"), "INR", "Overdraft attempt");

            when(journalEntryRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
            when(accountRepository.findById(sourceAccountId)).thenReturn(Optional.of(sourceAccount));
            when(accountRepository.findByAccountNumber(destAccNum)).thenReturn(Optional.of(destAccount));
            when(accountRepository.findByIdWithLock(sourceAccountId)).thenReturn(Optional.of(sourceAccount));
            when(accountRepository.findByIdWithLock(destAccountId)).thenReturn(Optional.of(destAccount));

            assertThatThrownBy(() -> transferService.transferFunds(request, idempotencyKey, sourceUser.getId(), false))
                    .isInstanceOf(InsufficientFundsException.class)
                    .hasMessageContaining("Insufficient available balance");

            // Balances remain untouched
            assertThat(sourceAccount.getCachedBalance()).isEqualByComparingTo(new BigDecimal("10000.0000"));
            assertThat(destAccount.getCachedBalance()).isEqualByComparingTo(new BigDecimal("5000.0000"));
            verify(journalEntryRepository, never()).save(any(JournalEntry.class));
        }

        @Test
        @DisplayName("Should reject transfer when source account is FROZEN")
        void shouldRejectWhenSourceAccountFrozen() {
            sourceAccount.setStatus(AccountStatus.FROZEN);
            TransferRequest request = new TransferRequest(sourceAccountId, destAccNum, new BigDecimal("1000.00"), "INR", "Test");

            when(journalEntryRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
            when(accountRepository.findById(sourceAccountId)).thenReturn(Optional.of(sourceAccount));
            when(accountRepository.findByAccountNumber(destAccNum)).thenReturn(Optional.of(destAccount));
            when(accountRepository.findByIdWithLock(sourceAccountId)).thenReturn(Optional.of(sourceAccount));
            when(accountRepository.findByIdWithLock(destAccountId)).thenReturn(Optional.of(destAccount));

            assertThatThrownBy(() -> transferService.transferFunds(request, idempotencyKey, sourceUser.getId(), false))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("FROZEN");
        }

        @Test
        @DisplayName("Should reject transfer when destination account is CLOSED")
        void shouldRejectWhenDestinationAccountClosed() {
            destAccount.setStatus(AccountStatus.CLOSED);
            TransferRequest request = new TransferRequest(sourceAccountId, destAccNum, new BigDecimal("1000.00"), "INR", "Test");

            when(journalEntryRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
            when(accountRepository.findById(sourceAccountId)).thenReturn(Optional.of(sourceAccount));
            when(accountRepository.findByAccountNumber(destAccNum)).thenReturn(Optional.of(destAccount));
            when(accountRepository.findByIdWithLock(sourceAccountId)).thenReturn(Optional.of(sourceAccount));
            when(accountRepository.findByIdWithLock(destAccountId)).thenReturn(Optional.of(destAccount));

            assertThatThrownBy(() -> transferService.transferFunds(request, idempotencyKey, sourceUser.getId(), false))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("CLOSED");
        }

        @Test
        @DisplayName("Should reject self-transfer (same source and destination)")
        void shouldRejectSelfTransfer() {
            TransferRequest request = new TransferRequest(sourceAccountId, sourceAccNum, new BigDecimal("1000.00"), "INR", "Self");

            when(journalEntryRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
            when(accountRepository.findById(sourceAccountId)).thenReturn(Optional.of(sourceAccount));
            when(accountRepository.findByAccountNumber(sourceAccNum)).thenReturn(Optional.of(sourceAccount));

            assertThatThrownBy(() -> transferService.transferFunds(request, idempotencyKey, sourceUser.getId(), false))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("identical");
        }

        @Test
        @DisplayName("Should return previously completed receipt on idempotent replay")
        void shouldReturnExistingReceiptOnIdempotentReplay() {
            TransferRequest request = new TransferRequest(sourceAccountId, destAccNum, new BigDecimal("8000.00"), "INR", "Duplicate submit");

            JournalEntry existingJournal = new JournalEntry("TXN-EXISTING-123", idempotencyKey, TransactionType.P2P_TRANSFER, new BigDecimal("8000.0000"), "INR", "Duplicate submit");
            existingJournal.setId(UUID.randomUUID());
            existingJournal.setStatus(TransactionStatus.COMPLETED);
            existingJournal.setCompletedAt(Instant.now());

            TransactionLeg debit = new TransactionLeg(existingJournal, sourceAccount, LegType.DEBIT, new BigDecimal("8000.0000"), "INR", new BigDecimal("2000.0000"));
            TransactionLeg credit = new TransactionLeg(existingJournal, destAccount, LegType.CREDIT, new BigDecimal("8000.0000"), "INR", new BigDecimal("13000.0000"));
            existingJournal.addLeg(debit);
            existingJournal.addLeg(credit);

            when(journalEntryRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existingJournal));

            TransferReceiptResponse receipt = transferService.transferFunds(request, idempotencyKey, sourceUser.getId(), false);

            assertThat(receipt.getTransactionReference()).isEqualTo("TXN-EXISTING-123");
            assertThat(receipt.getIdempotencyKey()).isEqualTo(idempotencyKey);
            verify(accountRepository, never()).save(any(Account.class));
            verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
        }

        @Test
        @DisplayName("Should prevent unauthorized customer from initiating transfer from someone else's account")
        void shouldRejectUnauthorizedCustomerTransfer() {
            UUID intruderId = UUID.randomUUID();
            TransferRequest request = new TransferRequest(sourceAccountId, destAccNum, new BigDecimal("1000.00"), "INR", "Hack");

            when(journalEntryRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
            when(accountRepository.findById(sourceAccountId)).thenReturn(Optional.of(sourceAccount));

            assertThatThrownBy(() -> transferService.transferFunds(request, idempotencyKey, intruderId, false))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("not authorized");
        }
    }

    @Nested
    @DisplayName("Transaction History Statement Queries")
    class HistoryQueryTests {

        @Test
        @DisplayName("Should retrieve paged transaction history for valid account")
        void shouldRetrieveTransactionHistory() {
            JournalEntry entry = new JournalEntry("TXN-REF-1", "idemp-1", TransactionType.P2P_TRANSFER, new BigDecimal("500.0000"), "INR", "Dinner");
            entry.setStatus(TransactionStatus.COMPLETED);

            TransactionLeg leg = new TransactionLeg(entry, sourceAccount, LegType.DEBIT, new BigDecimal("500.0000"), "INR", new BigDecimal("9500.0000"));
            leg.setId(UUID.randomUUID());

            Pageable pageable = PageRequest.of(0, 10);
            when(accountRepository.existsById(sourceAccountId)).thenReturn(true);
            when(transactionLegRepository.findByAccountIdWithJournalEntry(sourceAccountId, pageable))
                    .thenReturn(new PageImpl<>(List.of(leg), pageable, 1));

            Page<TransactionHistoryResponse> historyPage = transferService.getAccountTransactionHistory(sourceAccountId, pageable);

            assertThat(historyPage.getContent()).hasSize(1);
            TransactionHistoryResponse line = historyPage.getContent().get(0);
            assertThat(line.getTransactionReference()).isEqualTo("TXN-REF-1");
            assertThat(line.getLegType()).isEqualTo(LegType.DEBIT);
            assertThat(line.getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(line.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("9500.00"));
        }
    }
}
