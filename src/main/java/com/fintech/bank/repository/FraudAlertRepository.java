package com.fintech.bank.repository;

import com.fintech.bank.entity.FraudAlert;
import com.fintech.bank.entity.FraudStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Repository for FraudAlert entity.
 */
@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, UUID> {

    List<FraudAlert> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    Page<FraudAlert> findByStatusOrderByCreatedAtDesc(FraudStatus status, Pageable pageable);

    Page<FraudAlert> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(FraudStatus status);
}
