package com.fintech.bank.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MoneyUtils Unit Tests")
class MoneyUtilsTest {

    @Test
    @DisplayName("formatInternal should format to 4 decimal places with Banker's Rounding")
    void testFormatInternal() {
        BigDecimal input = new BigDecimal("150.25678");
        BigDecimal result = MoneyUtils.formatInternal(input);

        assertEquals(new BigDecimal("150.2568"), result);
        assertEquals(4, result.scale());
    }

    @Test
    @DisplayName("formatDisplay should format to 2 decimal places")
    void testFormatDisplay() {
        BigDecimal input = new BigDecimal("150.256");
        BigDecimal result = MoneyUtils.formatDisplay(input);

        assertEquals(new BigDecimal("150.26"), result);
        assertEquals(2, result.scale());
    }

    @Test
    @DisplayName("isPositive should return true only for strictly positive numbers")
    void testIsPositive() {
        assertTrue(MoneyUtils.isPositive(new BigDecimal("0.0001")));
        assertTrue(MoneyUtils.isPositive(new BigDecimal("100.00")));
        assertFalse(MoneyUtils.isPositive(BigDecimal.ZERO));
        assertFalse(MoneyUtils.isPositive(new BigDecimal("-10.00")));
        assertFalse(MoneyUtils.isPositive(null));
    }

    @Test
    @DisplayName("isSufficient should correctly evaluate balance vs debit request")
    void testIsSufficient() {
        BigDecimal balance = new BigDecimal("500.0000");
        assertTrue(MoneyUtils.isSufficient(balance, new BigDecimal("500.0000")));
        assertTrue(MoneyUtils.isSufficient(balance, new BigDecimal("499.9999")));
        assertFalse(MoneyUtils.isSufficient(balance, new BigDecimal("500.0001")));
    }
}
