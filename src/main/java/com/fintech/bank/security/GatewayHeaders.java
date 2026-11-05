package com.fintech.bank.security;

/**
 * Standard HTTP header contracts utilized across the API Gateway, Edge Router,
 * and downstream microservices.
 */
public final class GatewayHeaders {

    private GatewayHeaders() {
        // Prevent instantiation
    }

    /**
     * Unique correlation identifier passed across distributed service boundaries.
     */
    public static final String CORRELATION_ID = "X-Correlation-Id";

    /**
     * Authenticated User UUID injected by API Gateway after JWT validation.
     */
    public static final String USER_ID = "X-User-Id";

    /**
     * Authenticated User Role (e.g. ROLE_CUSTOMER, ROLE_ADMIN).
     */
    public static final String USER_ROLE = "X-User-Role";

    /**
     * Original client IPv4/IPv6 address.
     */
    public static final String CLIENT_IP = "X-Client-Ip";

    /**
     * W3C Distributed Tracing header standard (Trace Context).
     */
    public static final String TRACEPARENT = "traceparent";

    /**
     * Distributed idempotency key passed on mutating operations.
     */
    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";
}
