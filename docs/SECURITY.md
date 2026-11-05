# Security Architecture & Cryptographic Governance

## 1. Security Overview

The platform implements a **Defense-in-Depth** security posture compliant with modern financial engineering principles:

```mermaid
flowchart TD
    Request[Incoming Request] --> TLS[TLS 1.3 Termination]
    TLS --> RateLimit[Rate Limiter & Ingress Guard]
    RateLimit --> JWT[JWT Authentication Filter]
    JWT --> RBAC[Role-Based Access Control (RBAC)]
    RBAC --> Idemp[Cryptographic Idempotency Validator]
    Idemp --> Audit[WORM Audit Logger]
```

---

## 2. Core Security Mechanisms

### A. Authentication & JWT Rotation
- **Access Tokens**: Short-lived (15 minutes), signed with HMAC-SHA256 (`HS256`) using a 256-bit cryptographic secret.
- **Refresh Tokens**: Long-lived (7 days), stored securely and rotated on usage.
- **Client Auto-Refresh**: Axios interceptor in the React frontend catches `401 Unauthorized` responses and silently queues failed requests while acquiring a fresh access token.

### B. Role-Based Access Control (RBAC)
- `ROLE_CUSTOMER`: Scoped strictly to resources owned by their authenticated `user_auth_id` (enforced via database lookup verification before ledger execution).
- `ROLE_ADMIN`: Authorized to freeze accounts, review global fraud alerts, and inspect immutable audit logs.
- Strict Method Security: Spring `@PreAuthorize("hasRole('ADMIN')")` secures administrative controllers.

### C. Password Hashing & Sensitive Data Protection
- User credentials are encrypted using **BCrypt** with work factor 12.
- Plaintext passwords are never logged, returned in DTOs, or persisted.

### D. Zero-Secret Architecture
- No API keys, passwords, or JWT secrets are stored in Git.
- Configuration templates are provided via [`.env.example`](file:///c:/Users/mallu/OneDrive/Documents/Digital-banking-Fintech-Platform/Digital-banking-Fintech-Platform/.env.example) and injected via environment variables at runtime.

### E. Distributed Tracing & Audit Logging
- **`X-Correlation-Id`**: Propagated via SLF4J MDC across threads and asynchronous Kafka event headers.
- **WORM Audit Trail**: Write-Once-Read-Many logging records all authentication failures, funds transfers, and administrative actions with IP addresses and actor IDs.
