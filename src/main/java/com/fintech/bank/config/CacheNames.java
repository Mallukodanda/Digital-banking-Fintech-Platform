package com.fintech.bank.config;

/**
 * Centralized registry of cache space names and SLA configurations.
 */
public final class CacheNames {

    private CacheNames() {
    }

    /**
     * Customer Profile & KYC status cache (TTL: 15 minutes)
     */
    public static final String CUSTOMER_PROFILES = "customer-profiles";

    /**
     * Account Beneficiary list cache (TTL: 10 minutes)
     */
    public static final String BENEFICIARIES = "beneficiaries";

    /**
     * System metadata & fee schedules (TTL: 60 minutes)
     */
    public static final String SYSTEM_CONFIGS = "system-configs";
}
