package com.fintech.bank.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Notification Entity mapping notifications table.
 */
@Entity
@Table(name = "notifications")
public class Notification extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private UserAuth recipientUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 30)
    private NotificationChannel channel = NotificationChannel.IN_APP;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private NotificationStatus status = NotificationStatus.UNREAD;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(name = "event_id", length = 100)
    private String eventId;

    @Column(name = "read_at")
    private Instant readAt;

    public Notification() {
    }

    public Notification(UserAuth recipientUser, NotificationType notificationType, String title, String message,
                        NotificationChannel channel, String referenceId, String eventId) {
        this.recipientUser = recipientUser;
        this.notificationType = notificationType;
        this.title = title;
        this.message = message;
        this.channel = channel != null ? channel : NotificationChannel.IN_APP;
        this.status = NotificationStatus.UNREAD;
        this.referenceId = referenceId;
        this.eventId = eventId;
    }

    public void markAsRead() {
        this.status = NotificationStatus.READ;
        this.readAt = Instant.now();
    }

    public UserAuth getRecipientUser() {
        return recipientUser;
    }

    public void setRecipientUser(UserAuth recipientUser) {
        this.recipientUser = recipientUser;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }
}
