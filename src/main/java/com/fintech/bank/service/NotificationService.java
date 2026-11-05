package com.fintech.bank.service;

import com.fintech.bank.dto.NotificationResponse;
import com.fintech.bank.entity.NotificationChannel;
import com.fintech.bank.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for customer notification management.
 */
public interface NotificationService {

    /**
     * Creates and persists a notification with deduplication checks.
     */
    NotificationResponse createNotification(UUID recipientUserId, NotificationType type, String title,
                                            String message, NotificationChannel channel, String referenceId, String eventId);

    /**
     * Retrieves paged notifications for the authenticated user.
     */
    Page<NotificationResponse> getUserNotifications(UUID userId, Pageable pageable);

    /**
     * Counts unread notifications for badge display.
     */
    long getUnreadCount(UUID userId);

    /**
     * Marks a specific notification as read.
     */
    NotificationResponse markAsRead(UUID userId, UUID notificationId);

    /**
     * Marks all unread notifications for a user as read.
     */
    int markAllAsRead(UUID userId);
}
