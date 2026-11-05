package com.fintech.bank.repository;

import com.fintech.bank.entity.TransactionLeg;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Repository for immutable double-entry TransactionLeg entity.
 */
@Repository
public interface TransactionLegRepository extends JpaRepository<TransactionLeg, UUID> {

    @Query("SELECT leg FROM TransactionLeg leg WHERE leg.account.id = :accountId ORDER BY leg.createdAt DESC")
    List<TransactionLeg> findByAccountIdOrderByCreatedAtDesc(@Param("accountId") UUID accountId);

    @Query("SELECT leg FROM TransactionLeg leg JOIN FETCH leg.journalEntry WHERE leg.account.id = :accountId ORDER BY leg.createdAt DESC")
    Page<TransactionLeg> findByAccountIdWithJournalEntry(@Param("accountId") UUID accountId, Pageable pageable);
}
