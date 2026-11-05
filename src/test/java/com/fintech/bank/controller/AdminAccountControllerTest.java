package com.fintech.bank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.bank.config.SecurityConfig;
import com.fintech.bank.dto.AccountResponse;
import com.fintech.bank.dto.AccountStatusUpdateRequest;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminAccountController.class)
@Import({SecurityConfig.class, CorrelationIdFilter.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@DisplayName("AdminAccountController WebMvc Tests")
@SuppressWarnings("null")
class AdminAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "admin@bank.com", roles = {"ADMIN"})
    @DisplayName("ROLE_ADMIN can freeze an account with audit justification")
    void testAdminFreezeAccountSuccess() throws Exception {
        UUID accountId = UUID.randomUUID();
        AccountStatusUpdateRequest request = new AccountStatusUpdateRequest(AccountStatus.FROZEN, "Suspected fraud");

        AccountResponse response = new AccountResponse(
                accountId,
                UUID.randomUUID(),
                "1004829103",
                AccountType.CHECKING,
                "USD",
                AccountStatus.FROZEN,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                Instant.now(),
                Instant.now()
        );

        when(accountService.updateAccountStatus(eq(accountId), any(AccountStatusUpdateRequest.class), any(), any(), any(), any()))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/admin/accounts/" + accountId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("FROZEN"));
    }

    @Test
    @WithMockUser(username = "customer@bank.com", roles = {"CUSTOMER"})
    @DisplayName("ROLE_CUSTOMER attempting to freeze an account should receive 403 Forbidden")
    void testCustomerForbiddenFromAdminAccountEndpoint() throws Exception {
        UUID accountId = UUID.randomUUID();
        AccountStatusUpdateRequest request = new AccountStatusUpdateRequest(AccountStatus.FROZEN, "Attempted freeze");

        mockMvc.perform(patch("/api/v1/admin/accounts/" + accountId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("AUTH_FORBIDDEN_ACCESS"));
    }
}
