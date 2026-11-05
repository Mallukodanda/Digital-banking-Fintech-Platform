package com.fintech.bank.dto;

import com.fintech.bank.entity.NotificationChannel;
import com.fintech.bank.entity.NotificationStatus;
import com.fintech.bank.entity.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for customer notifications.
 */
@Schema(description = "Customer notification line item")
public record NotificationResponse(
        UUID id,
        NotificationType notificationType,
        String title,
        String message,
        NotificationChannel channel,
        NotificationStatus status,
        String referenceId,
        Instant readAt,
        Instant createdAt
) {
}
