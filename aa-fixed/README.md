# Account Aggregator Integration – Del Capital Assessment

## Overview

This project implements an Account Aggregator (AA) Integration using:

- Spring Boot 3
- PostgreSQL
- Angular
- Digio Account Aggregator Sandbox APIs

The application enables consent-driven financial data sharing workflows between Financial Information Users (FIUs) and Financial Information Providers (FIPs) through the Account Aggregator framework.

The solution includes:

- Consent lifecycle management
- Digio AA integration
- Financial data retrieval workflows
- PostgreSQL persistence
- Audit logging
- Angular frontend
- Swagger/OpenAPI documentation
- Retry and Circuit Breaker support using Resilience4j

---

# Assignment Objectives Covered

## Backend

✅ Spring Boot REST APIs

✅ PostgreSQL Integration

✅ Flyway Database Migrations

✅ DTO Validation

✅ Layered Architecture

✅ Audit Logging

✅ Exception Handling

✅ Digio API Integration

✅ Webhook Processing

✅ Resilience4j Retry

✅ Circuit Breaker

✅ Idempotency Support

---

## Frontend

✅ Angular Application

✅ Consent Creation Screen

✅ Consent Status View

✅ API Integration

---

## Database

✅ Customer Management

✅ Consent Persistence

✅ Financial Data Storage

✅ Audit Logs

✅ Webhook Events

---

# Technology Stack

## Backend

- Java 17
- Spring Boot 3.2.5
- Spring Data JPA
- Spring Security
- Spring Validation
- Spring WebFlux (WebClient)
- PostgreSQL
- Flyway
- Resilience4j
- Swagger/OpenAPI
- Lombok

## Frontend

- Angular
- TypeScript
- Bootstrap

## Database

- PostgreSQL 18

---

# Architecture

```text
┌──────────────────────┐
│   Angular Frontend   │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│ Spring Boot REST API │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│  Business Services   │
└──────────┬───────────┘
           │
   ┌───────┴────────┐
   ▼                ▼
┌───────────┐  ┌─────────────┐
│ Digio API │  │ PostgreSQL  │
│  Client   │  │  Database   │
└───────────┘  └─────────────┘
```

---

# Project Structure

```text
backend
│
├── src/main/java/com/delcapital/aa
│   ├── config
│   ├── controller
│   ├── dto
│   │   ├── request
│   │   └── response
│   ├── entity
│   ├── enums
│   ├── exception
│   ├── repository
│   ├── service
│   │   └── impl
│   └── AccountAggregatorApplication.java
│
├── src/main/resources
│   ├── db/migration
│   └── application.yml
│
├── pom.xml
└── Dockerfile

frontend
│
├── src
├── app
├── services
├── components
└── angular.json
```
---

# Features Implemented

## Consent Management

- Create Consent
- Get Consent Details
- List Customer Consents
- Consent Status Updates

## Financial Information

- Initiate FI Data Fetch
- Fetch Session Status
- Download FI Data

## Webhooks

- Digio Webhook Endpoint
- Webhook Event Storage

## Audit

- Audit Trail Logging
- Compliance Tracking

## Reliability

- Retry Mechanism
- Circuit Breaker
- Idempotency Support

## Observability

- Spring Actuator
- Health Monitoring
- Metrics
- Structured Logging

---

# Database Setup

## Create Database

```sql
CREATE DATABASE aa_db;
```

## Create User

```sql
CREATE USER aa_user WITH PASSWORD 'aa_pass';

GRANT ALL PRIVILEGES
ON DATABASE aa_db
TO aa_user;
```

---

# PostgreSQL Verification

Verify PostgreSQL installation:

```bash
psql --version
```

Expected:

```bash
psql (PostgreSQL) 18.x
```

---

# Backend Configuration

File:

```text
src/main/resources/application.yml
```

## Database Configuration

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/aa_db
    username: aa_user
    password: aa_pass
```

---

# Digio Sandbox Configuration

Credentials provided by Del Capital:

```text
Username:
ACK25030511375911186LKWJDXERYTXQ

Password:
********

Template ID:
CTMP250909154046573UG2FM8U7NZK36

API Base URL:
https://ext.digio.in:444/fiu_api
```

Configuration:

```yaml
digio:
  base-url: https://ext.digio.in:444/fiu_api
  username: ${DIGIO_USERNAME}
  password: ${DIGIO_PASSWORD}
  template-id: ${DIGIO_TEMPLATE_ID}
