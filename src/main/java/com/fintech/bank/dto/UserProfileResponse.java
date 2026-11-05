package com.fintech.bank.dto;

import com.fintech.bank.entity.KycStatus;
import com.fintech.bank.entity.UserRole;

import java.time.Instant;
import java.util.UUID;

/**
 * User profile response DTO.
 */
public record UserProfileResponse(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        String fullName,
        String phone,
        UserRole role,
        KycStatus kycStatus,
        int kycTier,
        Instant createdAt
) {}
