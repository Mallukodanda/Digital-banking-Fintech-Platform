package com.fintech.bank.service;

import com.fintech.bank.dto.AuthResponse;
import com.fintech.bank.dto.LoginRequest;
import com.fintech.bank.dto.RefreshTokenRequest;
import com.fintech.bank.dto.RegisterRequest;
import com.fintech.bank.entity.CustomerProfile;
import com.fintech.bank.entity.UserAuth;
import com.fintech.bank.entity.UserRole;
import com.fintech.bank.exception.DuplicateResourceException;
import com.fintech.bank.exception.UnauthorizedException;
import com.fintech.bank.repository.CustomerProfileRepository;
import com.fintech.bank.repository.UserAuthRepository;
import com.fintech.bank.security.JwtTokenProvider;
import com.fintech.bank.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
@SuppressWarnings("null")
class AuthServiceTest {

    @Mock
    private UserAuthRepository userAuthRepository;

    @Mock
    private CustomerProfileRepository customerProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private UserAuth mockUserAuth;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest(
                "alice@fintech.bank",
                "Password123!",
                "Alice",
                "Johnson",
                "+14155551234"
        );

        loginRequest = new LoginRequest("alice@fintech.bank", "Password123!");

        mockUserAuth = new UserAuth("alice@fintech.bank", "$2a$12$hashedPassword", UserRole.ROLE_CUSTOMER);
        mockUserAuth.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Successful registration should hash password and issue JWT tokens")
    void testRegisterCustomerSuccess() {
        when(userAuthRepository.existsByEmail(anyString())).thenReturn(false);
        when(customerProfileRepository.existsByPhone(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashedPassword");
        when(userAuthRepository.save(any(UserAuth.class))).thenReturn(mockUserAuth);
        when(customerProfileRepository.save(any(CustomerProfile.class))).thenReturn(new CustomerProfile());
        when(tokenProvider.generateAccessToken(any(), any(), any())).thenReturn("mock-access-token");
        when(tokenProvider.generateRefreshToken(any(), any())).thenReturn("mock-refresh-token");
        when(tokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L);

        AuthResponse response = authService.registerCustomer(registerRequest);

        assertNotNull(response);
        assertEquals("mock-access-token", response.accessToken());
        assertEquals("mock-refresh-token", response.refreshToken());
        assertEquals("alice@fintech.bank", response.email());
        assertEquals("ROLE_CUSTOMER", response.role());

        verify(passwordEncoder, times(1)).encode(registerRequest.password());
        verify(userAuthRepository, times(1)).save(any(UserAuth.class));
        verify(customerProfileRepository, times(1)).save(any(CustomerProfile.class));
    }

    @Test
    @DisplayName("Duplicate email registration should throw DuplicateResourceException")
    void testRegisterDuplicateEmail() {
        when(userAuthRepository.existsByEmail("alice@fintech.bank")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.registerCustomer(registerRequest));
        verify(userAuthRepository, never()).save(any());
    }

    @Test
    @DisplayName("Successful login should verify password hash and issue tokens")
    void testLoginSuccess() {
        when(userAuthRepository.findByEmail("alice@fintech.bank")).thenReturn(Optional.of(mockUserAuth));
        when(passwordEncoder.matches("Password123!", "$2a$12$hashedPassword")).thenReturn(true);
        when(tokenProvider.generateAccessToken(any(), any(), any())).thenReturn("mock-access-token");
        when(tokenProvider.generateRefreshToken(any(), any())).thenReturn("mock-refresh-token");
        when(tokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L);

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("mock-access-token", response.accessToken());
        verify(passwordEncoder, times(1)).matches("Password123!", "$2a$12$hashedPassword");
    }

    @Test
    @DisplayName("Login with invalid password should increment failed count and throw UnauthorizedException")
    void testLoginInvalidPassword() {
        when(userAuthRepository.findByEmail("alice@fintech.bank")).thenReturn(Optional.of(mockUserAuth));
        when(passwordEncoder.matches("WrongPassword", "$2a$12$hashedPassword")).thenReturn(false);

        LoginRequest badRequest = new LoginRequest("alice@fintech.bank", "WrongPassword");

        assertThrows(UnauthorizedException.class, () -> authService.login(badRequest));
        assertEquals(1, mockUserAuth.getFailedAttempts());
        verify(userAuthRepository, times(1)).save(mockUserAuth);
    }

    @Test
    @DisplayName("Refresh token with valid token should issue fresh token pair")
    void testRefreshTokenSuccess() {
        String refreshToken = "valid-refresh-token";
        UUID userId = mockUserAuth.getId();

        when(tokenProvider.validateToken(refreshToken)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(refreshToken)).thenReturn(userId);
        when(userAuthRepository.findById(userId)).thenReturn(Optional.of(mockUserAuth));
        when(tokenProvider.generateAccessToken(any(), any(), any())).thenReturn("new-access-token");
        when(tokenProvider.generateRefreshToken(any(), any())).thenReturn("new-refresh-token");
        when(tokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L);

        AuthResponse response = authService.refreshToken(new RefreshTokenRequest(refreshToken));

        assertNotNull(response);
        assertEquals("new-access-token", response.accessToken());
        assertEquals("new-refresh-token", response.refreshToken());
    }
}
