# Testing & Quality Engineering Documentation

## 1. Testing Pyramid & Multi-Layer Strategy

```
           / \
          /   \     Layer 10: End-to-End System Tests (E2E Lifecycle)
         /     \    Layer 9:  Kafka Consumer & Outbox Integration
        /       \   Layer 8:  Idempotency & Concurrent Transfers
       /         \  Layer 6-7: Transaction Rollback & DataJpa Repositories
      /           \ Layer 4-5: REST API & Security Authorization WebMvc
     /             \Layer 1-3: Unit & Domain Service Tests (Mockito, MoneyUtils)
    /_______________\
```

---

## 2. Test Layer Catalog (29 Test Suites)

| Layer | Focus Area | Test Classes | Key Validations |
| :---: | :--- | :--- | :--- |
| **1** | Domain & Utilities | `MoneyUtilsTest`, `AccountNumberGeneratorTest`, `JwtTokenProviderTest`, `FraudRuleEngineTest`, `DistributedRequestContextTest` | Four-decimal rounding, Token minting, Heuristic rules, MDC context. |
| **2** | Data JPA Layer | `DataJpaLayerRepositoryTest` | Foreign keys, JPA queries, unique constraints on H2/Postgres. |
| **3** | Domain Services | `AccountServiceTest`, `AuditServiceTest`, `AuthServiceTest`, `BeneficiaryServiceTest`, `CacheBehaviorTest`, `NotificationServiceTest`, `FraudDetectionServiceTest` | Business rule violations, cache invalidations, audit logging. |
| **4** | REST Controllers | `AccountControllerTest`, `AuthControllerTest`, `BeneficiaryControllerTest`, `TransferControllerTest`, `NotificationControllerTest`, `AdminAccountControllerTest`, `AdminFraudControllerTest`, `AdminAuditLogControllerTest` | Request validation (`@Valid`), HTTP status mapping, DTO serialization. |
| **5** | Security & Auth | `SecurityAuthorizationWebTest`, `ValidationFailureTest` | 401 Unauthorized, 403 Forbidden, RBAC permissions. |
| **6** | Transactions | `TransactionRollbackAndIntegrityTest` | Atomic rollback on runtime exception, zero financial leakage. |
| **7** | Concurrency | `ConcurrentTransferTest` | Multi-threaded transfers, deadlock prevention, balance conservation. |
| **8** | Idempotency | `TransferIdempotencyTest`, `IdempotencyServiceTest` | Replay deduplication, hash conflicts, lock expiration. |
| **9** | Kafka Streaming | `KafkaConsumerIntegrationTest`, `OutboxPublisherTest` | Outbox polling, topic routing, consumer error recovery. |
| **10**| E2E Lifecycle | `EndToEndBankingLifecycleTest`, `EnterpriseSystemIntegrationTest` | Full Customer & Admin lifecycle execution across all services. |

---

## 3. Running the Test Suites

```bash
# Run all unit and integration tests
mvn clean test

# Run a specific test suite
mvn test -Dtest=EnterpriseSystemIntegrationTest

# Run JaCoCo coverage verification (enforces >= 80% instruction coverage)
mvn jacoco:report jacoco:check
```
Coverage reports will be generated at `target/site/jacoco/index.html`.
