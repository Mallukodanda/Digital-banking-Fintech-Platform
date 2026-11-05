# REST API Specification & Endpoint Catalog

## 1. Authentication & Global Headers

All mutating endpoints require an `X-Correlation-Id` for distributed tracing and an `X-Idempotency-Key` for state-mutating requests.

| Header Name | Required | Description | Example |
| :--- | :---: | :--- | :--- |
| `Authorization` | Yes (Protected) | Bearer JWT Access Token | `Bearer eyJhbGciOiJIUzI1NiIsIn...` |
| `X-Correlation-Id` | Recommended | UUID/string for end-to-end distributed tracing | `corr-9f82a1b4-7c3e` |
| `X-Idempotency-Key`| Yes (Mutating) | Unique key preventing duplicate financial execution | `idemp-9028-48201` |
| `Content-Type` | Yes | Request payload format | `application/json` |

---

## 2. API Endpoint Matrix

### A. Authentication Endpoints (`/api/v1/auth`)

#### 1. User Registration
- **Endpoint**: `POST /api/v1/auth/register`
- **Access**: Public
- **Request Body**:
  ```json
  {
    "email": "customer@bank.com",
    "password": "SecurePassword123!",
    "firstName": "Alice",
    "lastName": "Smith",
    "phoneNumber": "+919876543210"
  }
  ```
- **Response `201 Created`**:
  ```json
  {
    "userId": "d4e21a80-0001-4b1e-9201-38e91823ab01",
    "email": "customer@bank.com",
    "role": "ROLE_CUSTOMER",
    "message": "User registered successfully"
  }
  ```

#### 2. User Login
- **Endpoint**: `POST /api/v1/auth/login`
- **Access**: Public
- **Request Body**:
  ```json
  {
    "email": "customer@bank.com",
    "password": "SecurePassword123!"
  }
  ```
- **Response `200 OK`**:
  ```json
  {
    "accessToken": "eyJhbGciOiJIUzI1NiIsIn...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsIn...",
    "tokenType": "Bearer",
    "expiresInMs": 900000,
    "user": {
      "id": "d4e21a80-0001-4b1e-9201-38e91823ab01",
      "email": "customer@bank.com",
      "role": "ROLE_CUSTOMER",
      "firstName": "Alice",
      "lastName": "Smith"
    }
  }
  ```

---

### B. Accounts Endpoints (`/api/v1/accounts`)

#### 1. Get Customer Accounts
- **Endpoint**: `GET /api/v1/accounts`
- **Access**: `ROLE_CUSTOMER`, `ROLE_ADMIN`
- **Response `200 OK`**:
  ```json
  [
    {
      "id": "a1b2c3d4-0000-0000-0000-000000000001",
      "accountNumber": "100000000001",
      "accountType": "CHECKING",
      "status": "ACTIVE",
      "currency": "INR",
      "balance": 50000.0000,
      "createdAt": "2026-10-01T10:00:00Z"
    }
  ]
  ```

---

### C. Funds Transfer Endpoints (`/api/v1/transfers`)

#### 1. Execute Funds Transfer
- **Endpoint**: `POST /api/v1/transfers`
- **Access**: `ROLE_CUSTOMER`
- **Headers**: `X-Idempotency-Key: idemp-uuid-123`
- **Request Body**:
  ```json
  {
    "sourceAccountId": "a1b2c3d4-0000-0000-0000-000000000001",
    "destinationAccountNumber": "100000000002",
    "amount": 25000.00,
    "currency": "INR",
    "description": "Invoice Payment #104"
  }
  ```
- **Response `200 OK`**:
  ```json
  {
    "transactionId": "f9e8d7c6-0000-0000-0000-000000000099",
    "transactionReference": "TXN-1712567890-A8F2BC1D",
    "idempotencyKey": "idemp-uuid-123",
    "sourceAccountNumber": "100000000001",
    "destinationAccountNumber": "100000000002",
    "amount": 25000.0000,
    "currency": "INR",
    "status": "COMPLETED",
    "sourceRemainingBalance": 25000.0000,
    "completedAt": "2026-10-08T18:45:00Z"
  }
  ```

#### 2. Get Transaction History & Statements
- **Endpoint**: `GET /api/v1/transfers/history?accountId=...&page=0&size=10`
- **Access**: `ROLE_CUSTOMER`, `ROLE_ADMIN`
- **Response `200 OK`**: Paginated transaction statement with running balances.

---

### D. Beneficiary Endpoints (`/api/v1/beneficiaries`)

- `GET /api/v1/beneficiaries`: List customer's saved beneficiaries.
- `POST /api/v1/beneficiaries`: Add and validate a new beneficiary account.
- `DELETE /api/v1/beneficiaries/{id}`: Remove a saved beneficiary.

---

### E. Admin Endpoints (`/api/v1/admin/**`)

- `GET /api/v1/admin/accounts`: Global paginated accounts across all customers.
- `PUT /api/v1/admin/accounts/{id}/status`: Freeze/Unfreeze account status.
- `GET /api/v1/admin/fraud/alerts`: Real-time queue of flagged high-risk transactions.
- `PUT /api/v1/admin/fraud/alerts/{id}/review`: Review and resolve alert (`CONFIRMED` / `DISMISSED`).
- `GET /api/v1/admin/audit-logs`: Query dynamic WORM audit logs with multicriteria filters.
