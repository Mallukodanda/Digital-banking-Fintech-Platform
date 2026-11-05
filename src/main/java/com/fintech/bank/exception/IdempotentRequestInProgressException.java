package com.fintech.bank.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a concurrent duplicate request is detected while the original request
 * is still being actively processed within the system.
 */
public class IdempotentRequestInProgressException extends ApiException {

    public IdempotentRequestInProgressException(String message) {
        super(message, HttpStatus.CONFLICT, "IDEMPOTENCY_REQUEST_IN_PROGRESS");
    }
}
