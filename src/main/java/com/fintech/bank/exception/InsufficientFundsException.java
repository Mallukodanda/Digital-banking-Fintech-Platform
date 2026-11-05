package com.fintech.bank.exception;

import org.springframework.http.HttpStatus;
import java.math.BigDecimal;

public class InsufficientFundsException extends ApiException {

    public InsufficientFundsException(String accountNumber, BigDecimal availableBalance, BigDecimal requestedAmount) {
        super(String.format("Account %s has available balance of %s, which is insufficient for requested amount of %s",
                        accountNumber, availableBalance, requestedAmount),
                HttpStatus.UNPROCESSABLE_ENTITY,
                "ACC_FUNDS_INSUFFICIENT");
    }

    public InsufficientFundsException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, "ACC_FUNDS_INSUFFICIENT");
    }
}
