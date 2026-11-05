package com.fintech.bank.repository;

import com.fintech.bank.entity.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for CustomerProfile entity.
 */
@Repository
public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, UUID> {

    Optional<CustomerProfile> findByUserAuthId(UUID userAuthId);

    boolean existsByPhone(String phone);
}
