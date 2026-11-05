package com.fintech.bank.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

/**
 * Authentication response returning JWT access token and refresh token.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UUID userId,
        String email,
        String role
) {
    public static AuthResponse of(String accessToken, String refreshToken, long expiresIn, UUID userId, String email, String role) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, userId, email, role);
    }
}
