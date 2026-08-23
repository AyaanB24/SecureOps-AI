# SecureOps AI
### AI-Assisted Security Assessment Platform for Java/Spring Boot Applications

> **Status:** Ongoing — Architecture & Backend Foundation

SecureOps AI is a Java-based DevSecOps platform designed to identify security vulnerabilities in **Java/Spring Boot applications** and provide actionable security insights.

The project focuses on building a production-oriented **Spring Boot backend** that can orchestrate security analysis, process vulnerability findings, and eventually use AI to explain and prioritize identified issues.

---

## 1. Problem Statement

Security analysis is often performed manually or through multiple independent tools, making it difficult for developers to understand:

* Which vulnerabilities are critical
* Where they exist in the application
* What caused them
* How they should be fixed
* Which issues should be addressed first

SecureOps aims to provide a centralized backend workflow for analyzing Java applications and converting security findings into actionable information.

---

## 2. Project Goals

### Primary Goal
Build a platform that can automatically identify security vulnerabilities in Java/Spring Boot applications.

### Initial Scope
* Analyze Java/Spring Boot applications
* Detect common security vulnerabilities
* Collect and normalize security findings
* Assign severity and risk information
* Expose findings through REST APIs
* Containerize the platform using Docker
* Establish a foundation for CI/CD integration
* Add AI-assisted vulnerability explanation and prioritization

---

## 3. System Architecture

```
Internet Users
    ↓
Nginx Reverse Proxy (:80 / :443)
    ↓
┌─────────────────────────────────┐
│ SecureOps Backend Network       │
├─────────────────────────────────┤
│                                 │
│  Spring Boot API (:8080)        │
│  ├── Auth Routes (JWT + Spring  │
│  │   Security)                  │
│  ├── Scan Routes                │
│  ├── Finding Routes             │
│  └── Report Routes              │
│       ↓                         │
│  Security Analysis Engine       │
│  ├── SAST Tools                 │
│  ├── Dependency Analysis        │
│  └── Container Analysis         │
│       ↓                         │
│  Finding Normalizer             │
│  (Severity / Risk / Source)     │
│       ↓                         │
│  AI Analysis Layer              │
│  (Explanation / Priority /      │
│   Remediation)                  │
│       ↓                         │
│  PostgreSQL / MySQL Database    │
│                                 │
│  Named Volumes:                 │
│  - db_data                      │
│  - scan_workspace               │
│                                 │
└─────────────────────────────────┘
```

All backend services communicate over a shared bridge network (`secureops-network`), with named volumes for `db_data` and `scan_workspace` persisting across container restarts.

> Components in the Analysis Services and AI Analysis Layer are part of the planned roadmap (Phase 3 / Phase 5), not completed functionality yet.

---

## 4. Backend Architecture

The core platform is implemented using **Java and Spring Boot**.

```
secureops/
├── src/main/java/com/secureops/
│   ├── project/              (Phase 1 ✅)
│   │   ├── Project.java
│   │   ├── ProjectRepository.java
│   │   ├── ProjectService.java
│   │   ├── ProjectController.java
│   │   ├── ProjectNotFoundException.java
│   │   └── dto/
│   │       ├── CreateProjectRequest.java
│   │       └── ProjectResponse.java
│   │
│   ├── pipeline/             (Phase 2 - Coming)
│   │   ├── Pipeline.java
│   │   ├── PipelineRepository.java
│   │   ├── PipelineService.java
│   │   └── PipelineController.java
│   │
│   ├── scan/                 (Phase 3 - Coming)
│   │   ├── Scan.java
│   │   ├── ScanRepository.java
│   │   ├── ScanService.java
│   │   └── ScanController.java
│   │
│   ├── report/               (Phase 4 - Coming)
│   │   ├── Report.java
│   │   └── ReportService.java
│   │
│   ├── finding/              (Phase 5 - Coming)
│   │   ├── Finding.java
│   │   ├── FindingRepository.java
│   │   └── FindingService.java
│   │
│   ├── policy/               (Phase 8 - Coming)
│   │   ├── Policy.java
│   │   └── PolicyService.java
│   │
│   ├── decision/             (Phase 9 - Coming)
│   │   ├── SecurityDecision.java
│   │   └── DecisionEngine.java
│   │
│   ├── common/
│   │   ├── controller/
│   │   │   ├── HealthController.java
│   │   │   └── ...
│   │   ├── dto/
│   │   │   ├── HealthResponse.java
│   │   │   └── ...
│   │   ├── exception/
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   └── ErrorResponse.java
│   │   ├── service/
│   │   │   └── HealthService.java
│   │   └── ...
│   │
│   └── SecureopsApplication.java
│
├── src/main/resources/
│   └── application.properties
│
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```

The architecture follows a standard layered Spring Boot structure:

