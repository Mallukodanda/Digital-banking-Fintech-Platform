package com.fintech.bank.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AccountNumberGenerator Unit Tests")
class AccountNumberGeneratorTest {

    @Test
    @DisplayName("Generated account numbers must be 10 digits and satisfy Luhn verification")
    void testGenerateAccountNumber() {
        for (int i = 0; i < 50; i++) {
            String accNum = AccountNumberGenerator.generateAccountNumber();
            assertNotNull(accNum);
            assertEquals(10, accNum.length());
            assertTrue(accNum.matches("\\d{10}"));
            assertTrue(AccountNumberGenerator.isValidAccountNumber(accNum),
                    "Account number " + accNum + " failed Luhn validation");
        }
    }

    @Test
    @DisplayName("Corrupted account number should fail Luhn validation")
    void testInvalidAccountNumber() {
        // Valid generated number
        String validAcc = AccountNumberGenerator.generateAccountNumber();
        // Mutate check digit
        char lastChar = validAcc.charAt(9);
        char mutatedChar = (lastChar == '9') ? '0' : (char) (lastChar + 1);
        String invalidAcc = validAcc.substring(0, 9) + mutatedChar;

        assertFalse(AccountNumberGenerator.isValidAccountNumber(invalidAcc));
    }
}
