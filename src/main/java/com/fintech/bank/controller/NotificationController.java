package com.fintech.bank.controller;

import com.fintech.bank.dto.ApiResponse;
import com.fintech.bank.dto.NotificationResponse;
import com.fintech.bank.security.UserPrincipal;
import com.fintech.bank.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for retrieving customer notifications, reading unread counts,
 * and managing read/unread states.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Customer Notifications", description = "Endpoints for managing in-app customer alerts, notifications, and read statuses")
@SecurityRequirement(name = "BearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Get Customer Notifications", description = "Returns a paged list of notifications for the authenticated customer ordered by creation date")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<NotificationResponse> notifications = notificationService.getUserNotifications(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved successfully", notifications));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get Unread Notification Count", description = "Returns the count of unread notifications for badge counters")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal principal) {

        long count = notificationService.getUnreadCount(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Unread count retrieved successfully", Map.of("unreadCount", count)));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Mark Notification as Read", description = "Marks a specific notification as READ for the authenticated customer")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal UserPrincipal principal) {

        NotificationResponse response = notificationService.markAsRead(principal.getId(), notificationId);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", response));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark All Notifications as Read", description = "Marks all unread notifications as READ for the authenticated customer")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal principal) {

        int updatedCount = notificationService.markAllAsRead(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", Map.of("updatedCount", updatedCount)));
    }
}
