package com.fintech.bank.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.bank.dto.TransactionHistoryResponse;
import com.fintech.bank.dto.TransferReceiptResponse;
import com.fintech.bank.dto.TransferRequest;
import com.fintech.bank.entity.*;
import com.fintech.bank.exception.BusinessRuleViolationException;
import com.fintech.bank.exception.InsufficientFundsException;
import com.fintech.bank.exception.ResourceNotFoundException;
import com.fintech.bank.exception.UnauthorizedException;
import com.fintech.bank.repository.AccountRepository;
import com.fintech.bank.repository.JournalEntryRepository;
import com.fintech.bank.repository.OutboxEventRepository;
import com.fintech.bank.repository.TransactionLegRepository;
import com.fintech.bank.service.AuditService;
import com.fintech.bank.service.IdempotencyService;
import com.fintech.bank.service.TransferService;
import com.fintech.bank.util.MoneyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Production-grade Financial Transaction Engine.
 * Implements strict ACID guarantees, deadlock-free deterministic pessimistic row locking,
 * double-entry accounting ledger entries, idempotency enforcement, and Transactional Outbox.
 */
@Service
@SuppressWarnings("null")
public class TransferServiceImpl implements TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferServiceImpl.class);

    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final TransactionLegRepository transactionLegRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final AuditService auditService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public TransferServiceImpl(AccountRepository accountRepository,
                               JournalEntryRepository journalEntryRepository,
                               TransactionLegRepository transactionLegRepository,
                               OutboxEventRepository outboxEventRepository,
                               AuditService auditService,
                               IdempotencyService idempotencyService,
                               ObjectMapper objectMapper) {
        this.accountRepository = accountRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.transactionLegRepository = transactionLegRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.auditService = auditService;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes funds transfer within a strict READ_COMMITTED transaction boundary
     * with explicit PESSIMISTIC_WRITE row locks acquired in deterministic order.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public TransferReceiptResponse transferFunds(TransferRequest request, String idempotencyKey, UUID authenticatedUserId, boolean isAdmin) {
        log.info("Initiating transfer request: sourceAccountId={}, destAccountNumber={}, amount={}, idempotencyKey={}",
                request.getSourceAccountId(), request.getDestinationAccountNumber(), request.getAmount(), idempotencyKey);

        // 1. Idempotency Validation & Fingerprint Check
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            throw new BusinessRuleViolationException("Idempotency key header (X-Idempotency-Key) is strictly required");
        }

        String requestHash = com.fintech.bank.util.IdempotencyUtils.computeRequestFingerprint(
                "POST",
                "/api/v1/transfers",
                authenticatedUserId,
                request,
                objectMapper
        );

        com.fintech.bank.dto.IdempotencyValidationResult idempResult = idempotencyService.validateOrAcquireLock(
                idempotencyKey.trim(),
                "/api/v1/transfers",
                requestHash,
                java.time.Duration.ofHours(24)
        );

        if (idempResult.alreadyCompleted()) {
            log.info("Idempotent completed response replayed for key: {}", idempotencyKey);
            try {
                return objectMapper.readValue(idempResult.responseBody(), TransferReceiptResponse.class);
            } catch (Exception e) {
                log.warn("Failed to parse cached response body, falling back to Journal lookup for key: {}", idempotencyKey);
                Optional<JournalEntry> journalOpt = journalEntryRepository.findByIdempotencyKey(idempotencyKey.trim());
                if (journalOpt.isPresent()) {
                    return mapToReceipt(journalOpt.get());
                }
            }
        }

        // 2. Amount Validation
        BigDecimal transferAmount = MoneyUtils.formatInternal(request.getAmount());
        if (!MoneyUtils.isPositive(transferAmount)) {
            throw new BusinessRuleViolationException("Transfer amount must be strictly greater than 0");
        }

        // 3. Initial Lookups (Verify existence before locking)
        Account sourceAccountRef = accountRepository.findById(request.getSourceAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Source account not found with ID: " + request.getSourceAccountId()));

        // Authorization Check
        if (!isAdmin && authenticatedUserId != null) {
            UUID ownerId = sourceAccountRef.getCustomerProfile().getUserAuth().getId();
            if (!ownerId.equals(authenticatedUserId)) {
                throw new UnauthorizedException("You are not authorized to transfer funds from account: " + sourceAccountRef.getAccountNumber());
            }
        }

        Account destAccountRef = accountRepository.findByAccountNumber(request.getDestinationAccountNumber().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Destination account not found with number: " + request.getDestinationAccountNumber()));

        // 4. Same Account Validation (Self-transfer prevention)
        if (sourceAccountRef.getId().equals(destAccountRef.getId()) ||
                sourceAccountRef.getAccountNumber().equalsIgnoreCase(destAccountRef.getAccountNumber())) {
            throw new BusinessRuleViolationException("Source and destination accounts cannot be identical");
        }

        // 5. Deadlock Prevention Strategy: Deterministic Locking Order (Sort by UUID)
        UUID sourceId = sourceAccountRef.getId();
        UUID destId = destAccountRef.getId();
        UUID firstLockId = sourceId.compareTo(destId) < 0 ? sourceId : destId;
        UUID secondLockId = sourceId.compareTo(destId) < 0 ? destId : sourceId;

        log.debug("Acquiring pessimistic write locks in order: 1st={}, 2nd={}", firstLockId, secondLockId);

        Account lockedFirst = accountRepository.findByIdWithLock(firstLockId)
                .orElseThrow(() -> new ResourceNotFoundException("Account lock acquisition failed for ID: " + firstLockId));
        Account lockedSecond = accountRepository.findByIdWithLock(secondLockId)
                .orElseThrow(() -> new ResourceNotFoundException("Account lock acquisition failed for ID: " + secondLockId));

        Account lockedSource = lockedFirst.getId().equals(sourceId) ? lockedFirst : lockedSecond;
        Account lockedDest = lockedFirst.getId().equals(destId) ? lockedFirst : lockedSecond;

        // 6. Account Status Checks
        if (!lockedSource.isOperational()) {
            throw new BusinessRuleViolationException("Source account " + lockedSource.getAccountNumber() + " is " + lockedSource.getStatus() + ". Transfers are disallowed.");
        }
        if (!lockedDest.isOperational()) {
            throw new BusinessRuleViolationException("Destination account " + lockedDest.getAccountNumber() + " is " + lockedDest.getStatus() + ". Transfers are disallowed.");
        }

        // 7. Balance Verification (Atomic check under row lock)
        BigDecimal sourceAvailable = lockedSource.getAvailableBalance();
        if (sourceAvailable.compareTo(transferAmount) < 0) {
            log.warn("Transfer rejected: Insufficient funds in account {}. Available: {}, Requested: {}",
                    lockedSource.getAccountNumber(), sourceAvailable, transferAmount);
            throw new InsufficientFundsException(
                    String.format("Insufficient available balance (Available: %s %s, Requested: %s %s)",
                            MoneyUtils.formatDisplay(sourceAvailable), lockedSource.getCurrency(),
                            MoneyUtils.formatDisplay(transferAmount), lockedSource.getCurrency())
            );
        }

        // 8. Execute Double-Entry Balance Updates
        BigDecimal newSourceBalance = lockedSource.getCachedBalance().subtract(transferAmount);
        BigDecimal newDestBalance = lockedDest.getCachedBalance().add(transferAmount);

        lockedSource.setCachedBalance(newSourceBalance);
        lockedDest.setCachedBalance(newDestBalance);

        accountRepository.save(lockedSource);
        accountRepository.save(lockedDest);

        // 9. Create Master Ledger Journal Entry
        String transactionRef = generateTransactionReference();
        JournalEntry journalEntry = new JournalEntry(
                transactionRef,
                idempotencyKey.trim(),
                TransactionType.P2P_TRANSFER,
                transferAmount,
                lockedSource.getCurrency(),
                request.getDescription() != null ? request.getDescription() : "Funds Transfer"
        );
        journalEntry.setStatus(TransactionStatus.COMPLETED);
        journalEntry.setCompletedAt(Instant.now());

        // 10. Create Immutable Double-Entry Legs (Debit Source, Credit Destination)
        TransactionLeg debitLeg = new TransactionLeg(
                journalEntry,
                lockedSource,
                LegType.DEBIT,
                transferAmount,
                lockedSource.getCurrency(),
                newSourceBalance
        );

        TransactionLeg creditLeg = new TransactionLeg(
                journalEntry,
                lockedDest,
                LegType.CREDIT,
                transferAmount,
                lockedDest.getCurrency(),
                newDestBalance
        );

        journalEntry.addLeg(debitLeg);
        journalEntry.addLeg(creditLeg);

        JournalEntry savedJournal = journalEntryRepository.save(journalEntry);

        // 11. Transactional Outbox Event
        createOutboxEvent(savedJournal, lockedSource, lockedDest, transferAmount);

        // 12. Audit Logging
        auditService.recordAction(
                authenticatedUserId,
                isAdmin ? "ADMIN" : "CUSTOMER",
                "TRANSFER_EXECUTED",
                "JOURNAL_ENTRY",
                savedJournal.getId().toString(),
                null,
                null
        );

        log.info("Transfer completed successfully: Reference={}, Amount={} {}, Source={}, Dest={}",
                transactionRef, transferAmount, lockedSource.getCurrency(),
                lockedSource.getAccountNumber(), lockedDest.getAccountNumber());

        TransferReceiptResponse receipt = new TransferReceiptResponse(
                savedJournal.getId(),
                savedJournal.getTransactionReference(),
                savedJournal.getIdempotencyKey(),
                lockedSource.getAccountNumber(),
                lockedDest.getAccountNumber(),
                MoneyUtils.formatDisplay(transferAmount),
                savedJournal.getCurrency(),
                savedJournal.getStatus(),
                savedJournal.getCompletedAt(),
                savedJournal.getDescription(),
                MoneyUtils.formatDisplay(lockedSource.getAvailableBalance())
        );

        // 13. Mark Idempotency Record as COMPLETED and cache response
        idempotencyService.complete(idempotencyKey.trim(), 201, receipt);

        return receipt;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionHistoryResponse> getAccountTransactionHistory(UUID accountId, Pageable pageable) {
        if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("Account not found with ID: " + accountId);
        }

        Page<TransactionLeg> legsPage = transactionLegRepository.findByAccountIdWithJournalEntry(accountId, pageable);
        return legsPage.map(leg -> new TransactionHistoryResponse(
                leg.getId(),
                leg.getJournalEntry().getTransactionReference(),
                leg.getJournalEntry().getTransactionType(),
                leg.getLegType(),
                MoneyUtils.formatDisplay(leg.getAmount()),
                leg.getCurrency(),
                MoneyUtils.formatDisplay(leg.getBalanceAfter()),
                leg.getJournalEntry().getDescription(),
                leg.getCreatedAt(),
                leg.getJournalEntry().getStatus()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public TransferReceiptResponse getReceiptByTransactionReference(String transactionReference) {
        JournalEntry journal = journalEntryRepository.findByTransactionReference(transactionReference)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with reference: " + transactionReference));
        return mapToReceipt(journal);
    }

    private TransferReceiptResponse mapToReceipt(JournalEntry journal) {
        String sourceAccountNum = "UNKNOWN";
        String destAccountNum = "UNKNOWN";
        BigDecimal sourceRemaining = BigDecimal.ZERO;

        for (TransactionLeg leg : journal.getLegs()) {
            if (leg.getLegType() == LegType.DEBIT) {
                sourceAccountNum = leg.getAccount().getAccountNumber();
                sourceRemaining = leg.getBalanceAfter();
            } else if (leg.getLegType() == LegType.CREDIT) {
                destAccountNum = leg.getAccount().getAccountNumber();
            }
        }

        return new TransferReceiptResponse(
                journal.getId(),
                journal.getTransactionReference(),
                journal.getIdempotencyKey(),
                sourceAccountNum,
                destAccountNum,
                MoneyUtils.formatDisplay(journal.getTotalAmount()),
                journal.getCurrency(),
                journal.getStatus(),
                journal.getCompletedAt() != null ? journal.getCompletedAt() : journal.getInitiatedAt(),
                journal.getDescription(),
                MoneyUtils.formatDisplay(sourceRemaining)
        );
    }

    private void createOutboxEvent(JournalEntry journal, Account source, Account dest, BigDecimal amount) {
        try {
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("transactionReference", journal.getTransactionReference());
            payloadMap.put("sourceAccountNumber", source.getAccountNumber());
            payloadMap.put("destinationAccountNumber", dest.getAccountNumber());
            payloadMap.put("amount", MoneyUtils.formatDisplay(amount));
            payloadMap.put("currency", journal.getCurrency());
            payloadMap.put("status", journal.getStatus().name());
            payloadMap.put("timestamp", journal.getCompletedAt().toString());

            String payloadJson = objectMapper.writeValueAsString(payloadMap);
            OutboxEvent outboxEvent = new OutboxEvent("TRANSFER", journal.getTransactionReference(), "TRANSFER_COMPLETED", payloadJson);
            outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Outbox payload for transfer: {}", journal.getTransactionReference(), e);
        }
    }

    private String generateTransactionReference() {
        return "TXN-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
