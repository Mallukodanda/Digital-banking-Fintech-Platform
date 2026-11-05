# Database Architecture & Schema Documentation

## 1. Database Overview

The platform uses **PostgreSQL 16** managed via **Flyway Schema Migrations**. The database enforces strict ACID guarantees, foreign key constraints, high-performance B-tree indexes, and table-level constraints for financial data integrity.

```mermaid
erDiagram
    users_auth ||--|| customer_profiles : "has profile"
    customer_profiles ||--o{ accounts : "owns accounts"
    customer_profiles ||--o{ beneficiaries : "maintains beneficiaries"
    accounts ||--o{ transaction_legs : "participates in"
    journal_entries ||--|{ transaction_legs : "contains legs"
    accounts ||--o{ fraud_alerts : "flagged on"
    users_auth ||--o{ notifications : "receives"
    
    users_auth {
        uuid id PK
        varchar email UK
        varchar password_hash
        varchar role
        varchar status
        timestamp created_at
    }

    customer_profiles {
        uuid id PK
        uuid user_auth_id FK
        varchar first_name
        varchar last_name
        varchar phone_number
        timestamp created_at
    }

    accounts {
        uuid id PK
        uuid customer_profile_id FK
        varchar account_number UK
        varchar account_type
        varchar status
        varchar currency
        numeric cached_balance
        bigint version
        timestamp created_at
    }

    journal_entries {
        uuid id PK
        varchar transaction_reference UK
        varchar idempotency_key UK
        varchar transaction_type
        numeric amount
        varchar currency
        varchar status
        text description
        timestamp completed_at
        timestamp created_at
    }

    transaction_legs {
        uuid id PK
        uuid journal_entry_id FK
        uuid account_id FK
        varchar leg_type
        numeric amount
        varchar currency
        numeric running_balance
        timestamp created_at
    }

    idempotency_records {
        varchar idempotency_key PK
        varchar request_path
        varchar request_hash
        varchar status
        int response_code
        text response_body
        timestamp expires_at
        timestamp created_at
    }

    outbox_events {
        uuid id PK
        varchar aggregate_type
        varchar aggregate_id
        varchar event_type
        text payload
        varchar status
        int retry_count
        timestamp processed_at
        timestamp created_at
    }

    audit_logs {
        uuid id PK
        uuid actor_id
        varchar actor_username
        varchar actor_role
        varchar action
        varchar resource_type
        varchar resource_id
        varchar result
        varchar correlation_id
        varchar client_ip
        text metadata
        timestamp created_at
    }
```

---

## 2. Table Catalog & Financial Invariants

### A. `journal_entries` & `transaction_legs`
- **Zero-Sum Ledger Guarantee**: Every row in `journal_entries` has at least two associated rows in `transaction_legs` (one `DEBIT`, one `CREDIT`).
- **Precision**: All amounts use `NUMERIC(19, 4)` to store monetary values to four decimal places.
- **Indexes**:
  - `idx_journal_entries_reference` on `transaction_reference` (Unique B-tree).
  - `idx_journal_entries_idempotency` on `idempotency_key` (Unique B-tree).
  - `idx_transaction_legs_account_created` on `(account_id, created_at DESC)` for high-speed paginated statement queries.

### B. `accounts`
- **Optimistic Concurrency**: Uses `@Version` column (`bigint version`) to guard against lost updates in concurrent scenarios.
- **Pessimistic Locking**: Core transfer operations lock account rows using `SELECT ... FOR UPDATE` in deterministic sorted order.
- **Balance Cache**: `cached_balance` is updated atomically within the transaction and verified against ledger sum.

### C. `outbox_events`
- Stores serialized JSON payloads of domain events (`TRANSFER_COMPLETED`, `ACCOUNT_CREATED`, `FRAUD_ALERT_GENERATED`).
- Index: `idx_outbox_events_pending` on `(status, created_at)` for $< 1\text{ms}$ polling queries by the background worker.

### D. `idempotency_records`
- Enforces distributed locking for in-flight requests (`PROCESSING`) and caches the completed response (`COMPLETED`).
- TTL Cleanup: Records expire after 24 hours (`expires_at`).

### E. `audit_logs`
- Write-Once-Read-Many (**WORM**) append-only compliance trail.
- Indexes on `actor_id`, `action`, `correlation_id`, and `created_at` enable flexible JPA criteria query filtering for compliance audits.
