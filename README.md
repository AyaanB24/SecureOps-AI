# SecureOps - DevSecOps Security Aggregation & Gating Platform

## Quick Start

```bash
# Clone repository
cd d:\SecureOps-AI\secureops

# Build project
mvn clean package -DskipTests

# Run application
java -jar target/secureops-0.0.1-SNAPSHOT.jar
# OR
mvn spring-boot:run
```

Application runs on `http://localhost:8080`

Health check: `curl http://localhost:8080/api/health`

Expected response:
```json
{"status":"UP","service":"SecureOps","database":"UP"}
```

## Project Overview

SecureOps is a lightweight, modular Spring Boot backend designed to aggregate security scan results from multiple DevSecOps tools (Trivy, Semgrep, OWASP Dependency-Check) into a unified platform. It normalizes findings, calculates risk scores, evaluates security policies, and provides PASS/BLOCK decisions for CI/CD pipeline gating.

### Problem Being Solved

Organizations run multiple security scanning tools in their CI/CD pipelines, generating fragmented reports across different formats and severity scales. SecureOps centralizes these findings, normalizes them into a common structure, applies configurable security policies, and gates deployments based on risk assessment.

**Current workflow:**
```
Developer push → Jenkins → Security tools (Trivy, Semgrep) → Reports
                                                              ↓
SecureOps ← Parse & normalize → Calculate risk → Evaluate policy → PASS/BLOCK
```

## Architecture Overview

### Technology Stack

**Core:**
- Java 21
- Spring Boot 3.3.3
- Maven
- PostgreSQL 14+

**Key Dependencies:**
- Spring Web (REST APIs)
- Spring Data JPA (Database abstraction)
- Jakarta Validation (Input validation)
- Lombok (Reduce boilerplate)
- Spring DevTools (Development productivity)
- PostgreSQL Driver

**NOT included (future phases):**
- Spring Security & JWT
- Jenkins integration
- React frontend
- Kafka/Redis/microservices
- AI analysis

### Architectural Principles

1. **Modular Monolith** - Domain-based package organization
2. **Layered Architecture** - Controller → Service → Repository → Database
3. **DTOs** - API contracts via Data Transfer Objects (never expose JPA entities)
4. **Multi-project isolation** - Strict database relationships prevent data mixing
5. **Deterministic risk scoring** - Simple, documented MVP model
6. **Configurable policies** - Rules stored in database, not hardcoded

## Package Structure

```
com.secureops/
├── project/          # Project domain (soon)
├── pipeline/         # Pipeline domain (soon)
├── scan/            # Scan domain (soon)
├── report/          # Report ingestion (soon)
├── finding/         # Normalized findings (soon)
├── policy/          # Security policies (soon)
├── decision/        # PASS/BLOCK decisions (soon)
├── common/          # Shared utilities, exceptions, DTOs
│   ├── controller/  # REST endpoints
│   ├── dto/         # Request/response objects
│   ├── exception/   # Exception handling (soon)
│   └── util/        # Common utilities (soon)
└── config/          # Configuration classes (soon)
```

## Database Design

### Core Entities (Future Implementation)

```
PROJECT
  ├─ id (PK)
  ├─ name
  ├─ description
  ├─ createdAt
  └─ Pipeline* (1:N)

PIPELINE
  ├─ id (PK)
  ├─ project_id (FK)
  ├─ name
  ├─ createdAt
  └─ Scan* (1:N)

SCAN
  ├─ id (PK)
  ├─ pipeline_id (FK)
  ├─ scanType (e.g., TRIVY, SEMGREP)
  ├─ status (PENDING, IN_PROGRESS, COMPLETED, FAILED)
  ├─ startTime
  ├─ endTime
  ├─ Report* (1:N)
  └─ SecurityDecision (1:1 future)

REPORT
  ├─ id (PK)
  ├─ scan_id (FK)
  ├─ tool (e.g., TRIVY)
  ├─ rawJsonData (store original report)
  ├─ processedAt
  └─ Finding* (1:N)

FINDING
  ├─ id (PK)
  ├─ scan_id (FK)
  ├─ report_id (FK)
  ├─ tool
  ├─ ruleId
  ├─ title
  ├─ description
  ├─ severity (CRITICAL, HIGH, MEDIUM, LOW)
  ├─ filePath
  ├─ lineNumber
  ├─ packageName
  ├─ packageVersion
  ├─ fingerprint (unique identifier for dedup)
  └─ status

POLICY
  ├─ id (PK)
  ├─ project_id (FK)
  ├─ environment (DEV, STAGING, PROD)
  ├─ maxCritical
  ├─ maxHigh
  └─ maxRiskScore

SECURITY_DECISION
  ├─ id (PK)
  ├─ scan_id (FK)
  ├─ decision (PASS, BLOCK)
  ├─ riskScore
  ├─ criticalCount
  ├─ highCount
  ├─ mediumCount
  ├─ lowCount
  └─ reason
```

