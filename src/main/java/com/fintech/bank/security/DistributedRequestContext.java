package com.fintech.bank.security;

import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * Thread-local context helper that provides unified access to distributed request metadata,
 * supporting both Modular Monolith mode (via Spring SecurityContext) and Microservice Edge mode
 * (via Gateway injected headers and MDC).
 */
public final class DistributedRequestContext {

    private DistributedRequestContext() {
        // Prevent instantiation
    }

    /**
     * Retrieves the current correlation ID from MDC.
     */
    public static Optional<String> getCorrelationId() {
        return Optional.ofNullable(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY));
    }

    /**
     * Resolves the authenticated User ID from the SecurityContext or Gateway header.
     */
    public static Optional<UUID> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            return Optional.ofNullable(userPrincipal.getId());
        }
        return Optional.empty();
    }
}
