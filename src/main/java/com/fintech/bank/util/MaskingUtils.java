package com.fintech.bank.util;

/**
 * Utility for masking sensitive PII and account identifiers in logs and API summaries.
 */
public final class MaskingUtils {

    private MaskingUtils() {
        // Prevent instantiation
    }

    /**
     * Masks account number: 1004829103 -> 1004****03
     */
    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 6) {
            return "******";
        }
        int len = accountNumber.length();
        return accountNumber.substring(0, 4) + "****" + accountNumber.substring(len - 2);
    }

    /**
     * Masks email: john.doe@example.com -> j***e@example.com
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***@***";
        }
        String[] parts = email.split("@");
        String name = parts[0];
        String domain = parts[1];

        if (name.length() <= 2) {
            return name.charAt(0) + "***@" + domain;
        }
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + "@" + domain;
    }

    /**
     * Masks phone number: +14155552671 -> +1415****671
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "******";
        }
        int len = phone.length();
        return phone.substring(0, 5) + "****" + phone.substring(len - 3);
    }
}
