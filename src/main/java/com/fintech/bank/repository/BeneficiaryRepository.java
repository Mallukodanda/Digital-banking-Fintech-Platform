package com.fintech.bank.repository;

import com.fintech.bank.entity.Beneficiary;
import com.fintech.bank.entity.BeneficiaryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for Beneficiary entity.
 */
@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, UUID> {

    @Query("SELECT b FROM Beneficiary b WHERE b.customerProfile.userAuth.id = :userId AND b.status = :status")
    List<Beneficiary> findAllByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") BeneficiaryStatus status);

    @Query("SELECT b FROM Beneficiary b WHERE b.customerProfile.userAuth.id = :userId AND b.status = :status")
    Page<Beneficiary> findAllByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") BeneficiaryStatus status, Pageable pageable);

    @Query("SELECT b FROM Beneficiary b WHERE b.customerProfile.id = :customerId AND b.destinationAccountNumber = :accountNumber AND b.status = 'ACTIVE'")
    Optional<Beneficiary> findActiveByCustomerIdAndAccountNumber(@Param("customerId") UUID customerId, @Param("accountNumber") String accountNumber);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN TRUE ELSE FALSE END FROM Beneficiary b WHERE b.customerProfile.id = :customerId AND b.destinationAccountNumber = :accountNumber AND b.status = 'ACTIVE'")
    boolean existsActiveByCustomerIdAndAccountNumber(@Param("customerId") UUID customerId, @Param("accountNumber") String accountNumber);

    @Query("SELECT b.customerProfile.userAuth.id FROM Beneficiary b WHERE b.id = :beneficiaryId")
    Optional<UUID> findOwnerUserIdByBeneficiaryId(@Param("beneficiaryId") UUID beneficiaryId);
}