```
Controller → Service → Repository → Database
```

Security-analysis components will be separated from the API layer so that additional security tools can be integrated without tightly coupling them to the REST controllers.

---

## 5. Core Workflow

### Step 1 — Submit Application
A user submits a Java/Spring Boot application or repository for analysis.

```
POST /api/projects (Phase 1)
POST /api/pipelines (Phase 2)
POST /api/scans (Phase 3)
```

### Step 2 — Create Scan
SecureOps creates a scan record and assigns it a unique scan ID.

```
Scan
├── ID
├── Project
├── Pipeline
├── Status
├── CreatedAt
└── CompletedAt
```

### Step 3 — Security Analysis
The analysis engine executes the configured security checks against the application.

Initially, the focus will be on **Java/Spring Boot security analysis**.

### Step 4 — Normalize Findings
Different security tools can produce different output formats.

SecureOps normalizes them into a common finding model.

```
Finding
├── Title
├── Description
├── Severity (CRITICAL, HIGH, MEDIUM, LOW)
├── Category
├── Source (Tool: Trivy, Semgrep, OWASP)
├── File
├── Line
└── Remediation
```

### Step 5 — AI Analysis
The AI layer consumes normalized findings and provides:

* Vulnerability explanation
* Risk interpretation
* Suggested remediation
* Finding prioritization

### Step 6 — Report
Final results are available through REST APIs and eventually through a report/dashboard layer.

---

## 6. Initial REST API Design

### Project API (Phase 1 ✅)
```
POST   /api/projects                 → 201 Created
GET    /api/projects                 → 200 OK
GET    /api/projects/{projectId}     → 200 OK / 404
```

### Pipeline API (Phase 2 - Coming)
```
POST   /api/projects/{projectId}/pipelines
GET    /api/projects/{projectId}/pipelines
GET    /api/pipelines/{pipelineId}
```

### Scan API (Phase 3 - Coming)
```
POST   /api/pipelines/{pipelineId}/scans
GET    /api/scans/{scanId}
```

### Findings API (Phase 5 - Coming)
```
GET    /api/scans/{scanId}/findings
GET    /api/findings/{findingId}
```

### Reports API (Phase 10 - Coming)
```
GET    /api/scans/{scanId}/report
GET    /api/scans/{scanId}/decision
```

---

## 7. Security Analysis

The security engine is planned to support multiple analysis categories.

### Source Code Analysis
Identify vulnerabilities in application source code.

Potential focus areas:
* Injection vulnerabilities
* Insecure API usage
* Authentication/authorization issues
* Hardcoded secrets
* Unsafe coding patterns

**Tools:** Semgrep

### Dependency Analysis
Analyze project dependencies for known vulnerabilities.

For Maven-based Spring Boot applications:

```
pom.xml
  ↓
Dependency Analysis
  ↓
Known Vulnerabilities
  ↓
Normalized Findings
```

**Tools:** OWASP Dependency-Check

### Container Analysis
The platform will eventually analyze Docker images associated with the application.

```
Spring Boot Application
  ↓
Docker Build
  ↓
Docker Image
  ↓
Security Analysis
  ↓
Vulnerability Findings
```

**Tools:** Trivy

---

## 8. Docker Architecture

SecureOps itself will be containerized to provide a reproducible development and deployment environment.

```
Docker Host
├── nginx (:80 / :443)
├── secureops-api (Spring Boot)
├── analysis-worker (SAST / Dependency / Container)
├── postgresql / mysql
│
Named Volumes:
├── db_data
└── scan_workspace
```

Docker Compose will initially be used for local development and service orchestration before migrating to Kubernetes in Phase 7.

---

## 9. Planned DevOps Workflow

The project will gradually introduce CI/CD automation.

```
Developer
  ↓
Git Push
  ↓
GitHub
  ↓
CI Pipeline
├── Build
├── Test
├── Code Quality
├── Security Scan
└── Docker Build
  ↓
Docker Image
  ↓
Deployment
```

Planned DevOps technologies include:
* GitHub
* Docker
* Docker Compose
* Jenkins / GitHub Actions
* Kubernetes
* Security scanning tools
* Container registry

---

## 10. Technology Stack

### Backend
* Java 21
* Spring Boot 4.1.1
* Spring Web
* Spring Data JPA
* Spring Security (Phase 6+)
* Maven
* REST APIs
* Jakarta Validation

### Database
* PostgreSQL 14+

### Security
* SAST (Semgrep)
* Dependency vulnerability scanning (OWASP Dependency-Check)
* Container scanning (Trivy)
* Secret detection

### DevOps
* Git
* GitHub
* Docker
* Docker Compose
* Jenkins / GitHub Actions
* Kubernetes