## Configuration Reference

### application.properties Explained

| Property | Value | Reason |
|----------|-------|--------|
| `server.port` | 8080 | Default Spring Boot port for local development |
| `spring.application.name` | SecureOps | Application identifier in logs/monitoring |
| `spring.datasource.url` | localhost:5432/secureops | PostgreSQL connection string |
| `spring.datasource.username` | postgres | Default PostgreSQL user |
| `spring.datasource.password` | postgres | Dev password (change in production) |
| `spring.jpa.hibernate.ddl-auto` | update | Automatically create/update tables from entities |
| `spring.jpa.show-sql` | false | Don't log SQL in production (set true for debugging) |
| `logging.level.com.secureops` | DEBUG | Show app-level debug logs |
| `spring.devtools.restart.enabled` | true | Auto-restart on file changes (dev only) |

### PostgreSQL Setup (Local Development)

```bash
# Create database (as postgres user)
psql -U postgres
postgres=# CREATE DATABASE secureops;
postgres=# \q

# Verify connection
psql -U postgres -d secureops
```

## Current Implementation Status

### Phase 0 ✓ COMPLETE - Project Setup & Health Check
**Objective:** Create a runnable Spring Boot application with PostgreSQL connectivity and a health endpoint that verifies both application and database status.

**Files Created:**
- `secureops/pom.xml` - Maven configuration with Spring Boot 4.1.1, PostgreSQL driver 42.7.3, Jakarta Validation, Lombok, and Spring DevTools
- `secureops/src/main/resources/application.properties` - PostgreSQL connection (localhost:5432/secureops), JPA/Hibernate config, logging levels
- `secureops/src/main/java/com/secureops/SecureopsApplication.java` - Spring Boot @SpringBootApplication entry point
- `secureops/src/main/java/com/secureops/common/dto/HealthResponse.java` - DTO with fields: status, service, database
- `secureops/src/main/java/com/secureops/common/service/HealthService.java` - Service that executes "SELECT 1" query to verify PostgreSQL connectivity
- `secureops/src/main/java/com/secureops/common/controller/HealthController.java` - REST controller with GET /api/health endpoint

**APIs Available:**
- `GET /api/health` → Returns `{"status":"UP","service":"SecureOps","database":"UP|DOWN"}` (HTTP 200)

**Database Connection Verified:**
- HealthService uses JdbcTemplate to execute `SELECT 1` against PostgreSQL
- Health endpoint response includes database status (UP/DOWN)
- Connection tested successfully with PostgreSQL running on localhost:5432

**Build Status:** ✓ SUCCESS (mvn clean package -DskipTests)

### Future Phases (Not Yet Implemented)
- Phase 1: Project management (CRUD, multi-project isolation)
- Phase 2: Pipeline management
- Phase 3: Scan management
- Phase 4: Report ingestion (JSON upload)
- Phase 5: Finding normalization
- Phase 6: Trivy report parser
- Phase 7: Risk engine (CRITICAL=10, HIGH=7, MEDIUM=4, LOW=1)
- Phase 8: Policy engine (configurable rules)
- Phase 9: Security decision (PASS/BLOCK)
- Phase 10: Postman end-to-end demonstration

## Postman Testing - Phase 0: Health Check

### Request Details

**Test Name:** Verify Application & Database Health

**HTTP Method:** GET

**URL:** `http://localhost:8080/api/health`

**Headers:**
```
Content-Type: application/json
```

**Request Body:** (none - GET request)

**Expected HTTP Status:** 200 OK

**Expected Response:**
```json
{
  "status": "UP",
  "service": "SecureOps",
  "database": "UP"
}
```

### Postman Setup Steps

1. Open Postman
2. Click "New" → "Request"
3. Set method to **GET**
4. Enter URL: `http://localhost:8080/api/health`
5. Click "Send"
6. Verify response status is `200 OK`
7. Verify response body contains `"database": "UP"`

### What Gets Verified

- ✓ Spring Boot application is running on port 8080
- ✓ REST controller is registered and responding
- ✓ PostgreSQL database connection is active
- ✓ HealthService.isDatabaseHealthy() successfully executed "SELECT 1" query
- ✓ Response DTO properly serialized to JSON

### Database Verification

After successful response, check PostgreSQL logs:
```bash
# PostgreSQL should show connection from Spring application
# Check logs for connection establishment and "SELECT 1" queries
```

