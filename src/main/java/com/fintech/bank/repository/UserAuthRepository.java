package com.fintech.bank.repository;

import com.fintech.bank.entity.UserAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for UserAuth entity.
 */
@Repository
public interface UserAuthRepository extends JpaRepository<UserAuth, UUID> {

    Optional<UserAuth> findByEmail(String email);

    boolean existsByEmail(String email);
}
