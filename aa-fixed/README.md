# Del Capital — Account Aggregator Integration

A full-stack implementation of the **RBI Account Aggregator (AA) framework** using **Digio** as the AA operator, built for Del Capital's FIU workflows (credit decisioning, underwriting, risk analytics).

---

## Architecture

```
Customer Browser
      │
      ▼
┌─────────────┐          ┌─────────────────────┐
│  Angular UI  │──REST───▶│  Spring Boot Backend │
│  (Port 4200) │◀─JSON───│  (Port 8080)         │
└─────────────┘          └────────┬─────────────┘
                                   │  HTTPS (Basic Auth)
                          ┌────────▼─────────────┐
                          │   Digio AA API        │
                          │   ext.digio.in:444    │
                          └────────┬─────────────┘
                                   │ Webhook callbacks
                          ┌────────▼─────────────┐
                          │   PostgreSQL           │
                          │   (Port 5432)          │
                          └─────────────────────────┘
```

### Key flows

```
1. Consent Flow
   UI → POST /v1/consents → Digio createConsent → consentUrl returned
   Customer visits consentUrl → signs consent on Digio
   Digio → POST /v1/webhook/digio (CONSENT_STATUS_UPDATE → ACTIVE)

2. Data Fetch Flow
   UI → POST /v1/fi/fetch → Digio initiateDataFetch → sessionId returned
   Digio fetches from FIPs asynchronously
   Digio → POST /v1/webhook/digio (DATA_FETCH_STATUS_UPDATE → COMPLETED)
   Backend downloads & normalises FI data → saved to PostgreSQL
   UI → GET /v1/fi/fetch/{sessionId}/accounts → canonical accounts + txns
```

---

## Project Structure

```
aa-integration/
├── backend/                        # Spring Boot 3.2, Java 17
│   ├── src/main/java/com/delcapital/aa/
│   │   ├── config/                 # Digio client, security, async, MDC, OpenAPI
│   │   ├── controller/             # REST controllers
│   │   ├── dto/                    # Request / response DTOs
│   │   ├── entity/                 # JPA entities
│   │   ├── enums/                  # ConsentStatus, FetchStatus, FiType
│   │   ├── exception/              # Custom exceptions + global handler
│   │   ├── repository/             # Spring Data JPA repos
│   │   └── service/                # Business logic + Digio API client
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/           # Flyway SQL migrations
│   └── Dockerfile
│
├── frontend/                       # Angular 17
│   ├── src/app/
│   │   ├── components/
│   │   │   ├── consent-form/       # Initiate consent
│   │   │   ├── consent-status/     # Poll + display consent status
│   │   │   ├── fetch-data/         # Trigger FI data fetch
│   │   │   └── accounts-view/      # Normalised accounts + transactions
│   │   ├── services/aa-api.service.ts
│   │   ├── models/aa.models.ts
│   │   └── interceptors/auth.interceptor.ts
│   ├── Dockerfile
│   └── nginx.conf
│
├── docker-compose.yml
├── .env.example
└── README.md
```

---

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 17+ |
| Maven | 3.9+ |
| Node.js | 20+ |
| Docker & Compose | Latest |
| PostgreSQL | 16 (or via Docker) |

---

## Quick Start (Docker — recommended)

```bash
# 1. Clone and enter the project
cd aa-integration

# 2. Set credentials
cp .env.example .env
# Edit .env — fill DIGIO_USERNAME, DIGIO_PASSWORD, DIGIO_TEMPLATE_ID

# 3. Start everything
docker compose up --build

# Frontend:  http://localhost:4200
# Backend:   http://localhost:8080/api
# Swagger:   http://localhost:8080/api/swagger-ui.html
```

---

## Manual Local Setup

### Backend

```bash
cd backend

# Start PostgreSQL (or use Docker)
docker run -d --name aa_pg -e POSTGRES_DB=aa_db \
  -e POSTGRES_USER=aa_user -e POSTGRES_PASSWORD=aa_pass \
  -p 5432:5432 postgres:16-alpine

# Export Digio credentials
export DIGIO_USERNAME=ACK25030511375911186LKWJDXERYTXQ
export DIGIO_PASSWORD=CGYGOZKNCMHN8I8GSA5HH2DFKRSR732L
export DIGIO_TEMPLATE_ID=CTMP250909154046573UG2FM8U7NZK36

# Run
mvn spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm start        # serves on http://localhost:4200
                 # proxies /api → http://localhost:8080
```

