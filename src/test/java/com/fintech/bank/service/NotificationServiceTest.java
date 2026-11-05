package com.fintech.bank.service;

import com.fintech.bank.dto.NotificationResponse;
import com.fintech.bank.entity.*;
import com.fintech.bank.exception.UnauthorizedException;
import com.fintech.bank.repository.NotificationRepository;
import com.fintech.bank.repository.UserAuthRepository;
import com.fintech.bank.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Unit Tests")
@SuppressWarnings("null")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserAuthRepository userAuthRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private UserAuth user;
    private final UUID userId = UUID.randomUUID();
    private final UUID notificationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        user = new UserAuth("alice@bank.com", "hash", UserRole.ROLE_CUSTOMER);
        user.setId(userId);
    }

    @Nested
    @DisplayName("Notification Creation & Deduplication")
    class CreationTests {

        @Test
        @DisplayName("Should successfully create a new notification")
        void shouldCreateNotification() {
            when(notificationRepository.existsByRecipientUserIdAndEventId(userId, "evt-123")).thenReturn(false);
            when(userAuthRepository.findById(userId)).thenReturn(Optional.of(user));

            Notification notification = new Notification(
                    user,
                    NotificationType.TRANSFER_SUCCESSFUL,
                    "Transfer Received",
                    "You received 5000 INR",
                    NotificationChannel.IN_APP,
                    "TXN-123",
                    "evt-123"
            );
            notification.setId(notificationId);

            when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

            NotificationResponse response = notificationService.createNotification(
                    userId,
                    NotificationType.TRANSFER_SUCCESSFUL,
                    "Transfer Received",
                    "You received 5000 INR",
                    NotificationChannel.IN_APP,
                    "TXN-123",
                    "evt-123"
            );

            assertThat(response).isNotNull();
            assertThat(response.notificationType()).isEqualTo(NotificationType.TRANSFER_SUCCESSFUL);
            assertThat(response.title()).isEqualTo("Transfer Received");
            assertThat(response.status()).isEqualTo(NotificationStatus.UNREAD);
            verify(notificationRepository, times(1)).save(any(Notification.class));
        }

        @Test
        @DisplayName("Should suppress duplicate notification when eventId already exists for user")
        void shouldSuppressDuplicateNotification() {
            when(notificationRepository.existsByRecipientUserIdAndEventId(userId, "evt-123")).thenReturn(true);

            NotificationResponse response = notificationService.createNotification(
                    userId,
                    NotificationType.TRANSFER_SUCCESSFUL,
                    "Transfer Received",
                    "You received 5000 INR",
                    NotificationChannel.IN_APP,
                    "TXN-123",
                    "evt-123"
            );

            assertThat(response).isNull();
            verify(notificationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Read Management & Queries")
    class ReadManagementTests {

        @Test
        @DisplayName("Should retrieve paged notifications for user")
        void shouldRetrieveUserNotifications() {
            Notification notification = new Notification(user, NotificationType.FRAUD_ALERT, "Suspicious Activity", "Alert", NotificationChannel.IN_APP, "REF-1", "EVT-1");
            notification.setId(notificationId);

            Pageable pageable = PageRequest.of(0, 10);
            when(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId, pageable))
                    .thenReturn(new PageImpl<>(List.of(notification), pageable, 1));

            Page<NotificationResponse> page = notificationService.getUserNotifications(userId, pageable);

            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).title()).isEqualTo("Suspicious Activity");
        }

        @Test
        @DisplayName("Should return count of unread notifications")
        void shouldReturnUnreadCount() {
            when(notificationRepository.countByRecipientUserIdAndStatus(userId, NotificationStatus.UNREAD)).thenReturn(3L);

            long count = notificationService.getUnreadCount(userId);
            assertThat(count).isEqualTo(3L);
        }

        @Test
        @DisplayName("Should mark a single notification as read")
        void shouldMarkSingleAsRead() {
            Notification notification = new Notification(user, NotificationType.TRANSFER_SUCCESSFUL, "Title", "Msg", NotificationChannel.IN_APP, "REF", "EVT");
            notification.setId(notificationId);

            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

            NotificationResponse response = notificationService.markAsRead(userId, notificationId);

            assertThat(response.status()).isEqualTo(NotificationStatus.READ);
            assertThat(response.readAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when marking someone else's notification")
        void shouldRejectUnauthorizedMarkAsRead() {
            UUID otherUserId = UUID.randomUUID();
            Notification notification = new Notification(user, NotificationType.TRANSFER_SUCCESSFUL, "Title", "Msg", NotificationChannel.IN_APP, "REF", "EVT");
            notification.setId(notificationId);

            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

            assertThatThrownBy(() -> notificationService.markAsRead(otherUserId, notificationId))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("Should mark all unread notifications as read")
        void shouldMarkAllAsRead() {
            when(notificationRepository.markAllAsReadForUser(eq(userId), any(Instant.class))).thenReturn(4);

            int updated = notificationService.markAllAsRead(userId);
            assertThat(updated).isEqualTo(4);
        }
    }
}
