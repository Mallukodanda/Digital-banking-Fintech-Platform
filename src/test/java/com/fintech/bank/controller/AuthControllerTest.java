package com.fintech.bank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.bank.config.SecurityConfig;
import com.fintech.bank.dto.AuthResponse;
import com.fintech.bank.dto.LoginRequest;
import com.fintech.bank.dto.RegisterRequest;
import com.fintech.bank.security.CorrelationIdFilter;
import com.fintech.bank.security.CustomUserDetailsService;
import com.fintech.bank.security.JwtAccessDeniedHandler;
import com.fintech.bank.security.JwtAuthenticationEntryPoint;
import com.fintech.bank.security.JwtAuthenticationFilter;
import com.fintech.bank.security.JwtTokenProvider;
import com.fintech.bank.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, CorrelationIdFilter.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@DisplayName("AuthController WebMvc Tests")
@SuppressWarnings("null")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /api/v1/auth/register with valid payload should return 201 Created")
    void testRegisterSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "john@fintech.bank",
                "StrongP@ss123",
                "John",
                "Doe",
                "+14155552671"
        );

        AuthResponse authResponse = AuthResponse.of(
                "mock-access-token",
                "mock-refresh-token",
                900,
                UUID.randomUUID(),
                "john@fintech.bank",
                "ROLE_CUSTOMER"
        );

        when(authService.registerCustomer(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("mock-access-token"))
                .andExpect(jsonPath("$.data.email").value("john@fintech.bank"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register with weak password should return 400 Bad Request with RFC 7807")
    void testRegisterWeakPassword() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "john@fintech.bank",
                "weak", // Fails regex pattern
                "John",
                "Doe",
                "+14155552671"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("INPUT_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors[0].field").value("password"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with valid credentials should return 200 OK")
    void testLoginSuccess() throws Exception {
        LoginRequest request = new LoginRequest("john@fintech.bank", "StrongP@ss123");

        AuthResponse authResponse = AuthResponse.of(
                "mock-access-token",
                "mock-refresh-token",
                900,
                UUID.randomUUID(),
                "john@fintech.bank",
                "ROLE_CUSTOMER"
        );

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("mock-access-token"));
    }
}
