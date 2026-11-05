package com.fintech.bank.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends ApiException {

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s already exists with %s: '%s'", resourceName, fieldName, fieldValue),
                HttpStatus.CONFLICT,
                "RESOURCE_ALREADY_EXISTS");
    }

    public DuplicateResourceException(String message, String errorCode) {
        super(message, HttpStatus.CONFLICT, errorCode);
    }
}