---
- `POST /api/projects` - Create project
- `GET /api/projects/{projectId}` - Get project details
- `POST /api/projects/{projectId}/pipelines` - Create pipeline
- `POST /api/pipelines/{pipelineId}/scans` - Create scan
- `POST /api/scans/{scanId}/reports` - Upload report
- `GET /api/scans/{scanId}/findings` - Get normalized findings
- `GET /api/scans/{scanId}/decision` - Get PASS/BLOCK decision

## How to Run Locally

### Prerequisites
- Java 21+ installed
- Maven 3.8+
- PostgreSQL 14+ running on localhost:5432
- Database `secureops` created

### Build & Run

```bash
# Navigate to project root
cd d:\SecureOps-AI

# Compile and package
mvn clean package -DskipTests

# Run the application
mvn spring-boot:run
# OR
java -jar target/secureops-backend-1.0.0-SNAPSHOT.jar
```

**Expected output:**
```
...
[main] INFO o.s.b.w.e.tomcat.TomcatWebServer : Tomcat started on port(s): 8080 (http)
[main] INFO c.s.Application : Started Application in X.XXX seconds
```

### Verify Application is Running

**Using curl:**
```bash
curl http://localhost:8080/api/health
```

**Expected response:**
```json
{"status":"UP","service":"SecureOps"}
```

## Postman Testing Guide

### Phase 0 Test: Health Check

**Request:**
```
GET http://localhost:8080/api/health
```

**Headers:**
```
Content-Type: application/json
```

**Body:** (empty)

**Expected Response:**
```
HTTP Status: 200 OK

{
  "status": "UP",
  "service": "SecureOps"
}
```

**Database Verification:**
- No database records created (only connection test)
- Application should connect to PostgreSQL successfully
- Check logs for "Hibernate: select 1" (connection validation)

## How Project Isolation Works (Future Phases)

SecureOps enforces strict multi-project isolation:

1. **Database-level:** Every entity (Pipeline, Scan, Report, Finding) has a `project_id` foreign key
2. **API-level:** Project ID is mandatory in all resource URLs
3. **Query-level:** All repository queries filter by project ID automatically
4. **No global pools:** Findings from Project A can never appear in Project B queries

Example:
```
Project 1 (Payment API)
└─ Pipeline A → Scan 101 → Report (Trivy) → Finding 1, 2, 3
└─ Pipeline C → Scan 103 → Report (Semgrep) → Finding 4, 5

Project 2 (Job Portal)
└─ Pipeline B → Scan 202 → Report (Trivy) → Finding 6, 7
```

Query `/api/projects/1/scans/101/findings` returns only Finding 1,2,3. Finding 6,7 are unreachable.

## How Reports Are Processed (Future)

1. **Upload:** `POST /api/scans/{scanId}/reports` with JSON payload
2. **Parse:** Dispatcher detects tool type (Trivy/Semgrep/OWASP)
3. **Extract:** Tool-specific parser extracts raw data
4. **Normalize:** Convert to common Finding model
5. **Store:** Save findings with fingerprints (deduplication)
6. **Link:** Associate with correct Scan and Project

Example flow:
```
Raw Trivy JSON (80 lines, tool-specific format)
           ↓
TrivyParser.parse() → extracts vulnerability objects
           ↓
Map to Finding entity → severity, filePath, packageName, etc.
           ↓
Save to database with fingerprint hash
           ↓
Findings queryable via /api/scans/{scanId}/findings
```

## How Findings Are Normalized

Despite tool differences, all findings converge to:

```
Finding {
  id                          // Auto-generated
  scanId                      // Which scan
  reportId                    // Which report
  tool                        // TRIVY | SEMGREP | OWASP
  ruleId                      // Tool-specific rule identifier
  title                       // Vulnerability title
  description                 // Full description
  severity                    // CRITICAL | HIGH | MEDIUM | LOW
  filePath                    // Path to vulnerable file
  lineNumber                  // Line in file (if applicable)
  packageName                 // e.g., "openssl"
  packageVersion              // e.g., "1.1.1"
  fingerprint                 // SHA256(tool+ruleId+filePath+lineNumber) for dedup
  status                      // NEW | ACKNOWLEDGED | RESOLVED | WONTFIX
}
```

**Example normalization:**

Trivy reports CVE-2021-1234 in openssl 1.1.0 at line 45 of src/util.c
→ Normalized to Finding with severity=CRITICAL, filePath=src/util.c, packageName=openssl

Semgrep reports hardcoded credentials at src/config.py:10
→ Normalized to Finding with severity=HIGH, filePath=src/config.py, lineNumber=10

## How Risk Calculation Works (Phase 7)

**MVP Risk Scoring Model** (Not industry-standard; documented as MVP only)

