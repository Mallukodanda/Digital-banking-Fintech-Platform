package com.fintech.bank.controller;

import com.fintech.bank.config.SecurityConfig;
import com.fintech.bank.dto.NotificationResponse;
import com.fintech.bank.entity.NotificationChannel;
import com.fintech.bank.entity.NotificationStatus;
import com.fintech.bank.entity.NotificationType;
import com.fintech.bank.security.*;
import com.fintech.bank.service.NotificationService;
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

@WebMvcTest(NotificationController.class)
@Import({SecurityConfig.class, CorrelationIdFilter.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@DisplayName("NotificationController WebMvc Tests")
@SuppressWarnings("null")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "customer@bank.com", roles = {"CUSTOMER"})
    @DisplayName("GET /api/v1/notifications returns paged notifications")
    void testGetNotifications() throws Exception {
        NotificationResponse item = new NotificationResponse(
                UUID.randomUUID(),
                NotificationType.TRANSFER_SUCCESSFUL,
                "Transfer Successful",
                "You sent 8000 INR",
                NotificationChannel.IN_APP,
                NotificationStatus.UNREAD,
                "TXN-123",
                null,
                Instant.now()
        );

        Page<NotificationResponse> page = new PageImpl<>(List.of(item));
        when(notificationService.getUserNotifications(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/notifications")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].title").value("Transfer Successful"));
    }

    @Test
    @WithMockUser(username = "customer@bank.com", roles = {"CUSTOMER"})
    @DisplayName("GET /api/v1/notifications/unread-count returns count")
    void testGetUnreadCount() throws Exception {
        when(notificationService.getUnreadCount(any())).thenReturn(5L);

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.unreadCount").value(5));
    }

    @Test
    @WithMockUser(username = "customer@bank.com", roles = {"CUSTOMER"})
    @DisplayName("PATCH /api/v1/notifications/{id}/read marks notification as read")
    void testMarkAsRead() throws Exception {
        UUID notificationId = UUID.randomUUID();
        NotificationResponse item = new NotificationResponse(
                notificationId,
                NotificationType.TRANSFER_SUCCESSFUL,
                "Transfer Successful",
                "You sent 8000 INR",
                NotificationChannel.IN_APP,
                NotificationStatus.READ,
                "TXN-123",
                Instant.now(),
                Instant.now()
        );

        when(notificationService.markAsRead(any(), eq(notificationId))).thenReturn(item);

        mockMvc.perform(patch("/api/v1/notifications/" + notificationId + "/read")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("READ"));
    }

    @Test
    @WithMockUser(username = "customer@bank.com", roles = {"CUSTOMER"})
    @DisplayName("PATCH /api/v1/notifications/read-all marks all notifications as read")
    void testMarkAllAsRead() throws Exception {
        when(notificationService.markAllAsRead(any())).thenReturn(3);

        mockMvc.perform(patch("/api/v1/notifications/read-all")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.updatedCount").value(3));
    }
}
