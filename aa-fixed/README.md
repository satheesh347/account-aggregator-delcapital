# Account Aggregator Integration – Del Capital Assessment

## Overview

This project implements an Account Aggregator (AA) integration using Spring Boot, PostgreSQL, Angular, and Digio Sandbox APIs.

The application enables:

* Customer consent creation
* Consent lifecycle management
* Financial Information (FI) data fetch initiation
* Webhook processing
* Audit logging
* Data persistence
* Basic Angular UI for customer interaction

The solution follows a layered architecture with DTO validation, service abstraction, persistence, audit tracking, resilience mechanisms, and API documentation.

---

# Technology Stack

## Backend

* Java 17
* Spring Boot 3.2.5
* Spring Data JPA
* Spring Security
* Spring Validation
* Spring WebFlux (WebClient)
* PostgreSQL
* Flyway
* Resilience4j
* Swagger / OpenAPI
* Lombok

## Frontend

* Angular
* TypeScript
* Bootstrap

## Database

* PostgreSQL 18

---

# Project Structure

backend/
├── config/
├── controller/
├── dto/
├── entity/
├── enums/
├── exception/
├── repository/
├── service/
├── resources/
│ ├── db/migration
│ └── application.yml
└── pom.xml

frontend/
├── src/
├── app/
├── components/
└── services/

---

# Features Implemented

## Consent Management

* Create Consent
* Retrieve Consent
* Customer Consent Listing
* Consent Status Updates

## Financial Information

* Initiate Data Fetch
* Fetch Session Status
* Retrieve Accounts

## Webhooks

* Digio Webhook Receiver
* Retry Handling

## Audit

* Audit Log Persistence
* Compliance Tracking

## Reliability

* Retry Mechanism
* Circuit Breaker
* Idempotency Support

## Observability

* Spring Actuator
* Health Endpoint
* Metrics Endpoint
* Structured Logging

---

# Database Schema

Entities:

* Customer
* ConsentRequest
* FiFetchSession
* FiAccount
* FiTransaction
* AuditLog
* WebhookEvent

Database migrations are managed using Flyway.

---

# Running PostgreSQL

Create Database:

```sql
CREATE DATABASE aa_db;
```

Create User:

```sql
CREATE USER aa_user WITH PASSWORD 'aa_pass';
GRANT ALL PRIVILEGES ON DATABASE aa_db TO aa_user;
```

Backend Configuration

application.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/aa_db
    username: aa_user
    password: aa_pass
```

Digio Sandbox Configuration

Credentials provided by Del Capital:

```text
Username: ACK25030511375911186LKWJDXERYTXQ
Password: ********
Template ID: CTMP250909154046573UG2FM8U7NZK36
Base URL: https://ext.digio.in:444/fiu_api
```

Configured in:

```yaml
digio:
  base-url: https://ext.digio.in:444/fiu_api
  username: ${DIGIO_USERNAME}
  password: ${DIGIO_PASSWORD}
  template-id: ${DIGIO_TEMPLATE_ID}
```

Running Backend

Navigate to backend directory:

```bash
cd backend
```

Build project:

```bash
mvn clean install
```

Run application:

```bash
mvn spring-boot:run
```

Backend URL:

```text
http://localhost:8080/api
```

Swagger:

```text
http://localhost:8080/api/swagger-ui/index.html
```

Health Check:

```text
http://localhost:8080/api/actuator/health
```

Running Frontend

Navigate to frontend:

```bash
cd frontend
```

Install dependencies:

```bash
npm install
```

Run Angular application:

```bash
npm start
```

Frontend URL:

```text
http://localhost:4200
```

API Endpoints

Consent APIs

```http
POST /api/v1/consents
GET  /api/v1/consents/{consentId}
GET  /api/v1/consents/customer/{externalId}
```

FI Data APIs

```http
POST /api/v1/fi/fetch
GET  /api/v1/fi/fetch/{sessionId}
GET  /api/v1/fi/fetch/{sessionId}/accounts
```

Webhook

```http
POST /api/v1/webhook/digio
```

Audit

```http
GET /api/v1/audit/{entityType}/{entityId}
```

Testing

Backend Health:

```text
http://localhost:8080/api/actuator/health
```

Expected Response:

```json
{
  "status": "UP"
}
```

Swagger UI:

```text
http://localhost:8080/api/swagger-ui/index.html
```

Known Limitations

* Digio Sandbox APIs were used.
* Live banking data cannot be fetched in sandbox mode.
* Consent creation depends on Digio sandbox endpoint availability.
* Some Digio endpoint behaviors are restricted in sandbox environments.
* Financial data retrieval requires sandbox-supported FIPs.

Security Considerations

* Basic Authentication used for Digio integration.
* DTO validation implemented.
* Audit logging enabled.
* PII stored only for required compliance use cases.
* Sensitive credentials externalized through environment variables.

Reliability Features

* Resilience4j Retry
* Resilience4j Circuit Breaker
* Idempotency Key Support
* Structured Error Handling
* Transaction Management

Future Improvements

* OAuth2/JWT based authentication
* Encryption of sensitive PII
* Distributed tracing
* Kafka based webhook processing
* Enhanced consent lifecycle monitoring
* Additional FIP normalization adapters

Author

Satheesh Rallapalli

Submission for Del Capital Software Engineer Assessment.
