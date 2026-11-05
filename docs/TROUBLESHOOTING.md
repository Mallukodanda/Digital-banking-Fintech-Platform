# Troubleshooting & Operational Runbook

## 1. Common Operational Issues & Remediation

### A. Database Connection Pool Saturation
- **Symptom**: HTTP 500/503 responses with `SQLTransientConnectionException: HikariPool-1 - Connection is not available`.
- **Diagnosis**: Check active connections:
  ```sql
  SELECT count(*), state FROM pg_stat_activity GROUP BY state;
  ```
- **Remediation**:
  1. Inspect long-running blocking queries in PostgreSQL.
  2. Increase `app.datasource.hikari.maximum-pool-size` in `application.yml` (default: 20).

### B. Pessimistic Lock Contention on High-Velocity Accounts
- **Symptom**: `PessimisticLockException: Lock acquisition timed out after 5000ms`.
- **Diagnosis**: Identify locked rows:
  ```sql
  SELECT blocked_locks.pid AS blocked_pid, blocking_locks.pid AS blocking_pid, blocked_activity.query AS blocked_statement
  FROM pg_catalog.pg_locks blocked_locks
  JOIN pg_catalog.pg_locks blocking_locks ON blocking_locks.locktype = blocked_locks.locktype
  JOIN pg_catalog.pg_stat_activity blocked_activity ON blocked_activity.pid = blocked_locks.pid;
  ```
- **Remediation**: Client should retry with exponential backoff using the same `X-Idempotency-Key`.

### C. Outbox Backlog / Kafka Producer Failure
- **Symptom**: `bank_outbox_pending_gauge` in Grafana increases steadily ($> 100$).
- **Diagnosis**: Check Kafka container logs:
  ```bash
  docker compose logs kafka
  docker compose logs backend | grep "OutboxPublisherImpl"
  ```
- **Remediation**:
  1. Ensure Kafka container is running and healthy on port `9092`.
  2. Once Kafka reconnects, the background worker automatically drains pending outbox events.

### D. 401 Token Refresh Loop on Frontend
- **Symptom**: React frontend repeatedly redirects to `/login`.
- **Remediation**:
  1. Verify `APP_JWT_SECRET` matches across restarts.
  2. Clear browser LocalStorage (`localStorage.clear()`).
