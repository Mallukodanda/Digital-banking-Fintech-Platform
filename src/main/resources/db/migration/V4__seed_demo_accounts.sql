-- =============================================================================
-- Migration: V4__seed_demo_accounts.sql
-- Description: Pre-seeds default Customer, Counterparty, Beneficiary, and Admin accounts
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. Insert Demo Customer: customer@bank.com / Customer@123
INSERT INTO users_auth (id, email, password_hash, role, status, failed_attempts, version, created_at, updated_at)
VALUES (
    'a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d',
    'customer@bank.com',
    crypt('Customer@123', gen_salt('bf', 12)),
    'ROLE_CUSTOMER',
    'ACTIVE',
    0,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (email) DO NOTHING;

-- 2. Insert Customer Profile
INSERT INTO customer_profiles (id, user_id, first_name, last_name, phone, kyc_status, kyc_tier, version, created_at, updated_at)
VALUES (
    'b2c3d4e5-f6a7-8b9c-0d1e-2f3a4b5c6d7e',
    'a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d',
    'Alex',
    'Mercer',
    '+919876543210',
    'VERIFIED',
    2,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (user_id) DO NOTHING;

-- 3. Insert Primary Checking Account (Account Number: 100000000001, Balance: ₹50,000.00)
INSERT INTO accounts (id, customer_id, account_number, account_type, currency, status, cached_balance, hold_balance, overdraft_limit, version, created_at, updated_at)
VALUES (
    'c3d4e5f6-a7b8-9c0d-1e2f-3a4b5c6d7e8f',
    'b2c3d4e5-f6a7-8b9c-0d1e-2f3a4b5c6d7e',
    '100000000001',
    'CHECKING',
    'INR',
    'ACTIVE',
    50000.0000,
    0.0000,
    0.0000,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (account_number) DO NOTHING;

-- 4. Insert Second Customer (Counterparty for P2P transfers): sarah@bank.com / Sarah@123
INSERT INTO users_auth (id, email, password_hash, role, status, failed_attempts, version, created_at, updated_at)
VALUES (
    'd4e5f6a7-b8c9-0d1e-2f3a-4b5c6d7e8f9a',
    'sarah@bank.com',
    crypt('Sarah@123', gen_salt('bf', 12)),
    'ROLE_CUSTOMER',
    'ACTIVE',
    0,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (email) DO NOTHING;

INSERT INTO customer_profiles (id, user_id, first_name, last_name, phone, kyc_status, kyc_tier, version, created_at, updated_at)
VALUES (
    'e5f6a7b8-c9d0-1e2f-3a4b-5c6d7e8f9a0b',
    'd4e5f6a7-b8c9-0d1e-2f3a-4b5c6d7e8f9a',
    'Sarah',
    'Connor',
    '+919876543211',
    'VERIFIED',
    2,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (user_id) DO NOTHING;

INSERT INTO accounts (id, customer_id, account_number, account_type, currency, status, cached_balance, hold_balance, overdraft_limit, version, created_at, updated_at)
VALUES (
    'f6a7b8c9-d0e1-2f3a-4b5c-6d7e8f9a0b1c',
    'e5f6a7b8-c9d0-1e2f-3a4b-5c6d7e8f9a0b',
    '100000000002',
    'CHECKING',
    'INR',
    'ACTIVE',
    10000.0000,
    0.0000,
    0.0000,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (account_number) DO NOTHING;

-- 5. Insert Sarah as a Saved Beneficiary for customer@bank.com
INSERT INTO beneficiaries (id, customer_id, beneficiary_name, destination_account_number, nickname, status, version, created_at, updated_at)
VALUES (
    'a7b8c9d0-e1f2-3a4b-5c6d-7e8f9a0b1c2d',
    'b2c3d4e5-f6a7-8b9c-0d1e-2f3a4b5c6d7e',
    'Sarah Connor',
    '100000000002',
    'Sarah Friend',
    'ACTIVE',
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (customer_id, destination_account_number) DO NOTHING;

-- 6. Insert Demo Admin: admin@bank.com / Admin@123
INSERT INTO users_auth (id, email, password_hash, role, status, failed_attempts, version, created_at, updated_at)
VALUES (
    'b8c9d0e1-f2a3-4b5c-6d7e-8f9a0b1c2d3e',
    'admin@bank.com',
    crypt('Admin@123', gen_salt('bf', 12)),
    'ROLE_ADMIN',
    'ACTIVE',
    0,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (email) DO NOTHING;

-- 7. Insert Admin Profile
INSERT INTO customer_profiles (id, user_id, first_name, last_name, phone, kyc_status, kyc_tier, version, created_at, updated_at)
VALUES (
    'c9d0e1f2-a3b4-5c6d-7e8f-9a0b1c2d3e4f',
    'b8c9d0e1-f2a3-4b5c-6d7e-8f9a0b1c2d3e',
    'System',
    'Administrator',
    '+919999999999',
    'VERIFIED',
    3,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (user_id) DO NOTHING;

