-- =============================================================================
-- Migration: V1__initial_banking_schema.sql
-- Engine: PostgreSQL 16
-- Description: Core schema definition with strict financial constraints
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. USERS AUTH TABLE
CREATE TABLE IF NOT EXISTS users_auth (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    failed_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_auth_email UNIQUE (email),
    CONSTRAINT chk_users_auth_role CHECK (role IN ('ROLE_CUSTOMER', 'ROLE_TELLER', 'ROLE_AUDITOR', 'ROLE_ADMIN')),
    CONSTRAINT chk_users_auth_status CHECK (status IN ('ACTIVE', 'LOCKED', 'SUSPENDED', 'DELETED'))
);

-- 2. CUSTOMER PROFILES TABLE
CREATE TABLE IF NOT EXISTS customer_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    kyc_status VARCHAR(30) NOT NULL DEFAULT 'UNVERIFIED',
    kyc_tier INT NOT NULL DEFAULT 1,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_customer_profiles_user_id UNIQUE (user_id),
    CONSTRAINT uq_customer_profiles_phone UNIQUE (phone),
    CONSTRAINT fk_customer_profiles_user FOREIGN KEY (user_id) REFERENCES users_auth(id) ON DELETE RESTRICT,
    CONSTRAINT chk_customer_kyc_status CHECK (kyc_status IN ('UNVERIFIED', 'PENDING_REVIEW', 'VERIFIED', 'REJECTED'))
);

-- 3. ACCOUNTS TABLE
CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    account_number VARCHAR(20) NOT NULL,
    account_type VARCHAR(30) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    cached_balance NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    hold_balance NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    overdraft_limit NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_accounts_account_number UNIQUE (account_number),
    CONSTRAINT fk_accounts_customer FOREIGN KEY (customer_id) REFERENCES customer_profiles(id) ON DELETE RESTRICT,
    CONSTRAINT chk_accounts_type CHECK (account_type IN ('CHECKING', 'SAVINGS', 'SETTLEMENT_SUSPENSE')),
    CONSTRAINT chk_accounts_status CHECK (status IN ('PENDING_ACTIVATION', 'ACTIVE', 'FROZEN', 'DORMANT', 'CLOSED')),
    CONSTRAINT chk_accounts_balance_limit CHECK (cached_balance + overdraft_limit >= hold_balance),
    CONSTRAINT chk_accounts_positive_hold CHECK (hold_balance >= 0),
    CONSTRAINT chk_accounts_positive_overdraft CHECK (overdraft_limit >= 0)
);

-- 4. BENEFICIARIES TABLE
CREATE TABLE IF NOT EXISTS beneficiaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    beneficiary_name VARCHAR(100) NOT NULL,
    destination_account_number VARCHAR(20) NOT NULL,
    nickname VARCHAR(50),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_beneficiaries_customer FOREIGN KEY (customer_id) REFERENCES customer_profiles(id) ON DELETE CASCADE,
    CONSTRAINT uq_beneficiaries_cust_acc UNIQUE (customer_id, destination_account_number)
);

-- 5. JOURNAL ENTRIES TABLE (Master Financial Ledger)
CREATE TABLE IF NOT EXISTS journal_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_reference VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    total_amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    description VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    initiated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_journal_entries_reference UNIQUE (transaction_reference),
    CONSTRAINT uq_journal_entries_idempotency UNIQUE (idempotency_key),
    CONSTRAINT chk_journal_type CHECK (transaction_type IN ('P2P_TRANSFER', 'DEPOSIT', 'WITHDRAWAL', 'FEE_CHARGE', 'REVERSAL')),
    CONSTRAINT chk_journal_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED', 'REVERSED')),
    CONSTRAINT chk_journal_positive_amount CHECK (total_amount > 0)
);

-- 6. TRANSACTION LEGS TABLE (Double-Entry Debit/Credit Lines)
CREATE TABLE IF NOT EXISTS transaction_legs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    journal_id UUID NOT NULL,
    account_id UUID NOT NULL,
    leg_type VARCHAR(10) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    balance_after NUMERIC(19, 4) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_legs_journal FOREIGN KEY (journal_id) REFERENCES journal_entries(id) ON DELETE RESTRICT,
    CONSTRAINT fk_legs_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE RESTRICT,
    CONSTRAINT chk_legs_type CHECK (leg_type IN ('DEBIT', 'CREDIT')),
    CONSTRAINT chk_legs_positive_amount CHECK (amount > 0)
);

-- 7. IDEMPOTENCY RECORDS TABLE
CREATE TABLE IF NOT EXISTS idempotency_records (
    idempotency_key VARCHAR(128) PRIMARY KEY,
    request_path VARCHAR(255) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_code INT,
    response_body TEXT,
    status VARCHAR(30) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 8. OUTBOX EVENTS TABLE (Transactional Outbox Pattern)
CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMPTZ
);

-- 9. FRAUD ALERTS TABLE
CREATE TABLE IF NOT EXISTS fraud_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    journal_id UUID,
    account_id UUID NOT NULL,
    rule_type VARCHAR(50) NOT NULL,
    risk_score INT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'FLAGGED',
    reviewer_notes TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fraud_journal FOREIGN KEY (journal_id) REFERENCES journal_entries(id) ON DELETE SET NULL,
    CONSTRAINT fk_fraud_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
    CONSTRAINT chk_fraud_score CHECK (risk_score BETWEEN 0 AND 100),
    CONSTRAINT chk_fraud_status CHECK (status IN ('FLAGGED', 'UNDER_REVIEW', 'DISMISSED', 'CONFIRMED_FRAUD'))
);

-- 10. AUDIT LOGS TABLE (Append-Only)
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id UUID,
    actor_role VARCHAR(50),
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id VARCHAR(64) NOT NULL,
    correlation_id VARCHAR(64),
    client_ip VARCHAR(45),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- INDEXES
CREATE INDEX IF NOT EXISTS idx_accounts_customer_id ON accounts(customer_id);
CREATE INDEX IF NOT EXISTS idx_accounts_account_number ON accounts(account_number);
CREATE INDEX IF NOT EXISTS idx_transaction_legs_account_created ON transaction_legs(account_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_transaction_legs_journal_id ON transaction_legs(journal_id);
CREATE INDEX IF NOT EXISTS idx_outbox_events_pending ON outbox_events(created_at ASC) WHERE status = 'PENDING';
CREATE INDEX IF NOT EXISTS idx_audit_logs_resource ON audit_logs(resource_type, resource_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_actor ON audit_logs(actor_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_fraud_alerts_status ON fraud_alerts(status, created_at DESC);
