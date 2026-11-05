package com.fintech.bank.entity;

/**
 * Type of double-entry ledger financial transaction.
 */
public enum TransactionType {
    P2P_TRANSFER,
    DEPOSIT,
    WITHDRAWAL,
    FEE_CHARGE,
    REVERSAL
}
