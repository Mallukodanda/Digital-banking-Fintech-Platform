package com.fintech.bank.util;

import java.security.SecureRandom;

/**
 * Generates unique 10-digit bank account numbers with Luhn check-digit verification.
 */
public final class AccountNumberGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private AccountNumberGenerator() {
        // Prevent instantiation
    }

    /**
     * Generates a 10-digit account number (9 random digits + 1 Luhn check digit).
     */
    public static String generateAccountNumber() {
        // 9 random digits starting with non-zero digit (1-9)
        int firstDigit = 1 + RANDOM.nextInt(9);
        StringBuilder sb = new StringBuilder();
        sb.append(firstDigit);

        for (int i = 0; i < 8; i++) {
            sb.append(RANDOM.nextInt(10));
        }

        String partialNumber = sb.toString();
        int checkDigit = calculateLuhnCheckDigit(partialNumber);
        return partialNumber + checkDigit;
    }

    /**
     * Calculates the Luhn check digit for a string of digits.
     */
    public static int calculateLuhnCheckDigit(String number) {
        int sum = 0;
        boolean alternate = true;

        for (int i = number.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(number.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }

        int remainder = sum % 10;
        return (remainder == 0) ? 0 : 10 - remainder;
    }

    /**
     * Validates whether a given account number satisfies the Luhn algorithm.
     */
    public static boolean isValidAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() != 10 || !accountNumber.matches("\\d{10}")) {
            return false;
        }

        String partial = accountNumber.substring(0, 9);
        int expectedCheck = calculateLuhnCheckDigit(partial);
        int actualCheck = Character.getNumericValue(accountNumber.charAt(9));

        return expectedCheck == actualCheck;
    }
}
