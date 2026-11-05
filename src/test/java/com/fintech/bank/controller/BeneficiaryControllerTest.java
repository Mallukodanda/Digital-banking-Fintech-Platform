package com.fintech.bank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.bank.config.SecurityConfig;
import com.fintech.bank.dto.BeneficiaryResponse;
import com.fintech.bank.dto.CreateBeneficiaryRequest;
import com.fintech.bank.dto.VerifyBeneficiaryResponse;
import com.fintech.bank.entity.AccountStatus;
import com.fintech.bank.entity.AccountType;
import com.fintech.bank.entity.BeneficiaryStatus;
import com.fintech.bank.security.*;
import com.fintech.bank.service.BeneficiaryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BeneficiaryController.class)
@Import({SecurityConfig.class, CorrelationIdFilter.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@DisplayName("BeneficiaryController WebMvc Tests")
@SuppressWarnings("null")
class BeneficiaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BeneficiaryService beneficiaryService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "customer@bank.com", roles = {"CUSTOMER"})
    @DisplayName("POST /api/v1/beneficiaries with valid payload should return 201 Created")
    void testAddBeneficiarySuccess() throws Exception {
        CreateBeneficiaryRequest request = new CreateBeneficiaryRequest("Alice Smith", "1004829103", "Alice Work");

        BeneficiaryResponse response = new BeneficiaryResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Alice Smith",
                "1004829103",
                "Alice Work",
                BeneficiaryStatus.ACTIVE,
                Instant.now()
        );

        when(beneficiaryService.addBeneficiary(any(UUID.class), any(CreateBeneficiaryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/beneficiaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.beneficiaryName").value("Alice Smith"))
                .andExpect(jsonPath("$.data.destinationAccountNumber").value("1004829103"));
    }

    @Test
    @WithMockUser(username = "customer@bank.com", roles = {"CUSTOMER"})
    @DisplayName("GET /api/v1/beneficiaries should return list of active beneficiaries")
    void testListBeneficiaries() throws Exception {
        BeneficiaryResponse response = new BeneficiaryResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Alice Smith",
                "1004829103",
                "Alice Work",
                BeneficiaryStatus.ACTIVE,
                Instant.now()
        );

        when(beneficiaryService.getBeneficiariesByUserId(any(UUID.class))).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/beneficiaries")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].beneficiaryName").value("Alice Smith"));
    }

    @Test
    @WithMockUser(username = "customer@bank.com", roles = {"CUSTOMER"})
    @DisplayName("GET /api/v1/beneficiaries/verify/{accountNumber} should return verification details")
    void testVerifyBeneficiary() throws Exception {
        VerifyBeneficiaryResponse response = new VerifyBeneficiaryResponse(
                "1004829103",
                "A***e S***h",
                AccountType.CHECKING,
                "USD",
                AccountStatus.ACTIVE,
                true
        );

        when(beneficiaryService.verifyBeneficiary(any(UUID.class), eq("1004829103"))).thenReturn(response);

        mockMvc.perform(get("/api/v1/beneficiaries/verify/1004829103")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eligibleForTransfers").value(true))
                .andExpect(jsonPath("$.data.maskedAccountHolderName").value("A***e S***h"));
    }

    @Test
    @WithMockUser(username = "customer@bank.com", roles = {"CUSTOMER"})
    @DisplayName("DELETE /api/v1/beneficiaries/{id} should return 200 OK")
    void testRemoveBeneficiary() throws Exception {
        UUID beneficiaryId = UUID.randomUUID();
        doNothing().when(beneficiaryService).removeBeneficiary(any(UUID.class), eq(beneficiaryId));

        mockMvc.perform(delete("/api/v1/beneficiaries/" + beneficiaryId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Unauthenticated request to beneficiaries should return 401 Unauthorized")
    void testUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/beneficiaries")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_UNAUTHORIZED"));
    }
}
