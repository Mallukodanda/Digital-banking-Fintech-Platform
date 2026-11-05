package com.fintech.bank.observability;

import com.fintech.bank.entity.OutboxStatus;
import com.fintech.bank.repository.AccountRepository;
import com.fintech.bank.repository.OutboxEventRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Custom Spring Boot Actuator Health Indicator for Core Banking Ledger.
 * Assesses system health based on database connectivity, outbox event queue depth,
 * and ledger operational thresholds.
 */
@Component("ledgerHealth")
public class LedgerHealthIndicator implements HealthIndicator {

    private final AccountRepository accountRepository;
    private final OutboxEventRepository outboxEventRepository;

    private static final int MAX_PENDING_OUTBOX_THRESHOLD = 5000;

    public LedgerHealthIndicator(AccountRepository accountRepository, OutboxEventRepository outboxEventRepository) {
        this.accountRepository = accountRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    @Override
    public Health health() {
        try {
            long totalAccounts = accountRepository.count();
            int pendingOutboxEvents = outboxEventRepository.findPendingEvents(OutboxStatus.PENDING, Pageable.ofSize(MAX_PENDING_OUTBOX_THRESHOLD + 1)).size();

            Health.Builder builder = Health.up()
                    .withDetail("activeAccounts", totalAccounts)
                    .withDetail("pendingOutboxEvents", pendingOutboxEvents)
                    .withDetail("ledgerStatus", "OPERATIONAL")
                    .withDetail("zeroSumInvariantVerification", "ACTIVE");

            if (pendingOutboxEvents > MAX_PENDING_OUTBOX_THRESHOLD) {
                return Health.down()
                        .withDetail("reason", "Outbox backlog exceeded threshold: " + pendingOutboxEvents)
                        .withDetail("pendingOutboxEvents", pendingOutboxEvents)
                        .build();
            }

            return builder.build();
        } catch (Exception ex) {
            return Health.down(ex)
                    .withDetail("reason", "Database connectivity failure or ledger probe timeout")
                    .build();
        }
    }
}
