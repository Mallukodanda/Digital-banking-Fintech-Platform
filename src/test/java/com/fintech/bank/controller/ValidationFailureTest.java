package com.fintech.bank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.bank.config.SecurityConfig;
import com.fintech.bank.dto.RegisterRequest;
import com.fintech.bank.dto.TransferRequest;
import com.fintech.bank.security.*;
import com.fintech.bank.service.AccountService;
import com.fintech.bank.service.AuthService;
import com.fintech.bank.service.TransferService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AuthController.class, TransferController.class, AccountController.class})
@Import({SecurityConfig.class, CorrelationIdFilter.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@DisplayName("Layer 4 & 5: Bean Validation & Security Rejection Tests")
@SuppressWarnings("null")
class ValidationFailureTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private TransferService transferService;

    @MockBean
    private AccountService accountService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("Should reject registration with invalid email and short password (HTTP 400)")
    void testRegisterValidationFailure() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest(
                "not-an-email",
                "short", // Password requires >= 8 characters
                "",      // Blank firstName
                "Smith",
                "invalid-phone"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "customer@bank.com", roles = {"CUSTOMER"})
    @DisplayName("Should reject transfer with negative or zero amount (HTTP 400)")
    void testTransferNegativeAmountValidationFailure() throws Exception {
        TransferRequest invalidTransfer = new TransferRequest(
                UUID.randomUUID(),
                "100000000002",
                new BigDecimal("-50.00"), // Negative amount
                "INR",
                "Bad Transfer"
        );

        mockMvc.perform(post("/api/v1/transfers")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidTransfer)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "customer@bank.com", roles = {"CUSTOMER"})
    @DisplayName("Should reject transfer when Idempotency-Key header is missing (HTTP 400)")
    void testTransferMissingIdempotencyKeyHeader() throws Exception {
        TransferRequest validTransfer = new TransferRequest(
                UUID.randomUUID(),
                "100000000002",
                new BigDecimal("500.00"),
                "INR",
                "Valid transfer payload"
        );

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validTransfer)))
                .andExpect(status().isBadRequest());
    }
}
