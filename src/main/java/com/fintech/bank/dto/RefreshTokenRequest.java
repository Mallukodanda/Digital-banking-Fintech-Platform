package com.fintech.bank.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for refreshing an access token.
 */
public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {}
