package com.fintech.bank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.bank.config.SecurityConfig;
import com.fintech.bank.dto.FraudAlertResponse;
import com.fintech.bank.dto.FraudReviewRequest;
import com.fintech.bank.entity.FraudStatus;
import com.fintech.bank.entity.RiskLevel;
import com.fintech.bank.security.*;
import com.fintech.bank.service.FraudDetectionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminFraudController.class)
@Import({SecurityConfig.class, CorrelationIdFilter.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@DisplayName("AdminFraudController WebMvc Tests")
@SuppressWarnings("null")
class AdminFraudControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FraudDetectionService fraudDetectionService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "admin@bank.com", roles = {"ADMIN"})
    @DisplayName("GET /api/v1/admin/fraud/alerts allows ADMIN and returns paged alerts")
    void testGetAlertsAllowedForAdmin() throws Exception {
        UUID alertId = UUID.randomUUID();
        FraudAlertResponse item = new FraudAlertResponse(
                alertId,
                "TXN-1712567890-TEST",
                UUID.randomUUID(),
                "100000000001",
                "LARGE_TRANSACTION_AMOUNT",
                85,
                RiskLevel.CRITICAL,
                FraudStatus.FLAGGED,
                "High risk transfer",
                Instant.now(),
                Instant.now()
        );

        Page<FraudAlertResponse> page = new PageImpl<>(List.of(item));
        when(fraudDetectionService.getFraudAlerts(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/fraud/alerts")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].ruleType").value("LARGE_TRANSACTION_AMOUNT"))
                .andExpect(jsonPath("$.data.content[0].riskScore").value(85));
    }

    @Test
    @WithMockUser(username = "customer@bank.com", roles = {"CUSTOMER"})
    @DisplayName("GET /api/v1/admin/fraud/alerts denies CUSTOMER role with 403 Forbidden")
    void testGetAlertsForbiddenForCustomer() throws Exception {
        mockMvc.perform(get("/api/v1/admin/fraud/alerts")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@bank.com", roles = {"ADMIN"})
    @DisplayName("PATCH /api/v1/admin/fraud/alerts/{id}/review resolves alert successfully")
    void testReviewAlertSuccess() throws Exception {
        UUID alertId = UUID.randomUUID();
        FraudReviewRequest request = new FraudReviewRequest(FraudStatus.DISMISSED, "Legitimate customer wire confirmed.");

        FraudAlertResponse item = new FraudAlertResponse(
                alertId,
                "TXN-1712567890-TEST",
                UUID.randomUUID(),
                "100000000001",
                "LARGE_TRANSACTION_AMOUNT",
                85,
                RiskLevel.CRITICAL,
                FraudStatus.DISMISSED,
                "Legitimate customer wire confirmed.",
                Instant.now(),
                Instant.now()
        );

        when(fraudDetectionService.reviewAlert(eq(alertId), any(FraudReviewRequest.class), any())).thenReturn(item);

        mockMvc.perform(patch("/api/v1/admin/fraud/alerts/" + alertId + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DISMISSED"));
    }
}
