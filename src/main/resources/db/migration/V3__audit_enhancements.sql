-- V3__audit_enhancements.sql
-- Enhances audit_logs table with actor username, execution result, structured metadata, and multi-column indexes.

ALTER TABLE audit_logs 
    ADD COLUMN IF NOT EXISTS actor_username VARCHAR(100),
    ADD COLUMN IF NOT EXISTS result VARCHAR(30) NOT NULL DEFAULT 'SUCCESS',
    ADD COLUMN IF NOT EXISTS metadata TEXT;

-- Multi-column and filter indexes for high-throughput enterprise compliance searching
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs(action, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_result ON audit_logs(result, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_correlation_id ON audit_logs(correlation_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_actor_username ON audit_logs(actor_username, created_at DESC);