### AI (Future)
* LLM-based vulnerability explanation
* Risk prioritization
* Remediation assistance

---

## 11. Development Roadmap

### Phase 0 — Foundation ✅
- [x] Define project scope
- [x] Create comprehensive README
- [x] Define initial architecture
- [x] Initialize Spring Boot project
- [x] Configure Maven
- [x] Create package structure
- [x] Health check endpoint

### Phase 1 — Project Management ✅
- [x] Project entity
- [x] Project repository (Spring Data JPA)
- [x] Project service
- [x] Project controller
- [x] Request/response DTOs
- [x] Validation and error handling
- [x] Multi-project isolation foundation

### Phase 2 — Pipeline Management (Coming)
- [ ] Pipeline entity
- [ ] Pipeline repository
- [ ] Pipeline service
- [ ] Pipeline controller
- [ ] Pipeline DTOs

### Phase 3 — Scan Management (Coming)
- [ ] Scan entity
- [ ] Scan repository
- [ ] Scan service
- [ ] Scan controller
- [ ] Scan DTOs

### Phase 4 — Report Ingestion (Coming)
- [ ] Report entity
- [ ] Report upload endpoints
- [ ] Support for Trivy JSON

### Phase 5 — Finding Normalization (Coming)
- [ ] Finding entity
- [ ] Finding repository
- [ ] Finding service
- [ ] Common Finding model

### Phase 6 — Trivy Parser (Coming)
- [ ] Parse Trivy JSON reports
- [ ] Extract vulnerabilities
- [ ] Normalize to Finding model

### Phase 7 — Risk Engine (Coming)
- [ ] Deterministic risk scoring
- [ ] Severity weighting (CRITICAL=10, HIGH=7, MEDIUM=4, LOW=1)
- [ ] Risk calculation

### Phase 8 — Policy Engine (Coming)
- [ ] Policy entity
- [ ] Configurable rules
- [ ] Policy evaluation

### Phase 9 — Security Decision (Coming)
- [ ] Decision entity
- [ ] PASS/BLOCK logic
- [ ] Reason generation

### Phase 10 — End-to-End Demo (Coming)
- [ ] Complete Postman workflow
- [ ] Multiple security tools
- [ ] Final reporting

### Phase 11 — Docker (Coming)
- [ ] Create Dockerfile
- [ ] Docker Compose setup
- [ ] Container orchestration

### Phase 12 — CI/CD (Coming)
- [ ] GitHub Actions pipeline
- [ ] Automated security scans
- [ ] Image build and push

### Phase 13 — AI Layer (Coming)
- [ ] Integrate LLM
- [ ] Vulnerability explanation
- [ ] Remediation suggestions
- [ ] Risk prioritization

### Phase 14 — Kubernetes (Coming)
- [ ] Kubernetes manifests
- [ ] Services and ConfigMaps
- [ ] Persistent storage
- [ ] Production deployment

---

## 12. Current Implementation Status

**Current Phase: Phase 1 — Project Management ✅**

### Completed
- Phase 0: Spring Boot foundation with health check
- Phase 1: Project management with multi-project isolation

### In Progress
- None (waiting for next milestone)

### Upcoming
- Phase 2: Pipeline management
- Phase 3: Scan management
- Phase 4+: Security analysis and reporting

---

## 13. Quick Start

### Prerequisites
- Java 21+
- Maven 3.8+
- PostgreSQL 14+

### Build & Run
```bash
cd secureops
mvn clean package -DskipTests
java -jar target/secureops-0.0.1-SNAPSHOT.jar
```

### Test Health Endpoint
```bash
curl http://localhost:8080/api/health
```

Expected response:
```json
{"status":"UP","service":"SecureOps","database":"UP"}
```

### Test Project API
```bash
# Create project
curl -X POST http://localhost:8080/api/projects \
  -H "Content-Type: application/json" \
  -d '{"name":"Payment API","repositoryUrl":"https://github.com/company/payment-api"}'

# List projects
curl http://localhost:8080/api/projects

# Get specific project
curl http://localhost:8080/api/projects/{projectId}
```

---

## 14. Long-Term Vision

SecureOps AI is intended to evolve from a security-analysis backend into a complete DevSecOps platform where a developer can submit an application and receive a centralized security assessment containing:

```
Application
  ↓
Automated Security Analysis
  ↓
Vulnerability Detection
  ↓
Risk Prioritization
  ↓
AI Explanation
  ↓
Remediation Guidance
  ↓
Secure Deployment
```

The long-term objective is to make security analysis an integrated part of the application development and deployment lifecycle rather than a separate manual activity.

---

## 15. Contributing

This project is currently in active development. Contributions are welcome once the foundation stabilizes.

---

## 16. License

TBD

---

## 17. Contact

For questions or collaboration, please open an issue on GitHub.
