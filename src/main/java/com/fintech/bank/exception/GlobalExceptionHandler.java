package com.fintech.bank.exception;

import com.fintech.bank.dto.ErrorResponse;
import com.fintech.bank.dto.ValidationErrorItem;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

/**
 * Centralized Global Exception Handler transforming all application exceptions
 * into RFC 7807 compliant ErrorResponse envelopes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String CORRELATION_ID_KEY = "correlationId";

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        log.warn("Business domain exception [code: {}]: {}", ex.getErrorCode(), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .type(buildErrorType(ex.getErrorCode()))
                .title(ex.getHttpStatus().getReasonPhrase())
                .status(ex.getHttpStatus().value())
                .detail(ex.getMessage())
                .instance(request.getRequestURI())
                .errorCode(ex.getErrorCode())
                .correlationId(getCorrelationId())
                .build();

        return ResponseEntity.status(ex.getHttpStatus().value()).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ValidationErrorItem> validationErrors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            validationErrors.add(new ValidationErrorItem(
                    fieldError.getField(),
                    fieldError.getRejectedValue(),
                    fieldError.getDefaultMessage()
            ));
        }

        log.warn("Validation failure on URI {}: {} errors", request.getRequestURI(), validationErrors.size());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .type(buildErrorType("VALIDATION_FAILED"))
                .title("Validation Failed")
                .status(HttpStatus.BAD_REQUEST.value())
                .detail("One or more input fields failed validation constraints.")
                .instance(request.getRequestURI())
                .errorCode("INPUT_VALIDATION_ERROR")
                .correlationId(getCorrelationId())
                .validationErrors(validationErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied for URI {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .type(buildErrorType("FORBIDDEN"))
                .title("Forbidden")
                .status(HttpStatus.FORBIDDEN.value())
                .detail("You do not possess the required permissions or role to perform this operation.")
                .instance(request.getRequestURI())
                .errorCode("AUTH_FORBIDDEN_ACCESS")
                .correlationId(getCorrelationId())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication failed for URI {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .type(buildErrorType("UNAUTHORIZED"))
                .title("Unauthorized")
                .status(HttpStatus.UNAUTHORIZED.value())
                .detail(ex.getMessage())
                .instance(request.getRequestURI())
                .errorCode("AUTH_UNAUTHORIZED")
                .correlationId(getCorrelationId())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled internal server error on URI: " + request.getRequestURI(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .type(buildErrorType("INTERNAL_SERVER_ERROR"))
                .title("Internal Server Error")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .detail("An unexpected error occurred while processing your request. Please quote the correlationId to support.")
                .instance(request.getRequestURI())
                .errorCode("INTERNAL_UNEXPECTED_ERROR")
                .correlationId(getCorrelationId())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    private String getCorrelationId() {
        String correlationId = MDC.get(CORRELATION_ID_KEY);
        return correlationId != null ? correlationId : "N/A";
    }

    private String buildErrorType(String errorCode) {
        return "https://api.fintech.bank/v1/errors/" + errorCode.toLowerCase().replace('_', '-');
    }
}
