package com.fintech.bank.repository;

import com.fintech.bank.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for Account entity.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    @Query("SELECT a FROM Account a WHERE a.customerProfile.userAuth.id = :userId")
    List<Account> findAllByUserId(@Param("userId") UUID userId);

    @Query("SELECT a FROM Account a WHERE a.customerProfile.userAuth.id = :userId")
    Page<Account> findAllByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT a.customerProfile.userAuth.id FROM Account a WHERE a.id = :accountId")
    Optional<UUID> findOwnerUserIdByAccountId(@Param("accountId") UUID accountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWithLock(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberWithLock(@Param("accountNumber") String accountNumber);
}
