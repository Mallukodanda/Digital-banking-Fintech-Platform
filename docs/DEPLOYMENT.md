# Deployment & Infrastructure Guide

## 1. Local Containerized Deployment (Docker Compose)

The entire platform runs locally with a single command via [docker-compose.yml](file:///c:/Users/mallu/OneDrive/Documents/Digital-banking-Fintech-Platform/Digital-banking-Fintech-Platform/docker-compose.yml).

### Prerequisites
- Docker Engine 24+ & Docker Compose v2+
- 4 GB+ RAM allocated to Docker daemon

### Quick Start
```bash
# 1. Clone the repository
git clone https://github.com/Mallukodanda/Digital-banking-Fintech-Platform.git
cd Digital-banking-Fintech-Platform

# 2. Configure environment template
cp .env.example .env

# 3. Build and launch all 7 services in detached mode
docker compose up --build -d

# 4. Verify running services
docker compose ps
```

### Services & Port Mappings

| Service | Port | Healthcheck Endpoint | Description |
| :--- | :--- | :--- | :--- |
| **Frontend** | `3000` | `http://localhost:3000/healthz` | React 18 SPA + Nginx Reverse Proxy |
| **Backend Core** | `8080` | `http://localhost:8080/actuator/health` | Spring Boot Core Banking API |
| **PostgreSQL** | `5432` | `pg_isready -U banking_admin` | PostgreSQL 16 Database |
| **Redis** | `6379` | `redis-cli ping` | In-Memory Cache |
| **Kafka (KRaft)**| `9092` | Broker Port Probe | Apache Kafka Event Broker |
| **Prometheus** | `9090` | `http://localhost:9090/-/healthy` | Time-Series Metrics Scraper |
| **Grafana** | `3001` | `http://localhost:3001/api/health` | SRE Observability Dashboards |

---

## 2. CI/CD Automated Pipelines (GitHub Actions)

- **Continuous Integration** (`.github/workflows/ci.yml`): Runs on every push and PR to `main`/`develop`. Executes all 10 testing layers with a **JaCoCo $\ge 80\%$ code coverage gate** and TypeScript compile check.
- **Docker Release & Scan** (`.github/workflows/docker-publish.yml`): Builds multi-stage images, runs Trivy vulnerability scanner, and pushes to `ghcr.io`.
- **Continuous Deployment** (`.github/workflows/cd-deploy.yml`): Deploys to Staging and Production with GitHub Environments approval gates.

---

## 3. AWS Cloud Deployment

Refer to [aws_cloud_architecture.md](file:///C:/Users/mallu/.gemini/antigravity-ide/brain/ce648cc9-67e4-4a49-97b0-533f2ab07035/aws_cloud_architecture.md) for full Terraform scripts, Multi-AZ ECS Fargate topology, and cost breakdowns ($30/mo educational vs $300/mo production).
