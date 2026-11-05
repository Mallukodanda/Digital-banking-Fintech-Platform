package com.fintech.bank.repository;

import com.fintech.bank.entity.Notification;
import com.fintech.bank.entity.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

/**
 * Spring Data JPA Repository for Notification entity.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByRecipientUserIdOrderByCreatedAtDesc(UUID recipientUserId, Pageable pageable);

    long countByRecipientUserIdAndStatus(UUID recipientUserId, NotificationStatus status);

    boolean existsByRecipientUserIdAndEventId(UUID recipientUserId, String eventId);

    @Modifying
    @Query("UPDATE Notification n SET n.status = 'READ', n.readAt = :now WHERE n.recipientUser.id = :userId AND n.status = 'UNREAD'")
    int markAllAsReadForUser(@Param("userId") UUID userId, @Param("now") Instant now);
}
