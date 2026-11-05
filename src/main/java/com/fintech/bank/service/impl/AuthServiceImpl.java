package com.fintech.bank.service.impl;

import com.fintech.bank.dto.*;
import com.fintech.bank.entity.*;
import com.fintech.bank.exception.DuplicateResourceException;
import com.fintech.bank.exception.ResourceNotFoundException;
import com.fintech.bank.exception.UnauthorizedException;
import com.fintech.bank.repository.CustomerProfileRepository;
import com.fintech.bank.repository.UserAuthRepository;
import com.fintech.bank.config.CacheNames;
import com.fintech.bank.security.JwtTokenProvider;
import com.fintech.bank.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Production implementation of AuthService enforcing password hashing,
 * duplicate verification, JWT issuance, and user profile management.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserAuthRepository userAuthRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthServiceImpl(UserAuthRepository userAuthRepository,
                           CustomerProfileRepository customerProfileRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider tokenProvider) {
        this.userAuthRepository = userAuthRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Override
    @Transactional
    public AuthResponse registerCustomer(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userAuthRepository.existsByEmail(normalizedEmail)) {
            log.warn("Registration rejected: email already exists [{}]", normalizedEmail);
            throw new DuplicateResourceException("User", "email", normalizedEmail);
        }

        if (customerProfileRepository.existsByPhone(request.phone().trim())) {
            log.warn("Registration rejected: phone already exists [{}]", request.phone());
            throw new DuplicateResourceException("Customer", "phone", request.phone());
        }

        // 1. Hash Password with BCrypt cost factor 12
        String passwordHash = passwordEncoder.encode(request.password());

        // 2. Create UserAuth Entity
        UserAuth userAuth = new UserAuth(normalizedEmail, passwordHash, UserRole.ROLE_CUSTOMER);
        UserAuth savedUser = userAuthRepository.save(userAuth);

        // 3. Create CustomerProfile Entity
        CustomerProfile profile = new CustomerProfile(
                savedUser,
                request.firstName().trim(),
                request.lastName().trim(),
                request.phone().trim()
        );
        customerProfileRepository.save(profile);

        log.info("Customer successfully registered [userId: {}, email: {}]", savedUser.getId(), normalizedEmail);

        // 4. Generate Initial Tokens
        String accessToken = tokenProvider.generateAccessToken(savedUser.getId(), savedUser.getEmail(), savedUser.getRole());
        String refreshToken = tokenProvider.generateRefreshToken(savedUser.getId(), savedUser.getEmail());

        return AuthResponse.of(
                accessToken,
                refreshToken,
                tokenProvider.getAccessTokenExpirationMs() / 1000,
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        UserAuth userAuth = userAuthRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password", "AUTH_INVALID_CREDENTIALS"));

        // Check if account is locked
        if (!userAuth.isAccountNonLocked()) {
            log.warn("Login attempt on locked account [email: {}]", normalizedEmail);
            throw new UnauthorizedException("Account is temporarily locked. Please try again later.", "AUTH_ACCOUNT_LOCKED");
        }

        // Verify Password Hash
        if (!passwordEncoder.matches(request.password(), userAuth.getPasswordHash())) {
            int attempts = userAuth.getFailedAttempts() + 1;
            userAuth.setFailedAttempts(attempts);
            if (attempts >= 5) {
                userAuth.setStatus(AuthStatus.LOCKED);
                userAuth.setLockedUntil(Instant.now().plusSeconds(900)); // 15 min lock
                log.warn("Account locked due to 5 consecutive failed attempts [email: {}]", normalizedEmail);
            }
            userAuthRepository.save(userAuth);
            throw new UnauthorizedException("Invalid email or password", "AUTH_INVALID_CREDENTIALS");
        }

        // Reset failed attempts on success
        if (userAuth.getFailedAttempts() > 0) {
            userAuth.setFailedAttempts(0);
            userAuth.setLockedUntil(null);
            userAuthRepository.save(userAuth);
        }

        log.info("User successfully logged in [userId: {}, email: {}]", userAuth.getId(), normalizedEmail);

        String accessToken = tokenProvider.generateAccessToken(userAuth.getId(), userAuth.getEmail(), userAuth.getRole());
        String refreshToken = tokenProvider.generateRefreshToken(userAuth.getId(), userAuth.getEmail());

        return AuthResponse.of(
                accessToken,
                refreshToken,
                tokenProvider.getAccessTokenExpirationMs() / 1000,
                userAuth.getId(),
                userAuth.getEmail(),
                userAuth.getRole().name()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String token = request.refreshToken();

        if (!tokenProvider.validateToken(token)) {
            log.warn("Invalid or expired refresh token presented");
            throw new UnauthorizedException("Invalid or expired refresh token", "AUTH_REFRESH_TOKEN_INVALID");
        }

        UUID userId = tokenProvider.getUserIdFromToken(token);
        UserAuth userAuth = userAuthRepository.findById(java.util.Objects.requireNonNull(userId, "userId cannot be null"))
                .orElseThrow(() -> new UnauthorizedException("User not found", "AUTH_USER_NOT_FOUND"));

        if (!userAuth.isAccountNonLocked()) {
            throw new UnauthorizedException("Account is locked or suspended", "AUTH_ACCOUNT_LOCKED");
        }

        String newAccessToken = tokenProvider.generateAccessToken(userAuth.getId(), userAuth.getEmail(), userAuth.getRole());
        String newRefreshToken = tokenProvider.generateRefreshToken(userAuth.getId(), userAuth.getEmail());

        return AuthResponse.of(
                newAccessToken,
                newRefreshToken,
                tokenProvider.getAccessTokenExpirationMs() / 1000,
                userAuth.getId(),
                userAuth.getEmail(),
                userAuth.getRole().name()
        );
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.CUSTOMER_PROFILES, key = "#userId")
    public UserProfileResponse getCurrentUserProfile(UUID userId) {
        UserAuth userAuth = userAuthRepository.findById(java.util.Objects.requireNonNull(userId, "userId cannot be null"))
                .orElseThrow(() -> new ResourceNotFoundException("UserAuth", "userId", userId));

        java.util.Optional<CustomerProfile> profileOpt = customerProfileRepository.findByUserAuthId(userId);

        if (profileOpt.isPresent()) {
            CustomerProfile profile = profileOpt.get();
            return new UserProfileResponse(
                    userAuth.getId(),
                    userAuth.getEmail(),
                    profile.getFirstName(),
                    profile.getLastName(),
                    profile.getFullName(),
                    profile.getPhone(),
                    userAuth.getRole(),
                    profile.getKycStatus(),
                    profile.getKycTier(),
                    profile.getCreatedAt()
            );
        } else {
            return new UserProfileResponse(
                    userAuth.getId(),
                    userAuth.getEmail(),
                    "Admin",
                    "User",
                    "Admin User",
                    "+0000000000",
                    userAuth.getRole(),
                    KycStatus.VERIFIED,
                    3,
                    userAuth.getCreatedAt()
            );
        }
    }
}
