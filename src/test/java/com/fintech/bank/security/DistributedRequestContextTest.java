package com.fintech.bank.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DistributedRequestContext Unit Tests")
class DistributedRequestContextTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should resolve correlation ID from MDC")
    void shouldResolveCorrelationId() {
        MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, "corr-abc-123");

        Optional<String> correlationId = DistributedRequestContext.getCorrelationId();

        assertThat(correlationId).isPresent().contains("corr-abc-123");
    }

    @Test
    @DisplayName("Should resolve authenticated user ID from SecurityContext")
    void shouldResolveUserIdFromSecurityContext() {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(
                userId, "user@bank.com", "hash",
                com.fintech.bank.entity.UserRole.ROLE_CUSTOMER,
                com.fintech.bank.entity.AuthStatus.ACTIVE,
                Collections.emptyList()
        );
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<UUID> resolvedId = DistributedRequestContext.getCurrentUserId();

        assertThat(resolvedId).isPresent().contains(userId);
    }

    @Test
    @DisplayName("Should return empty when no authentication exists")
    void shouldReturnEmptyWhenUnauthenticated() {
        Optional<UUID> resolvedId = DistributedRequestContext.getCurrentUserId();
        assertThat(resolvedId).isEmpty();
    }
}