Severity weights:
- CRITICAL = 10 points
- HIGH = 7 points
- MEDIUM = 4 points
- LOW = 1 point

**Formula:**
```
totalRiskScore = (criticalCount × 10) + (highCount × 7) + (mediumCount × 4) + (lowCount × 1)

Example:
5 CRITICAL + 3 HIGH + 8 MEDIUM + 12 LOW
= (5 × 10) + (3 × 7) + (8 × 4) + (12 × 1)
= 50 + 21 + 32 + 12
= 115 points
```

**Score interpretation (MVP):**
- 0-30 = Low risk
- 31-60 = Medium risk
- 61-85 = High risk
- 86+ = Critical risk

## How Policy Evaluation Works (Phase 8)

Policies are **database-driven, not hardcoded**. Example policy for PROD environment:

```
{
  projectId: 1,
  environment: "PROD",
  maxCritical: 0,      // Zero toleration
  maxHigh: 2,
  maxRiskScore: 70
}
```

**Evaluation logic:**
```
if (criticalCount > maxCritical) → BLOCK (reason: "Exceeded critical limit")
if (highCount > maxHigh) → BLOCK (reason: "Exceeded high limit")
if (riskScore > maxRiskScore) → BLOCK (reason: "Risk score exceeded")
Otherwise → PASS
```

## How PASS/BLOCK Decision is Generated (Phase 9)

After all findings are normalized and policy evaluated:

```
SecurityDecision {
  id: UUID,
  scanId: <scanId>,
  decision: "PASS" or "BLOCK",
  riskScore: <calculated>,
  criticalCount: <count>,
  highCount: <count>,
  mediumCount: <count>,
  lowCount: <count>,
  reason: "Policy violation: Critical findings exceed threshold"
}
```

**API Response:**
```
GET /api/scans/{scanId}/decision

{
  "scanId": "scan-12345",
  "decision": "BLOCK",
  "riskScore": 95,
  "criticalCount": 2,
  "highCount": 4,
  "mediumCount": 8,
  "lowCount": 15,
  "reason": "Critical findings (2) exceed policy maximum of 0"
}
```

Jenkins will receive this decision and decide whether to allow deployment.

## Future Jenkins Integration (Not Yet Implemented)

**Planned workflow:**
1. Jenkins runs Trivy/Semgrep → generates JSON reports
2. Jenkins calls `POST /api/scans/{scanId}/reports` with report JSON
3. SecureOps processes, normalizes, evaluates policy
4. Jenkins polls `GET /api/scans/{scanId}/decision`
5. If BLOCK → abort deployment, if PASS → continue

## Future AI Integration (Not Yet Implemented)

**Placeholder for ML features:**
- Anomaly detection in finding patterns
- Risk score refinement based on project history
- Automated remediation suggestions
- False positive filtering

## File Purpose Reference

### Phase 0 Files

| File | Purpose | Why It Exists | Dependencies |
|------|---------|---------------|--------------|
| `secureops/pom.xml` | Maven build configuration | Declare dependencies and build lifecycle | N/A |
| `secureops/src/main/resources/application.properties` | Spring Boot application configuration | Configure database URL, username, password, JPA settings, logging | Spring Framework |
| `secureops/src/main/java/com/secureops/SecureopsApplication.java` | Application entry point | Bootstrap Spring Boot application and auto-configuration | Spring Framework |
| `secureops/src/main/java/com/secureops/common/dto/HealthResponse.java` | Health response DTO | Serialize health check response to JSON | Jackson (Spring Web) |
| `secureops/src/main/java/com/secureops/common/service/HealthService.java` | Health check service | Encapsulate database connectivity check logic; isolate JDBC calls | Spring JDBC (JdbcTemplate) |
| `secureops/src/main/java/com/secureops/common/controller/HealthController.java` | REST controller | Expose GET /api/health endpoint; orchestrate health check | HealthService, HealthResponse |

---

## Phase Completion Checklist (Phase 0)

- [x] Maven pom.xml created with Java 21, Spring Boot, PostgreSQL, Lombok dependencies
- [x] application.properties configured for PostgreSQL on localhost:5432/secureops
- [x] Application.java entry point created with @SpringBootApplication
- [x] HealthResponse DTO created (status, service fields)
- [x] HealthController created with GET /api/health endpoint
- [x] Application compiles without errors
- [x] Application runs successfully on port 8080
- [x] GET /api/health returns 200 with correct JSON
- [x] PostgreSQL connection is established (check logs)
- [x] No database tables created yet (only connection)
- [x] README.md created with complete documentation
- [x] File purposes documented inline and in README table
- [x] Package structure is clean and ready for Phase 1

---

**Next:** START PHASE 1 when ready to implement Project domain entity and CRUD operations.
