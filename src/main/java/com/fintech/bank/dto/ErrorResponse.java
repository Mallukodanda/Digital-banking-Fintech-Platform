package com.fintech.bank.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * Standard RFC 7807 Problem Details representation for all API error responses.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        String errorCode,
        String correlationId,
        Instant timestamp,
        List<ValidationErrorItem> validationErrors
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String type = "about:blank";
        private String title;
        private int status;
        private String detail;
        private String instance;
        private String errorCode;
        private String correlationId;
        private Instant timestamp = Instant.now();
        private List<ValidationErrorItem> validationErrors;

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder status(int status) {
            this.status = status;
            return this;
        }

        public Builder detail(String detail) {
            this.detail = detail;
            return this;
        }

        public Builder instance(String instance) {
            this.instance = instance;
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder validationErrors(List<ValidationErrorItem> validationErrors) {
            this.validationErrors = validationErrors;
            return this;
        }

        public ErrorResponse build() {
            return new ErrorResponse(type, title, status, detail, instance, errorCode, correlationId, timestamp, validationErrors);
        }
    }
}
