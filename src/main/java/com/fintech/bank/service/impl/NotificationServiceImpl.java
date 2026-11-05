package com.fintech.bank.service.impl;

import com.fintech.bank.dto.NotificationResponse;
import com.fintech.bank.entity.*;
import com.fintech.bank.exception.ResourceNotFoundException;
import com.fintech.bank.exception.UnauthorizedException;
import com.fintech.bank.repository.NotificationRepository;
import com.fintech.bank.repository.UserAuthRepository;
import com.fintech.bank.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Production implementation of NotificationService.
 * Handles in-app notifications, event deduplication, and read state transitions.
 */
@Service
@SuppressWarnings("null")
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final UserAuthRepository userAuthRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   UserAuthRepository userAuthRepository) {
        this.notificationRepository = notificationRepository;
        this.userAuthRepository = userAuthRepository;
    }

    @Override
    @Transactional
    public NotificationResponse createNotification(UUID recipientUserId, NotificationType type, String title,
                                                   String message, NotificationChannel channel, String referenceId, String eventId) {

        // Deduplication check: Do not re-create notification for identical event ID and recipient
        if (eventId != null && notificationRepository.existsByRecipientUserIdAndEventId(recipientUserId, eventId)) {
            log.warn("Duplicate notification suppressed for userId: {} and eventId: {}", recipientUserId, eventId);
            return null;
        }

        UserAuth recipient = userAuthRepository.findById(recipientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + recipientUserId));

        Notification notification = new Notification(
                recipient,
                type,
                title,
                message,
                channel != null ? channel : NotificationChannel.IN_APP,
                referenceId,
                eventId
        );

        Notification saved = notificationRepository.save(notification);
        log.info("Created {} notification id: {} for user: {} [EventId: {}]",
                type, saved.getId(), recipientUserId, eventId);

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(UUID userId, Pageable pageable) {
        return notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByRecipientUserIdAndStatus(userId, NotificationStatus.UNREAD);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + notificationId));

        if (!notification.getRecipientUser().getId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to modify this notification");
        }

        notification.markAsRead();
        Notification updated = notificationRepository.save(notification);
        log.debug("Marked notification '{}' as READ for user '{}'", notificationId, userId);

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public int markAllAsRead(UUID userId) {
        int count = notificationRepository.markAllAsReadForUser(userId, Instant.now());
        log.info("Marked {} unread notifications as READ for user '{}'", count, userId);
        return count;
    }

    private NotificationResponse mapToResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getNotificationType(),
                n.getTitle(),
                n.getMessage(),
                n.getChannel(),
                n.getStatus(),
                n.getReferenceId(),
                n.getReadAt(),
                n.getCreatedAt()
        );
    }
}
