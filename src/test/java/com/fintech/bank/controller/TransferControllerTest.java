package com.fintech.bank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.bank.config.SecurityConfig;
import com.fintech.bank.dto.TransactionHistoryResponse;
import com.fintech.bank.dto.TransferReceiptResponse;
import com.fintech.bank.dto.TransferRequest;
import com.fintech.bank.entity.LegType;
import com.fintech.bank.entity.TransactionStatus;
import com.fintech.bank.entity.TransactionType;
import com.fintech.bank.security.*;
import com.fintech.bank.service.TransferService;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransferController.class)
@Import({SecurityConfig.class, CorrelationIdFilter.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@DisplayName("TransferController WebMvc Tests")
@SuppressWarnings("null")
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransferService transferService;

    @MockBean
    private AccountSecurity accountSecurity;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "customer@bank.com", roles = {"CUSTOMER"})
    @DisplayName("POST /api/v1/transfers with valid payload and idempotency header returns 201 Created")
    void testInitiateTransferSuccess() throws Exception {
        UUID sourceId = UUID.randomUUID();
        TransferRequest request = new TransferRequest(sourceId, "100000000002", new BigDecimal("8000.00"), "INR", "Rent");

        TransferReceiptResponse receipt = new TransferReceiptResponse(
                UUID.randomUUID(),
                "TXN-1712567890-A8F2BC1D",
                "idemp-uuid-1234",
                "100000000001",
                "100000000002",
                new BigDecimal("8000.00"),
                "INR",
                TransactionStatus.COMPLETED,
                Instant.now(),
                "Rent",
                new BigDecimal("2000.00")
        );

        when(transferService.transferFunds(any(TransferRequest.class), eq("idemp-uuid-1234"), any(), anyBoolean()))
                .thenReturn(receipt);

        mockMvc.perform(post("/api/v1/transfers")
                        .header("X-Idempotency-Key", "idemp-uuid-1234")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.transactionReference").value("TXN-1712567890-A8F2BC1D"))
                .andExpect(jsonPath("$.data.amount").value(8000.00))
                .andExpect(jsonPath("$.data.sourceRemainingBalance").value(2000.00));
    }

    @Test
    @WithMockUser(username = "customer@bank.com", roles = {"CUSTOMER"})
    @DisplayName("POST /api/v1/transfers without idempotency header returns 400 Bad Request")
    void testMissingIdempotencyHeader() throws Exception {
        TransferRequest request = new TransferRequest(UUID.randomUUID(), "100000000002", new BigDecimal("8000.00"), "INR", "Rent");

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "customer@bank.com", roles = {"CUSTOMER"})
    @DisplayName("POST /api/v1/transfers with zero or negative amount returns 400 Bad Request")
    void testInvalidAmount() throws Exception {
        TransferRequest request = new TransferRequest(UUID.randomUUID(), "100000000002", new BigDecimal("0.00"), "INR", "Rent");

        mockMvc.perform(post("/api/v1/transfers")
                        .header("X-Idempotency-Key", "idemp-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(username = "customer@bank.com", roles = {"CUSTOMER"})
    @DisplayName("GET /api/v1/accounts/{accountId}/transactions returns paged history")
    void testGetAccountTransactions() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(accountSecurity.isAccountOwner(eq(accountId), any())).thenReturn(true);

        TransactionHistoryResponse item = new TransactionHistoryResponse(
                UUID.randomUUID(),
                "TXN-12345",
                TransactionType.P2P_TRANSFER,
                LegType.DEBIT,
                new BigDecimal("8000.00"),
                "INR",
                new BigDecimal("2000.00"),
                "Payment",
                Instant.now(),
                TransactionStatus.COMPLETED
        );

        Page<TransactionHistoryResponse> page = new PageImpl<>(List.of(item));
        when(transferService.getAccountTransactionHistory(eq(accountId), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/accounts/" + accountId + "/transactions")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].transactionReference").value("TXN-12345"))
                .andExpect(jsonPath("$.data.content[0].legType").value("DEBIT"));
    }
}
