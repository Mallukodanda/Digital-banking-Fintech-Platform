package com.fintech.bank.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables Spring Data JPA Auditing for automated creation and update timestamps.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
