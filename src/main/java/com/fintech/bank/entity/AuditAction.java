package com.fintech.bank.entity;

/**
 * Standardized audit action types for banking operations.
 */
public final class AuditAction {

    private AuditAction() {
    }

    // Authentication & Identity
    public static final String AUTH_LOGIN_SUCCESS = "AUTH_LOGIN_SUCCESS";
    public static final String AUTH_LOGIN_FAILED = "AUTH_LOGIN_FAILED";
    public static final String AUTH_LOGOUT = "AUTH_LOGOUT";
    public static final String AUTH_REGISTER_SUCCESS = "AUTH_REGISTER_SUCCESS";

    // Account Lifecycle
    public static final String ACCOUNT_CREATED = "ACCOUNT_CREATED";
    public static final String ACCOUNT_STATUS_UPDATED = "ACCOUNT_STATUS_UPDATED";
    public static final String ACCOUNT_FROZEN = "ACCOUNT_FROZEN";
    public static final String ACCOUNT_ACTIVATED = "ACCOUNT_ACTIVATED";
    public static final String ACCOUNT_CLOSED = "ACCOUNT_CLOSED";

    // Beneficiary Operations
    public static final String BENEFICIARY_CREATED = "BENEFICIARY_CREATED";
    public static final String BENEFICIARY_VERIFIED = "BENEFICIARY_VERIFIED";
    public static final String BENEFICIARY_DELETED = "BENEFICIARY_DELETED";

    // Financial Transaction Engine
    public static final String TRANSFER_INITIATED = "TRANSFER_INITIATED";
    public static final String TRANSFER_COMPLETED = "TRANSFER_COMPLETED";
    public static final String TRANSFER_FAILED = "TRANSFER_FAILED";

    // Admin & Governance
    public static final String ADMIN_ACTION = "ADMIN_ACTION";
    public static final String ADMIN_ACCOUNT_STATUS_CHANGE = "ADMIN_ACCOUNT_STATUS_CHANGE";

    // Fraud Surveillance & Risk
    public static final String FRAUD_ALERT_FLAGGED = "FRAUD_ALERT_FLAGGED";
    public static final String FRAUD_ALERT_REVIEWED = "FRAUD_ALERT_REVIEWED";
}
