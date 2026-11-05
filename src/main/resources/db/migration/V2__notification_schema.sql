-- =============================================================================
-- Migration: V2__notification_schema.sql
-- Description: In-App & Multi-channel notifications schema with deduplication
-- =============================================================================

CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_user_id UUID NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    channel VARCHAR(30) NOT NULL DEFAULT 'IN_APP',
    status VARCHAR(30) NOT NULL DEFAULT 'UNREAD',
    reference_id VARCHAR(100),
    event_id VARCHAR(100),
    read_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_user FOREIGN KEY (recipient_user_id) REFERENCES users_auth(id) ON DELETE CASCADE,
    CONSTRAINT chk_notification_status CHECK (status IN ('UNREAD', 'READ', 'ARCHIVED')),
    CONSTRAINT chk_notification_channel CHECK (channel IN ('IN_APP', 'SMS', 'EMAIL', 'PUSH')),
    CONSTRAINT chk_notification_type CHECK (notification_type IN ('TRANSFER_SUCCESSFUL', 'TRANSFER_FAILED', 'ACCOUNT_FROZEN', 'FRAUD_ALERT', 'SECURITY_NOTIFICATION')),
    CONSTRAINT uq_notifications_user_event UNIQUE (recipient_user_id, event_id)
);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_status ON notifications(recipient_user_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_event_id ON notifications(event_id);
