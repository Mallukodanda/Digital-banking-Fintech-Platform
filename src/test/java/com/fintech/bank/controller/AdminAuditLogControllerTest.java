package com.fintech.bank.controller;

import com.fintech.bank.config.SecurityConfig;
import com.fintech.bank.dto.AuditLogFilterRequest;
import com.fintech.bank.dto.AuditLogResponse;
import com.fintech.bank.entity.AuditAction;
import com.fintech.bank.entity.AuditResult;
import com.fintech.bank.security.*;
import com.fintech.bank.service.AuditService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminAuditLogController.class)
@Import({SecurityConfig.class, CorrelationIdFilter.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@DisplayName("AdminAuditLogController WebMvc Tests")
@SuppressWarnings("null")
class AdminAuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditService auditService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "admin@bank.com", roles = {"ADMIN"})
    @DisplayName("GET /api/v1/admin/audit-logs allows ROLE_ADMIN and returns paged logs")
    void testGetAuditLogsAllowedForAdmin() throws Exception {
        UUID logId = UUID.randomUUID();
        AuditLogResponse item = new AuditLogResponse(
                logId,
                UUID.randomUUID(),
                "admin@bank.com",
                "ROLE_ADMIN",
                AuditAction.ACCOUNT_FROZEN,
                "ACCOUNT",
                "1000000001",
                AuditResult.SUCCESS,
                "corr-111",
                "127.0.0.1",
                "{\"note\":\"Regulatory freeze\"}",
                Instant.now()
        );

        Page<AuditLogResponse> page = new PageImpl<>(List.of(item));
        when(auditService.getAuditLogs(any(AuditLogFilterRequest.class), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .param("action", AuditAction.ACCOUNT_FROZEN)
                        .param("result", "SUCCESS")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(logId.toString()))
                .andExpect(jsonPath("$.data.content[0].action").value(AuditAction.ACCOUNT_FROZEN))
                .andExpect(jsonPath("$.data.content[0].result").value("SUCCESS"));
    }

    @Test
    @WithMockUser(username = "auditor@bank.com", roles = {"AUDITOR"})
    @DisplayName("GET /api/v1/admin/audit-logs allows ROLE_AUDITOR")
    void testGetAuditLogsAllowedForAuditor() throws Exception {
        when(auditService.getAuditLogs(any(AuditLogFilterRequest.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "customer@bank.com", roles = {"CUSTOMER"})
    @DisplayName("GET /api/v1/admin/audit-logs forbids ROLE_CUSTOMER with 403")
    void testGetAuditLogsForbiddenForCustomer() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/admin/audit-logs rejects unauthenticated requests with 401")
    void testGetAuditLogsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@bank.com", roles = {"ADMIN"})
    @DisplayName("GET /api/v1/admin/audit-logs/{id} returns individual log entry")
    void testGetAuditLogById() throws Exception {
        UUID logId = UUID.randomUUID();
        AuditLogResponse item = new AuditLogResponse(
                logId,
                UUID.randomUUID(),
                "customer@bank.com",
                "ROLE_CUSTOMER",
                AuditAction.TRANSFER_COMPLETED,
                "TRANSFER",
                "TXN-777",
                AuditResult.SUCCESS,
                "corr-777",
                "10.0.0.5",
                null,
                Instant.now()
        );

        when(auditService.getAuditLogById(logId)).thenReturn(item);

        mockMvc.perform(get("/api/v1/admin/audit-logs/" + logId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(logId.toString()))
                .andExpect(jsonPath("$.data.action").value(AuditAction.TRANSFER_COMPLETED));
    }
}
