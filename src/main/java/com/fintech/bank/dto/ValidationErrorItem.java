package com.fintech.bank.dto;

/**
 * Encapsulates a single field-level validation failure.
 */
public record ValidationErrorItem(
        String field,
        Object rejectedValue,
        String message
) {}