---

## API Reference

Full interactive docs at `http://localhost:8080/api/swagger-ui.html`

### Consent APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/v1/consents` | Create consent (idempotent) |
| GET | `/v1/consents/{id}` | Get consent by ID |
| GET | `/v1/consents/customer/{externalId}` | List consents for customer |

**Create Consent Request body:**
```json
{
  "customerExternalId": "CUST-001",
  "customerName": "Satheesh Kumar",
  "mobile": "9876543210",
  "email": "satheesh@example.com",
  "purposeCode": "104",
  "purposeText": "Credit decisioning",
  "fiTypes": ["DEPOSIT"],
  "consentStart": "2026-06-13T00:00:00+05:30",
  "consentExpiry": "2027-06-13T00:00:00+05:30",
  "fetchType": "ONETIME"
}
```

### FI Data APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/v1/fi/fetch` | Initiate data fetch (idempotent) |
| GET | `/v1/fi/fetch/{sessionId}` | Poll fetch status |
| GET | `/v1/fi/fetch/{sessionId}/accounts` | Get normalised accounts + txns |

### Webhook

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/v1/webhook/digio` | Receives async events from Digio |

### Other

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/v1/customers/{externalId}` | Get customer |
| GET | `/v1/audit/{entityType}/{entityId}` | Get audit logs |

---

## Sandbox Testing

Per the assignment constraints, live AA data cannot be fetched. Use these steps:

1. Create a consent via the UI or API
2. Copy the `consentUrl` from the response and open it in a browser
3. Select **FinShareBank OE UAT FIP** as the FIP
4. Enter OTP: **021069**
5. Consent becomes ACTIVE (webhook fires or poll detects it)
6. Trigger data fetch — sandbox returns mock FI data

---

## Database Schema

### Tables

| Table | Purpose |
|-------|---------|
| `customers` | Stores customer identity (PII-minimised) |
| `consent_requests` | Full consent lifecycle records |
| `fi_fetch_sessions` | Each data-fetch attempt |
| `fi_accounts` | Normalised account data from FIPs |
| `fi_transactions` | Individual transaction records |
| `audit_logs` | Append-only compliance audit trail |
| `webhook_events` | Raw Digio webhook payloads (idempotent store) |

All tables use UUID primary keys and `TIMESTAMPTZ` timestamps. `updated_at` is managed by PostgreSQL triggers.

---

## Security Controls

| Control | Implementation |
|---------|----------------|
| Transport security | HTTPS enforced (Digio calls over TLS 1.2+) |
| Credential storage | Never in code — env vars / secrets manager |
| Stateless auth | JWT Bearer tokens |
| CORS | Allowlist via `cors.allowed-origins` |
| Input validation | Jakarta Bean Validation on all DTOs |
| PII minimisation | Mobile masked in API responses; raw FIP payloads stored as JSONB (encrypt-at-rest via DB-level encryption) |
| Audit trail | Append-only `audit_logs` table, never deleted |
| Idempotency | Consent + fetch endpoints are fully idempotent via caller-supplied keys |
| Webhook safety | Raw events persisted before processing; retry scheduler for failures |
| Non-root container | Spring Boot runs as `spring:spring` user in Docker |

---

## Resilience

- **Circuit Breaker** (Resilience4j) on all Digio API calls — opens after 50% failure rate over 10 calls
- **Retry** — up to 3 attempts with 2s backoff for transient Digio failures
- **Async fetch** — FI data download runs on a separate thread pool, never blocking HTTP threads
- **Webhook retry scheduler** — retries unprocessed webhook events every 60 seconds
- **Idempotency** — safe to replay any request without side effects

---

## Observability

- Structured logs with MDC trace IDs (`X-Trace-Id` header propagated)
- Prometheus metrics at `/api/actuator/prometheus`
- Health check at `/api/actuator/health`
- All Digio request/response payloads stored in `raw_request` / `raw_response` JSONB columns

---

## Compliance Notes

- Consent records are never deleted — soft lifecycle via `status` field
- Audit log is append-only by design (no DELETE/UPDATE triggers)
- FIP raw payloads retained per regulatory requirements
- No personal account numbers stored — only masked references from FIPs
- Sandbox: do not enter real account details per Digio sandbox policy
