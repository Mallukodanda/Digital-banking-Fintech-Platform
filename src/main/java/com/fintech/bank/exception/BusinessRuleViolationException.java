package com.fintech.bank.exception;

import org.springframework.http.HttpStatus;

public class BusinessRuleViolationException extends ApiException {

    public BusinessRuleViolationException(String message, String errorCode) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, errorCode);
    }

    public BusinessRuleViolationException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, "BUSINESS_RULE_VIOLATION");
    }
}
