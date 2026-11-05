package com.fintech.bank.repository;

import com.fintech.bank.entity.OutboxEvent;
import com.fintech.bank.entity.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Repository for Transactional Outbox pattern events.
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query("SELECT e FROM OutboxEvent e WHERE e.status = :status ORDER BY e.createdAt ASC")
    List<OutboxEvent> findPendingEvents(@Param("status") OutboxStatus status, Pageable pageable);

    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
