package com.fintech.bank.service;

import com.fintech.bank.dto.TransactionHistoryResponse;
import com.fintech.bank.dto.TransferReceiptResponse;
import com.fintech.bank.dto.TransferRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for high-integrity, ACID-compliant financial transfers and ledger histories.
 */
public interface TransferService {

    /**
     * Executes an ACID, concurrency-safe, double-entry peer-to-peer funds transfer.
     *
     * @param request              Transfer payload detailing source account, beneficiary account, amount, and description
     * @param idempotencyKey       Unique idempotency token supplied in the request header
     * @param authenticatedUserId  UUID of the authenticated user performing the transfer
     * @param isAdmin              Flag indicating if the user has administrative privileges
     * @return TransferReceiptResponse containing transaction references, timestamps, and balance snapshots
     */
    TransferReceiptResponse transferFunds(TransferRequest request, String idempotencyKey, UUID authenticatedUserId, boolean isAdmin);

    /**
     * Retrieves paged historical transaction ledger lines for a given account.
     *
     * @param accountId Target account ID
     * @param pageable  Pagination and sorting parameters
     * @return Page of TransactionHistoryResponse
     */
    Page<TransactionHistoryResponse> getAccountTransactionHistory(UUID accountId, Pageable pageable);

    /**
     * Retrieves transaction receipt by master reference.
     *
     * @param transactionReference Human-readable transaction reference code
     * @return TransferReceiptResponse
     */
    TransferReceiptResponse getReceiptByTransactionReference(String transactionReference);
}
