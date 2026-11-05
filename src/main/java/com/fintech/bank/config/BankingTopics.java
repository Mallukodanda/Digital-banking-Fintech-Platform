package com.fintech.bank.config;

/**
 * Global Kafka Topic and Consumer Group Constants for the Digital Banking Platform.
 */
public final class BankingTopics {

    private BankingTopics() {
    }

    // Topics
    public static final String TRANSACTION_COMPLETED = "banking.transactions.completed";
    public static final String TRANSACTION_COMPLETED_DLT = "banking.transactions.completed.DLT";
    public static final String FRAUD_ALERTS = "banking.fraud.alerts";
    public static final String AUDIT_EVENTS = "banking.audit.events";

    // Consumer Group IDs
    public static final String GROUP_NOTIFICATIONS = "notification-service-group";
    public static final String GROUP_FRAUD_DETECTION = "fraud-detection-group";
    public static final String GROUP_AUDIT = "audit-service-group";
    public static final String GROUP_ANALYTICS = "analytics-service-group";
    public static final String GROUP_DLQ_MONITOR = "dlq-monitoring-group";
}
