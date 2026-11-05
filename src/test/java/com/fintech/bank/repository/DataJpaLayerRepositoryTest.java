package com.fintech.bank.repository;

import com.fintech.bank.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Layer 2: JPA Repository & Data Access Tests")
class DataJpaLayerRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Test
    @DisplayName("Should persist and lock account by ID with pessimistic write lock")
    void testAccountPessimisticLockQuery() {
        UserAuth user = new UserAuth("repo_test@bank.com", "hash123", UserRole.ROLE_CUSTOMER);
        entityManager.persist(user);

        CustomerProfile profile = new CustomerProfile(user, "Test", "User", "+19999999999");
        entityManager.persist(profile);

        Account account = new Account(profile, "999000000001", AccountType.CHECKING, "INR");
        account.setCachedBalance(new BigDecimal("50000.0000"));
        entityManager.persist(account);
        entityManager.flush();

        Optional<Account> lockedAccount = accountRepository.findByIdWithLock(account.getId());

        assertThat(lockedAccount).isPresent();
        assertThat(lockedAccount.get().getAccountNumber()).isEqualTo("999000000001");
        assertThat(lockedAccount.get().getCachedBalance()).isEqualByComparingTo(new BigDecimal("50000.0000"));
    }

    @Test
    @DisplayName("Should find journal entry and associated transaction legs by idempotency key")
    void testFindJournalEntryByIdempotencyKey() {
        UserAuth user = new UserAuth("ledger@bank.com", "hash", UserRole.ROLE_CUSTOMER);
        entityManager.persist(user);
        CustomerProfile profile = new CustomerProfile(user, "Ledger", "Owner", "+18888888888");
        entityManager.persist(profile);

        Account src = new Account(profile, "888000000001", AccountType.CHECKING, "INR");
        Account dst = new Account(profile, "888000000002", AccountType.SAVINGS, "INR");
        entityManager.persist(src);
        entityManager.persist(dst);

        String idempKey = UUID.randomUUID().toString();
        JournalEntry entry = new JournalEntry(
                "TXN-REPO-001", idempKey, TransactionType.P2P_TRANSFER,
                new BigDecimal("1500.0000"), "INR", "Repository layer test"
        );
        entry.setStatus(TransactionStatus.COMPLETED);

        TransactionLeg debit = new TransactionLeg(entry, src, LegType.DEBIT, new BigDecimal("1500.0000"), "INR", new BigDecimal("8500.0000"));
        TransactionLeg credit = new TransactionLeg(entry, dst, LegType.CREDIT, new BigDecimal("1500.0000"), "INR", new BigDecimal("1500.0000"));
        entry.addLeg(debit);
        entry.addLeg(credit);

        entityManager.persist(entry);
        entityManager.flush();

        Optional<JournalEntry> retrieved = journalEntryRepository.findByIdempotencyKey(idempKey);

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getTransactionReference()).isEqualTo("TXN-REPO-001");
        assertThat(retrieved.get().getLegs()).hasSize(2);
    }

    @Test
    @DisplayName("Should safely delete expired idempotency records in bulk")
    void testDeleteExpiredIdempotencyRecords() {
        IdempotencyRecord expired1 = new IdempotencyRecord(
                "key-expired-1", "/api/v1/transfers", "hash1", Instant.now().minusSeconds(7200)
        );
        expired1.setStatus(IdempotencyStatus.COMPLETED);

        IdempotencyRecord active = new IdempotencyRecord(
                "key-active-1", "/api/v1/transfers", "hash2", Instant.now().plusSeconds(7200)
        );
        active.setStatus(IdempotencyStatus.COMPLETED);

        entityManager.persist(expired1);
        entityManager.persist(active);
        entityManager.flush();

        int deletedCount = idempotencyRecordRepository.deleteExpiredRecords(Instant.now());

        assertThat(deletedCount).isGreaterThanOrEqualTo(1);
        assertThat(idempotencyRecordRepository.findById("key-expired-1")).isEmpty();
        assertThat(idempotencyRecordRepository.findById("key-active-1")).isPresent();
    }
}
