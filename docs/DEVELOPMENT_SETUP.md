# Development Environment & Setup Guide

## 1. Prerequisites

- **Java Development Kit (JDK)**: Java 21 (Eclipse Temurin recommended)
- **Node.js**: Node 20+ & npm 10+
- **Database / Cache / Broker**: PostgreSQL 16, Redis 7, Apache Kafka 3.7 (or run via Docker)
- **Build Tools**: Apache Maven 3.9+
- **IDE**: IntelliJ IDEA, VS Code (with Java Extension Pack), or Eclipse

---

## 2. Step-by-Step Local Development Setup

### Step 1: Start Supporting Infrastructure via Docker
To run supporting infrastructure (Postgres, Redis, Kafka, Prometheus, Grafana) locally:
```bash
# Start infrastructure containers only
docker compose up -d postgres redis kafka prometheus grafana
```

### Step 2: Configure Environment Variables
Copy the template configuration:
```bash
cp .env.example .env
```

### Step 3: Run Spring Boot Backend
```bash
# Compile and run the backend on port 8080
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
The backend will be available at `http://localhost:8080`.
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Actuator Health: `http://localhost:8080/actuator/health`

### Step 4: Run React TypeScript Frontend
In a separate terminal window:
```bash
cd frontend
npm install
npm run dev
```
The Vite development server will launch at `http://localhost:5173` with instant Hot Module Replacement (HMR).

---

## 3. Pre-Seeded Default Accounts

| Role | Email | Password | Pre-Seeded Balance |
| :--- | :--- | :--- | :--- |
| **Customer** | `customer@bank.com` | `Customer@123` | ₹50,000.00 (Account: `100000000001`) |
| **Admin** | `admin@bank.com` | `Admin@123` | N/A (Compliance / Admin Role) |
