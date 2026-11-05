package com.fintech.bank.service;

import com.fintech.bank.dto.*;

import java.util.UUID;

/**
 * Authentication and User Identity Service Interface.
 */
public interface AuthService {

    AuthResponse registerCustomer(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    UserProfileResponse getCurrentUserProfile(UUID userId);
}
