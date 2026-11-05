package com.fintech.bank.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an incoming request provides an Idempotency-Key that was already used
 * with a different request payload, URL, or authentication principal.
 */
public class IdempotencyConflictException extends ApiException {

    public IdempotencyConflictException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, "IDEMPOTENCY_PAYLOAD_MISMATCH");
    }
}
