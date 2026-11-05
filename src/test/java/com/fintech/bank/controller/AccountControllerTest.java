package com.fintech.bank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.bank.config.SecurityConfig;
import com.fintech.bank.dto.AccountBalanceResponse;
import com.fintech.bank.dto.AccountResponse;
import com.fintech.bank.dto.CreateAccountRequest;
import com.fintech.bank.entity.AccountStatus;
import com.fintech.bank.entity.AccountType;
import com.fintech.bank.security.*;
import com.fintech.bank.service.AccountService;
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
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
@Import({SecurityConfig.class, CorrelationIdFilter.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@DisplayName("AccountController WebMvc Tests")
@SuppressWarnings("null")
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    @MockBean
    private AccountSecurity accountSecurity;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "customer@bank.com", roles = {"CUSTOMER"})
    @DisplayName("POST /api/v1/accounts with valid payload should return 201 Created")
    void testCreateAccountSuccess() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest(AccountType.CHECKING, "USD");
        UUID accountId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        AccountResponse accountResponse = new AccountResponse(
                accountId,
                customerId,
                "1004829103",
                AccountType.CHECKING,
                "USD",
                AccountStatus.ACTIVE,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                Instant.now(),
                Instant.now()
        );

        when(accountService.createAccount(any(UUID.class), any(CreateAccountRequest.class))).thenReturn(accountResponse);

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountNumber").value("1004829103"))
                .andExpect(jsonPath("$.data.accountType").value("CHECKING"));
    }

    @Test
    @WithMockUser(username = "customer@bank.com", roles = {"CUSTOMER"})
    @DisplayName("GET /api/v1/accounts should list all customer-owned accounts")
    void testListMyAccounts() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        AccountResponse accountResponse = new AccountResponse(
                accountId,
                customerId,
                "1004829103",
                AccountType.CHECKING,
                "USD",
                AccountStatus.ACTIVE,
                new BigDecimal("1000.0000"),
                new BigDecimal("1000.0000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                Instant.now(),
                Instant.now()
        );

        when(accountService.getAccountsByUserId(any(UUID.class))).thenReturn(List.of(accountResponse));

        mockMvc.perform(get("/api/v1/accounts")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].accountNumber").value("1004829103"));
    }

    @Test
    @WithMockUser(username = "customer@bank.com", roles = {"CUSTOMER"})
    @DisplayName("GET /api/v1/accounts/{id}/balance should return real-time balance for owner")
    void testGetAccountBalance() throws Exception {
        UUID accountId = UUID.randomUUID();
        AccountBalanceResponse balanceResponse = new AccountBalanceResponse(
                accountId,
                "1004829103",
                "USD",
                new BigDecimal("950.0000"),
                new BigDecimal("1000.0000"),
                new BigDecimal("50.0000"),
                Instant.now()
        );

        when(accountSecurity.isAccountOwner(eq(accountId), any())).thenReturn(true);
        when(accountService.getAccountBalance(accountId)).thenReturn(balanceResponse);

        mockMvc.perform(get("/api/v1/accounts/" + accountId + "/balance")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.availableBalance").value(950.0000));
    }

    @Test
    @WithMockUser(username = "attacker@bank.com", roles = {"CUSTOMER"})
    @DisplayName("GET /api/v1/accounts/{id} should return 403 Forbidden if not the owner")
    void testGetAccountDeniedForNonOwner() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(accountSecurity.isAccountOwner(eq(accountId), any())).thenReturn(false);

        mockMvc.perform(get("/api/v1/accounts/" + accountId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("AUTH_FORBIDDEN_ACCESS"));
    }
}
