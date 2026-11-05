package com.fintech.bank.security;

import com.fintech.bank.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtTokenProvider Security Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final long accessExpirationMs = 900000; // 15 min
    private final long refreshExpirationMs = 604800000; // 7 days

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(secret, accessExpirationMs, refreshExpirationMs, "fintech-digital-bank");
    }

    @Test
    @DisplayName("Should generate a valid signed access token with expected claims")
    void testGenerateAndValidateAccessToken() {
        UUID userId = UUID.randomUUID();
        String email = "john.doe@fintech.bank";
        UserRole role = UserRole.ROLE_CUSTOMER;

        String token = tokenProvider.generateAccessToken(userId, email, role);

        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));
        assertEquals(userId, tokenProvider.getUserIdFromToken(token));
        assertEquals(email, tokenProvider.getEmailFromToken(token));
    }

    @Test
    @DisplayName("Should reject expired token")
    void testExpiredToken() {
        // Create provider with 1ms expiration
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(secret, -1000, -1000, "fintech-digital-bank");
        String expiredToken = shortLivedProvider.generateAccessToken(UUID.randomUUID(), "test@bank.com", UserRole.ROLE_CUSTOMER);

        assertFalse(tokenProvider.validateToken(expiredToken));
    }

    @Test
    @DisplayName("Should reject tampered token with invalid signature")
    void testTamperedToken() {
        String token = tokenProvider.generateAccessToken(UUID.randomUUID(), "test@bank.com", UserRole.ROLE_CUSTOMER);
        String tamperedToken = token + "corrupted";

        assertFalse(tokenProvider.validateToken(tamperedToken));
    }
}