```

---

# Running Backend

Navigate to backend:

```bash
cd backend
```

Build:

```bash
mvn clean install
```

Run:

```bash
mvn spring-boot:run
```

Application starts at:

```text
http://localhost:8080/api
```

---

# Swagger UI

```text
http://localhost:8080/api/swagger-ui/index.html
```

---

# Health Check

```text
http://localhost:8080/api/actuator/health
```

Expected:

```json
{
  "status": "UP"
}
```

---

# Running Frontend

Navigate to frontend:

```bash
cd frontend
```

Install dependencies:

```bash
npm install
```

Run:

```bash
npm start
```

Frontend URL:

```text
http://localhost:4200
```

---

# Main APIs

## Create Consent

```http
POST /api/v1/consents
```

Example Request:

```json
{
  "customerExternalId": "TEST001",
  "customerName": "Satheesh",
  "mobile": "9876543210",
  "email": "satheesh@test.com",
  "purposeCode": "101",
  "purposeText": "Credit Assessment",
  "fiTypes": ["DEPOSIT"],
  "consentStart": "2026-06-15T12:00:00Z",
  "consentExpiry": "2027-06-15T12:00:00Z",
  "fetchType": "ONETIME",
  "frequencyUnit": "MONTH",
  "frequencyValue": 1,
  "redirectUrl": "http://localhost:4200",
  "callbackUrl": "http://localhost:8080/api/v1/webhook/digio"
}
```

---

## Get Consent

```http
GET /api/v1/consents/{consentId}
```

---

## Get Customer Consents

```http
GET /api/v1/consents/customer/{customerExternalId}
```

---

## Initiate Data Fetch

```http
POST /api/v1/fetch
```

---

## Webhook Endpoint

```http
POST /api/v1/webhook/digio
```

---

# Security Features

- DTO Validation
- Input Sanitization
- Exception Handling
- Audit Logging
- Externalized Credentials
- Transaction Management

---

# Reliability Features

- Retry Mechanism
- Circuit Breaker
- Idempotency Key Support
- Structured Logging
- Audit Trail

---

# Database Entities

Implemented Entities:

- Customer
- ConsentRequest
- FiFetchSession
- FiAccount
- AuditLog
- WebhookEvent

---

# Issues Faced During Development

## 1. Missing Hypersistence Dependencies

Initial build failed because:

```text
package io.hypersistence.utils.hibernate.type.array does not exist
```

Resolution:

- Removed unused imports.
- Simplified entity mappings.

---

## 2. Spring Boot 3 HttpStatus Changes

Compilation error:

```text
invalid method reference
HttpStatus::isError
```

Resolution:

```java
status -> status.isError()
```

---

## 3. PostgreSQL Setup

Issue:

```text
psql is not recognized
```

Resolution:

- Installed PostgreSQL 18
- Added PostgreSQL bin folder to PATH

---

## 4. Database Authentication

Issue:

```text
password authentication failed for user postgres
```

Resolution:

- Reset and verified PostgreSQL credentials.
- Successfully connected using psql.

---

## 5. Angular Routing Error

Issue:

```text
NG04002: Cannot match any routes
```

Resolution:

- Fixed Angular route configuration.

---

## 6. Digio API SSL Connectivity

Issue:

```text
CRYPT_E_NO_REVOCATION_CHECK
```

Resolution:

- Verified connectivity using curl with SSL bypass for testing.

---

## 7. Digio Consent Creation Failure

Observed Request:

```text
POST
https://ext.digio.in:444/fiu_api/v2/client/consent/request/create/{templateId}
```

Observed Response:

```text
500 INTERNAL_SERVER_ERROR
Request failed.
```

Investigation Completed:

✅ Verified Digio credentials

✅ Verified Template ID

✅ Verified API URL

✅ Verified Backend Connectivity

✅ Verified Request Payload Generation

✅ Verified Authentication

✅ Verified Database Persistence

Current Observation:

The application successfully reaches the Digio Sandbox endpoint, but consent creation returns HTTP 500 from the Digio side.

This appears to be related to:

- Sandbox API behavior
- Template configuration
- Endpoint-specific requirements

rather than the local application setup.

---

# Current Status

| Component | Status |
|------------|----------|
| Spring Boot Backend | ✅ Working |
| PostgreSQL | ✅ Working |
| Flyway Migration | ✅ Working |
| Swagger UI | ✅ Working |
| Angular Frontend | ✅ Working |
| Audit Logging | ✅ Working |
| Webhook APIs | ✅ Working |
| Digio Connectivity | ✅ Working |
| Digio Consent Creation | ⚠ Sandbox Error |

---

# Future Enhancements

- JWT Authentication
- Encryption of Sensitive Data
- Kafka Event Processing
- Distributed Tracing
- Advanced Monitoring
- Production-grade Secret Management

---

# Author

**Satheesh Rallapalli**

Submission for:

**Del Capital – Software Engineer Assessment**
