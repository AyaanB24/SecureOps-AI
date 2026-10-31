# Phase 1: Authentication Codebase Audit and Implementation Plan

**Date:** October 27, 2026  
**Status:** Audit Complete - Ready for Phase 2  
**Scope:** Inspection and documentation only - NO changes to existing code

---

## Executive Summary

The SecureOps AI application is a fully functional Spring Boot + React DevSecOps platform with **no existing authentication**. All APIs are currently public and unauthenticated. This audit documents the current architecture, identifies integration points, and proposes a phased authentication implementation that preserves all existing functionality.

**Key Finding:** The application is ready for secure authentication. No breaking changes to existing architecture are required. Authentication can be added incrementally using industry-standard patterns (JWT, BCrypt, Spring Security) compatible with the existing tech stack.

---

## Part 1: Architecture Overview

### 1.1 Technology Stack

**Backend:**
- **Java:** 21
- **Spring Boot:** 3.4.0
- **Database:** PostgreSQL 42.7.3 (JDBC driver)
- **Build Tool:** Maven 3.x (implied by Spring Boot parent)
- **ORM:** Spring Data JPA / Hibernate
- **Validation:** Spring Boot Starter Validation
- **Utilities:** Lombok, Jackson
- **Testing:** JUnit 5, Spring Boot Test

**Frontend:**
- **Framework:** React 18.2.0
- **Build Tool:** Vite 5.0.0
- **Router:** React Router DOM 6.20.0
- **HTTP Client:** Axios 1.6.0
- **State Management:** TanStack Query (React Query) 5.25.0
- **Styling:** Tailwind CSS 3.3.0, PostCSS
- **UI Icons:** Lucide React 0.294.0
- **Testing:** Vitest 1.6.1, React Testing Library

**Current Deployment:**
- Backend: `http://localhost:8081`
- Frontend: `http://localhost:5173` (Vite dev server)
- Database: PostgreSQL on `localhost:5432`
- Jenkins: Docker-based, reaches backend via `http://host.docker.internal:8081`

### 1.2 Current CORS Configuration

**File:** `secureops/src/main/java/com/secureops/common/config/CorsConfig.java`

**Current Config:**
```java
registry.addMapping("/api/**")
        .allowedOrigins("http://localhost:5173", "http://localhost:3000")
        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true)
        .maxAge(3600);
```

**Implication:** CORS is configured and will work with credentialed requests (cookies or auth headers). No changes needed for authentication.

### 1.3 Spring Security Status

**Finding:** No Spring Security dependency or configuration exists.

- `pom.xml` does NOT include `spring-boot-starter-security`
- No `SecurityConfig.java` or `SecurityConfiguration.java`
- No authentication filters or interceptors
- All endpoints are completely open

**Action for Phase 3:** Add `spring-boot-starter-security` and implement stateless JWT configuration.

---

## Part 2: Relevant Files and Their Purposes

### Backend Core Modules

#### 2.1 Project Module

**Files:**
- `src/main/java/com/secureops/project/Project.java` — JPA Entity, owns pipelines, scans, policies
- `src/main/java/com/secureops/project/ProjectController.java` — REST endpoints
- `src/main/java/com/secureops/project/ProjectService.java` — Business logic
- `src/main/java/com/secureops/project/ProjectRepository.java` — Data access

**Key Relationships:**
- One Project has many Pipelines (cascade delete)
- One Project has many Scans (no cascade)
- One Project has many Policies (cascade delete)

**Database Table:** `project`
- Columns: `id` (UUID PK), `name` (unique), `repository_url`, `created_at`

**API Endpoints:**
- `POST /api/projects` — Create project
- `GET /api/projects` — List all projects
- `GET /api/projects?name={name}` — Find project by name (Jenkins use)
- `GET /api/projects/{projectId}` — Get specific project
- `DELETE /api/projects/{projectId}` — Delete project

**Authentication Impact:** Must add **project ownership verification**. Only project owner can modify/delete their project. Owner field to be added in Phase 5.

#### 2.2 Pipeline Module

**Files:**
- `src/main/java/com/secureops/pipeline/Pipeline.java` — JPA Entity
- `src/main/java/com/secureops/pipeline/PipelineController.java` — REST endpoints
- `src/main/java/com/secureops/pipeline/PipelineService.java` — Business logic
- `src/main/java/com/secureops/pipeline/PipelineRepository.java` — Data access

**Key Relationships:**
- Many Pipelines belong to one Project (FK)
- One Pipeline has many Scans (cascade delete)
- Composite unique constraint: (projectId, jobName, buildNumber)

**Database Table:** `pipeline`
- Columns: `id`, `project_id` (FK), `provider`, `job_name`, `build_number`, `branch`, `commit_sha`, `created_at`

**API Endpoints:**
- `POST /api/projects/{projectId}/pipelines` — Create pipeline
- `GET /api/projects/{projectId}/pipelines` — List pipelines
- `GET /api/pipelines/{pipelineId}` — Get specific pipeline

**Authentication Impact:** Must verify user is authorized to access the parent project.

#### 2.3 Scan Module

**Files:**
- `src/main/java/com/secureops/scan/Scan.java` — JPA Entity
- `src/main/java/com/secureops/scan/ScanController.java` — REST endpoints
- `src/main/java/com/secureops/scan/ScanService.java` — Business logic
- `src/main/java/com/secureops/scan/ScanRepository.java` — Data access

**Key Relationships:**
- Many Scans belong to one Project (FK)
- Many Scans belong to one Pipeline (FK, not null)
- One Scan has many Reports (cascade delete)
- One Scan has one SecurityDecision

**Database Table:** `scan`
- Columns: `id`, `project_id` (FK), `pipeline_id` (FK, NOT NULL), `environment`, `status`, `started_at`, `completed_at`

**API Endpoints:**
- `POST /api/projects/{projectId}/scans` — Create scan
- `GET /api/projects/{projectId}/scans` — List scans for project
- `GET /api/scans/{scanId}` — Get specific scan

**Authentication Impact:** Must verify project access and prevent cross-project scans.

#### 2.4 Report Module

**Files:**
- `src/main/java/com/secureops/report/Report.java` — JPA Entity
- `src/main/java/com/secureops/report/ReportController.java` — REST endpoints
- `src/main/java/com/secureops/report/ReportService.java` — Business logic

**Key Relationships:**
- Many Reports belong to one Scan (FK)
- Composite unique constraint: (scanId, tool)

**API Endpoints:**
- `POST /api/scans/{scanId}/reports?tool=TRIVY` — Upload Trivy JSON report
- `GET /api/scans/{scanId}/reports` — List reports for scan
- `POST /api/scans/{scanId}/reports/{reportId}/process` — Parse and extract findings

**Authentication Impact:** Must verify scan access (transitively, project access).

#### 2.5 Finding Module

**Files:**
- `src/main/java/com/secureops/finding/Finding.java` — JPA Entity
- `src/main/java/com/secureops/finding/FindingController.java` — REST endpoints
- `src/main/java/com/secureops/finding/FindingService.java` — Business logic

**Key Relationships:**
- Many Findings belong to one Scan (FK)
- Many Findings belong to one Report (FK)
- FingerprintService implements deterministic SHA-256 hashing for deduplication

**API Endpoints:**
- `GET /api/scans/{scanId}/findings` — List findings for scan
- `GET /api/findings/{findingId}` — Get specific finding
- `POST /api/scans/{scanId}/findings?reportId={reportId}` — Create finding (internal test use)

**Authentication Impact:** Must verify scan access.

#### 2.6 Policy Module

**Files:**
- `src/main/java/com/secureops/policy/Policy.java` — JPA Entity
- `src/main/java/com/secureops/policy/PolicyController.java` — REST endpoints
- `src/main/java/com/secureops/policy/PolicyService.java` — Business logic

**Key Relationships:**
- Many Policies belong to one Project (FK)
- Composite unique constraint: (projectId, environment)
- Three environments: DEVELOPMENT, STAGING, PRODUCTION

**Database Table:** `policy`
- Columns: `id`, `project_id` (FK), `environment`, `max_critical`, `max_high`, `max_risk_score`, `created_at`, `updated_at`

**API Endpoints:**
- `POST /api/projects/{projectId}/policies` — Create policy
- `GET /api/projects/{projectId}/policies` — List policies
- `GET /api/projects/{projectId}/policies/{environment}` — Get specific policy
- `PUT /api/projects/{projectId}/policies/{environment}` — Update policy

**Authentication Impact:** Only ADMIN or project owner can modify policies.

#### 2.7 Decision Module

**Files:**
- `src/main/java/com/secureops/decision/SecurityDecision.java` — JPA Entity
- `src/main/java/com/secureops/decision/DecisionController.java` — REST endpoints
- `src/main/java/com/secureops/decision/DecisionService.java` — Business logic
- `src/main/java/com/secureops/decision/DecisionEngine.java` — Deterministic PASS/BLOCK logic

**Key Relationships:**
- One SecurityDecision belongs to one Scan (1:1 unique)
- Combines findings, risk score, and policy to generate verdict
- Verdict: PASS or BLOCK
- Includes detailed reason for verdict

**Database Table:** `security_decision`
- Columns: `id`, `scan_id` (FK, unique), `decision`, `risk_score`, `critical_count`, `high_count`, `medium_count`, `low_count`, `reason`, `created_at`

**API Endpoints:**
- `GET /api/scans/{scanId}/decision` — Get security decision (Jenkins critical use)
- `GET /api/scans/{scanId}/risk` — Get risk score breakdown

**Authentication Impact:** Jenkins must authenticate to retrieve decisions.

#### 2.8 Health Check

**File:** `src/main/java/com/secureops/common/controller/HealthController.java`

**Endpoint:** `GET /api/health` (public, no auth needed)

**Use:** Frontend and Jenkins health checks before API calls.

**Action:** Keep this endpoint public for monitoring.

### Frontend Pages and Components

#### 2.9 Main Routes

**File:** `frontend-app/src/App.tsx`

**Current Routes (all public, no auth):**
- `/` → Dashboard
- `/projects` → Projects list
- `/pipelines` → Pipelines & Scans
- `/scans` → Scans detail
- `/findings` → Findings browser
- `/decisions` → Security decisions
- `/policies` → Policy management
- `/ai-analysis` → AI recommendations (coming soon)
- `/settings` → Settings & health
- `*` → 404 Not Found

**Action for Phase 6:** Add `/login` and `/register` routes. Add route protection for all existing routes.

#### 2.10 API Client

**File:** `frontend-app/src/lib/axios-config.ts`

**Configuration:**
- Base URL: `http://localhost:8081/api` (or VITE_API_BASE_URL env var)
- Content-Type: application/json
- Request interceptor: Currently empty (hooks for future auth logic)
- Response interceptor: Handles Axios errors and converts to ApiError DTO

**Action for Phase 6:** Update request interceptor to inject JWT token from storage. Update response interceptor to handle 401/403 errors and redirect to login.

#### 2.11 Toast Context

**File:** `frontend-app/src/contexts/ToastContext.tsx`

**Purpose:** Global toast notifications for success/error feedback.

**Action:** Reuse for authentication messages (login success, invalid credentials, session expired, etc.).

#### 2.12 Layout and Navigation

**File:** `frontend-app/src/layouts/MainLayout.tsx`

**Purpose:** Wraps all pages with header, sidebar, and navigation.

**Action for Phase 6:** Update layout to show authenticated user name and logout button. Show login/register links when not authenticated.

---

## Part 3: Database Schema and Relationships

### 3.1 Current Schema Diagram

```
project (1)
├── pipelines (M)
│   ├── scans (M)
│   │   ├── reports (M)
│   │   │   └── findings (M)
│   │   └── security_decision (1)
│   └── [would be CASCADE DELETE]
├── scans (M)
│   └── [same as above, accessed through pipeline]
└── policies (M)
    └── [environment-scoped thresholds]

Jenkins uses:
├── Find/Create Project (by name)
├── Create Pipeline (job-specific)
├── Create Scan (with environment)
├── Upload Report (Trivy JSON)
├── Process Report (extract findings)
└── Get Decision (PASS/BLOCK enforcement)
```

### 3.2 Planned User Schema (Phase 2)

```sql
CREATE TABLE "user" (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,  -- BCrypt hashed, never plaintext
    role VARCHAR(50) NOT NULL DEFAULT 'USER',  -- ADMIN or USER
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_email (email)
);

-- Project ownership tracking (Phase 5)
ALTER TABLE project ADD COLUMN owner_id UUID REFERENCES "user"(id);
ALTER TABLE project ADD INDEX idx_project_owner (owner_id);

-- Jenkins service account (Phase 7)
-- Stored as a special user with role JENKINS_SERVICE
-- Or separate jenkins_credential table
```

### 3.3 Backward Compatibility Strategy

**Challenge:** Existing projects in database have NO owner. Jenkins-created projects need special handling.

**Solution (Phase 5):** 
- Add optional `owner_id` FK to project table
- Migrate existing projects to a system service account (null owner = system)
- Allow Jenkins to create projects using service credentials
- Allow admins to assign ownership post-creation

---

## Part 4: Existing API Endpoint Inventory

### 4.1 Public Endpoints (Currently Unauthenticated)

| Method | Endpoint | Purpose | Jenkins Use | Auth Action |
|--------|----------|---------|-------------|-------------|
| GET | `/api/health` | Health check | Yes (pre-flight) | **Keep public** |
| POST | `/api/projects` | Create project | Yes (find or create) | Require auth (USER role) |
| GET | `/api/projects` | List projects | No | Require auth (USER role) |
| GET | `/api/projects?name={name}` | Find by name | Yes (Jenkins) | Require service auth |
| GET | `/api/projects/{id}` | Get project | Yes (verify) | Require auth (project member) |
| DELETE | `/api/projects/{id}` | Delete project | No | Require auth (project owner + ADMIN) |
| POST | `/api/projects/{id}/pipelines` | Create pipeline | Yes (Jenkins) | Require service auth |
| GET | `/api/projects/{id}/pipelines` | List pipelines | Yes (Jenkins) | Require auth (project member) |
| GET | `/api/pipelines/{id}` | Get pipeline | Yes (Jenkins) | Require auth (project member) |
| POST | `/api/projects/{id}/scans` | Create scan | Yes (Jenkins) | Require service auth |
| GET | `/api/projects/{id}/scans` | List scans | Yes (Jenkins) | Require auth (project member) |
| GET | `/api/scans/{id}` | Get scan | Yes (Jenkins) | Require auth (project member) |
| POST | `/api/scans/{id}/reports?tool=TRIVY` | Upload report | Yes (Jenkins) | Require service auth |
| GET | `/api/scans/{id}/reports` | List reports | Yes (Jenkins) | Require auth (project member) |
| POST | `/api/scans/{id}/reports/{id}/process` | Process report | Yes (Jenkins) | Require service auth |
| GET | `/api/scans/{id}/findings` | List findings | Yes (Jenkins) | Require auth (project member) |
| GET | `/api/findings/{id}` | Get finding | No | Require auth (project member) |
| POST | `/api/scans/{id}/findings` | Create finding (test) | No | Require auth (project member) |
| GET | `/api/scans/{id}/risk` | Get risk score | Yes (Jenkins) | Require service auth |
| GET | `/api/scans/{id}/decision` | Get decision | **Yes (critical!)** | **Require service auth** |
| POST | `/api/projects/{id}/policies` | Create policy | No | Require auth (ADMIN or owner) |
| GET | `/api/projects/{id}/policies` | List policies | No | Require auth (project member) |
| GET | `/api/projects/{id}/policies/{env}` | Get policy | No | Require auth (project member) |
| PUT | `/api/projects/{id}/policies/{env}` | Update policy | No | Require auth (ADMIN or owner) |

**Note:** Jenkins API calls marked "Yes" must work with service account credentials.

### 4.2 Error Response Format

**Current (existing):**
```json
{
  "errorCode": "VALIDATION_ERROR|NOT_FOUND|DUPLICATE",
  "message": "Human-readable message"
}
```

**HTTP Status Codes Used:**
- 200 OK — Successful GET
- 201 Created — Successful POST resource creation
- 204 No Content — Successful DELETE
- 400 Bad Request — Validation errors
- 404 Not Found — Resource not found
- 409 Conflict — Duplicate resource or constraint violation
- 500 Internal Server Error — Unexpected error

**Action:** Add authentication error responses:
- 401 Unauthorized — Invalid/missing token
- 403 Forbidden — Valid token, insufficient permissions

---

## Part 5: Jenkins Integration Flow

### 5.1 Current Workflow (Unauthenticated)

**Jenkins → SecureOps API Flow:**

```
1. Checkout code from GitHub
2. Build Docker image
3. Run Trivy scan
   └─ Generates trivy-report.json
4. SecureOps API calls:
   a. GET /api/projects?name={PROJECT_NAME}
      → Find or create project
   b. POST /api/projects/{projectId}/pipelines
      → Register build pipeline
   c. POST /api/projects/{projectId}/scans
      → Create scan record
   d. POST /api/scans/{scanId}/reports?tool=TRIVY
      → Upload Trivy JSON
   e. POST /api/scans/{scanId}/reports/{reportId}/process
      → Extract findings
   f. GET /api/scans/{scanId}/decision
      → Get PASS/BLOCK verdict ← **CRITICAL**
5. Enforce in Jenkins:
   if decision == "PASS": continue to deployment
   if decision == "BLOCK": fail the build
```

### 5.2 Backend URLs Used

- Development: `http://localhost:8081/api`
- Jenkins (Docker): `http://host.docker.internal:8081/api`

### 5.3 Current Authentication

None. All endpoints are open.

### 5.4 Authentication Changes (Phase 7)

**Option A: API Key/Token (Simpler)**
- Create system service account with JENKINS_SERVICE role
- Generate long-lived JWT or API key
- Store in Jenkins Credentials manager
- Inject via environment variable in Jenkinsfile
- Use Bearer token in all API calls

**Option B: OAuth2 Client Credentials (More secure)**
- Implement OAuth2 resource server in Spring Security
- Jenkins requests access token
- Use access token for subsequent calls

**Recommendation:** Start with Option A (simpler, sufficient for DevOps automation). Option B can be added later if needed.

---

## Part 6: Baseline Tests - Current State

### 6.1 Backend Build

**Command:**
```bash
cd secureops
mvn clean compile
```

**Expected Result:** BUILD SUCCESS

**Actual Result (if executed):** [Would be executed in Phase 2]

### 6.2 Backend Unit Tests

**Command:**
```bash
cd secureops
mvn test
```

**Test Coverage:** [To be determined - no test count provided in current inspection]

**Note:** No authentication tests exist (auth not yet implemented).

### 6.3 Frontend Build

**Command:**
```bash
cd frontend-app
npm run build
```

**Expected Result:** Successful build, ✓ 0 TypeScript errors

**Actual Result (baseline):** [Confirmed in Phase 0 as working]

### 6.4 Frontend Tests

**Command:**
```bash
cd frontend-app
npm run test
```

**Coverage:** [To be determined]

### 6.5 Full Stack Test - Health Check

**Command:**
```bash
# Terminal 1: Start backend
cd secureops
mvn spring-boot:run

# Terminal 2: Start frontend
cd frontend-app
npm run dev

# Terminal 3: Test health endpoint
curl http://localhost:8081/api/health | jq .
```

**Expected Response:**
```json
{
  "status": "UP",
  "service": "SecureOps",
  "database": "UP"
}
```

### 6.6 Jenkins Integration Test

**Command (from JENKINS_INTEGRATION_TESTING.md):**
```bash
./jenkins-integration-test.sh
```

**Current Behavior:** All endpoints return 200/201 (no auth required).

**After Auth:** Requires Jenkins service account credentials.

---

## Part 7: Proposed Authentication Implementation Plan

### Phase 2: User Entity and Database Migration

**Objective:** Add user persistence layer.

**Deliverables:**
1. Create `User.java` JPA entity with:
   - UUID id
   - Email (unique, normalized)
   - Name
   - BCrypt password hash
   - Role (enum: ADMIN, USER)
   - Enabled flag
   - Timestamps
   
2. Create `UserRepository.java` with:
   - `findByEmail(email): Optional<User>`
   - `existsByEmail(email): boolean`

3. Add Flyway or Hibernate migration:
   - Create `user` table
   - Add indexes on email

4. **No frontend or API changes yet**

**Estimated Lines:** 100 backend lines, 1 migration file

---

### Phase 3: Registration, Login, and Spring Security

**Objective:** Implement authentication logic without JWT.

**Deliverables:**
1. Add dependencies:
   - `spring-boot-starter-security`
   - `jjwt` (JWT library)
   - (BCrypt included in Spring Security)

2. Create auth endpoints:
   - `POST /api/auth/register` — Public, creates USER role
   - `POST /api/auth/login` — Public, returns JWT
   - `GET /api/auth/me` — Protected, returns current user

3. Create DTOs:
   - `RegisterRequest` (name, email, password)
   - `LoginRequest` (email, password)
   - `LoginResponse` (token, user)
   - `UserResponse` (id, email, name, role, no password)

4. Create `AuthService` with:
   - `register(RegisterRequest): User`
   - `login(LoginRequest): LoginResponse`
   - Password validation (length ≥ 8, complexity rules)
   - Email uniqueness check

5. Create Spring Security configuration (stateless JWT):
   - `SecurityConfig.java` with JWT filter
   - `JwtTokenProvider` — sign/validate tokens
   - `CustomUserDetailsService` — load User from database

6. Add validation:
   - Email format and uniqueness
   - Password length (≥ 8) and complexity
   - Error responses (400 for validation, 409 for duplicate email)

**Estimated Lines:** 400 backend lines

---

### Phase 4: Endpoint Authorization

**Objective:** Protect existing endpoints with @PreAuthorize and project-level checks.

**Deliverables:**
1. Add role-based authorization:
   - `@PreAuthorize("hasRole('ADMIN')")` for admin-only endpoints
   - `@PreAuthorize("hasAnyRole('ADMIN', 'USER')")` for authenticated users

2. Add project-level authorization checks in services:
   - Verify current user owns or is member of project before allowing access
   - Return 403 Forbidden if not authorized

3. Update controllers to extract current user:
   - Use `@AuthenticationPrincipal` or `SecurityContextHolder`
   - Pass user to service layer

4. Create `ProjectAuthorizationService`:
   - `checkProjectAccess(userId, projectId): void` — throws 403 if denied
   - `isProjectOwner(userId, projectId): boolean`

5. Apply to all project-scoped endpoints:
   - GET/POST/PUT/DELETE on pipelines, scans, reports, findings, policies
   - GET/POST/PUT/DELETE on projects

6. Error handling:
   - 401 for missing/invalid token
   - 403 for insufficient permissions
   - Keep error messages generic (don't leak whether project exists)

**Estimated Lines:** 300 backend lines

---

### Phase 5: Project Ownership and Membership

**Objective:** Track who owns/can access each project.

**Deliverables:**
1. Add `owner_id` (UUID FK to User) to Project entity
   - Optional field (for backward compatibility)
   - Indicates project creator

2. Create database migration:
   - Add `owner_id` column to project table
   - Allow NULL for existing Jenkins-created projects
   - Add FK constraint to user table

3. Create `ProjectMembership` entity (optional for now, planned for future):
   - userId, projectId, role (owner, editor, viewer)
   - Composite PK and unique constraint

4. Update ProjectService:
   - On create: Set owner_id to current user
   - Enforce: Only owner + ADMIN can delete
   - Enforce: Only owner + ADMIN can modify policies
   - Query: Filter user's projects (where owner_id = current user OR role = ADMIN)

5. Handle Jenkins projects:
   - Option A: Assign to system service account (create once)
   - Option B: Allow null owner for service-account-created projects, admins can access

6. Existing data migration:
   - Set owner_id = NULL for projects without owner (safe, non-destructive)
   - Document workaround for admins

**Estimated Lines:** 200 backend lines + migration

---

### Phase 6: Frontend Authentication Integration

**Objective:** Add login, registration, and protected routes to React app.

**Deliverables:**
1. Create login page (`/login`):
   - Email and password inputs
   - Form validation
   - Submit to `/api/auth/login`
   - Store JWT in secure storage (localStorage for now, HttpOnly cookie later)
   - Redirect to dashboard on success
   - Show error messages on failure

2. Create registration page (`/register`):
   - Name, email, password, confirm password inputs
   - Form validation
   - Submit to `/api/auth/register`
   - Auto-login after successful registration
   - Show validation errors
   - Link to login page

3. Update API client (`axios-config.ts`):
   - Request interceptor: Inject JWT from storage into Authorization header
   - Response interceptor: On 401, clear token and redirect to login
   - Handle 403 (show Forbidden page)

4. Create protected route wrapper:
   - `<ProtectedRoute>` component
   - Check if user is authenticated
   - Redirect to login if not
   - Pass user info to routes

5. Update routing (`App.tsx`):
   - Add `/login` and `/register` routes (public)
   - Wrap existing routes in `<ProtectedRoute>`
   - Add `<Forbidden>` page for 403 errors

6. Update layout (`MainLayout.tsx`):
   - Show logged-in user name in header
   - Add logout button
   - Show/hide nav based on auth state

7. Add auth context/hook:
   - `useAuth()` hook to access current user
   - `AuthContext` to store user and token
   - `getCurrentUser()` on app load
   - Handle token expiry (401 triggers re-login)

8. Create `auth/` pages:
   - `LoginPage.tsx`
   - `RegisterPage.tsx`
   - `ForbiddenPage.tsx` (403 error)
   - `ExpiredSessionPage.tsx` (optional)

**Estimated Lines:** 600 frontend lines

---

### Phase 7: Jenkins Service Account Authentication

**Objective:** Allow Jenkins to authenticate without breaking existing automation.

**Deliverables:**
1. Create system service account:
   - Email: `jenkins@secureops.internal`
   - Name: "Jenkins Automation"
   - Role: `JENKINS_SERVICE` (new role)
   - Enabled: true
   - Password: Generated securely, stored in Jenkins Credentials manager

2. Create service account generation script:
   - `scripts/create-jenkins-account.sh` (not committed to repo)
   - Generates account and outputs credentials
   - Instructions to add to Jenkins Credentials manager

3. Update Jenkinsfile to use credentials:
   - `withCredentials([string(credentialsId: '...', variable: 'JENKINS_TOKEN')])` block
   - All API calls include: `-H "Authorization: Bearer $JENKINS_TOKEN"`
   - Minimal changes to existing Jenkinsfile structure

4. Add role-based authorization for Jenkins:
   - `JENKINS_SERVICE` role can:
     - Find/create projects (own name)
     - Create pipelines for their projects
     - Create scans
     - Upload reports
     - Process reports
     - Retrieve decisions
   - Cannot: Delete projects, modify policies, view other users' projects

5. Create admin endpoint to manage service accounts:
   - `POST /api/admin/service-accounts` — Create service account
   - `DELETE /api/admin/service-accounts/{id}` — Disable account
   - `POST /api/admin/service-accounts/{id}/rotate-token` — Rotate API token

**Estimated Lines:** 200 backend lines + Jenkinsfile updates

---

### Phase 8: Testing and Regression Verification

**Objective:** Ensure authentication doesn't break existing functionality.

**Deliverables:**
1. Backend Tests:
   - Registration: valid input, duplicate email, invalid email, short password
   - Login: correct/incorrect credentials, disabled account
   - Token: valid JWT, expired JWT, malformed JWT, missing JWT
   - Authorization: user can access own project, cannot access others' project, admin can access all
   - Jenkins: service account can perform required operations

2. Frontend Tests:
   - Login flow: email/password, error messages, redirect
   - Registration: form validation, success/error states
   - Protected routes: redirects to login if not authenticated
   - Logout: clears token, redirects to login
   - Expired session: automatic logout and redirect

3. End-to-End Regression:
   - Jenkins workflow with service account:
     - Checkout code
     - Build image
     - Run Trivy
     - Upload report
     - Get decision
     - Enforce PASS/BLOCK
   - Both PASS and BLOCK scenarios

4. Existing Feature Verification:
   - Projects, pipelines, scans still create successfully
   - Reports process correctly
   - Findings extracted accurately
   - Decisions generated with correct thresholds
   - No data loss or corruption

---

## Part 8: Risks and Regression Checklist

### 8.1 Identified Risks

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|-----------|
| Existing Jenkins automation breaks | High | Critical | Phase 7: Service account with backward-compatible auth |
| Cross-project data access due to auth bypass | Medium | Critical | Phase 5: Strict project ownership checks, unit tests |
| Token stolen from localStorage | Medium | High | Phase 6: Use HttpOnly cookies after Phase 6 |
| Password hash exposed in migration | Low | Medium | Phase 2: Never commit plaintext passwords, review migration |
| Existing projects lose ownership | Low | Medium | Phase 5: Migrate to system account, document workaround |
| Performance degradation from auth checks | Low | Low | Use caching, lazy-load user details, profile after Phase 4 |
| CSRF attacks on cookie-based auth | Low | Medium | Phase 6+: Implement CSRF tokens if using cookies |

### 8.2 Regression Testing Checklist

**Must Verify After Each Phase:**

- [ ] Backend compiles without errors: `mvn clean compile`
- [ ] Frontend builds without errors: `npm run build`
- [ ] Health check works: `curl http://localhost:8081/api/health`
- [ ] Existing projects still accessible through API
- [ ] Existing pipelines, scans, findings, policies still accessible
- [ ] Report processing still works (Trivy JSON extraction)
- [ ] Security decisions still generated correctly (PASS/BLOCK logic unchanged)
- [ ] Risk score calculation unchanged
- [ ] Database has no data corruption or missing records
- [ ] Jenkins integration test passes (with service account in Phase 7+)
- [ ] Both PASS and BLOCK decisions enforced correctly
- [ ] No unintended 401/403 errors on unauthenticated endpoints
- [ ] Error messages don't leak sensitive information

---

## Part 9: Implementation Status and Next Steps

### 9.1 Current Status

**Phase 1 (This Document):** ✅ COMPLETE
- Codebase audited
- Architecture documented
- Risks identified
- Plan proposed

### 9.2 Next Steps

1. **Review this document** for accuracy and completeness
2. **Approve the plan** or request modifications
3. **Proceed to Phase 2** when ready (user entity + migration)
4. After each phase, run regression tests before proceeding

---

## Part 10: Git and Documentation

### 10.1 Current Repository State

**Files NOT modified in Phase 1:**
- All source code files
- Configuration files
- Database schema
- Jenkinsfile (not found, may not exist)
- Frontend components

**New File Created:**
- `docs/phase-auth-1.md` (this audit report)

### 10.2 Review Commands

To verify no files were modified:
```bash
git status  # Should show only phase-auth-1.md as untracked
git diff HEAD  # Should show no changes to existing files
```

To add and commit this phase:
```bash
git add docs/phase-auth-1.md
git commit -m "docs: phase 1 - authentication codebase audit and implementation plan"
git log --oneline | head -5
```

### 10.3 Suggested Commit

**Single-line commit message:**
```
docs: add phase 1 authentication audit - codebase inspection, API inventory, implementation roadmap
```

---

## Conclusion

The SecureOps AI application is well-architected and ready for authentication implementation. The codebase is clean, well-documented, and follows Spring Boot conventions. Authentication can be added incrementally using industry-standard patterns without disrupting existing functionality.

**Key Success Factors:**
1. Phased approach ensures thorough testing at each stage
2. Backward compatibility preserved through careful project ownership design
3. Jenkins integration handled separately in Phase 7
4. Existing functionality maintained throughout all phases

**Timeline Estimate:**
- Phase 2: 2-3 hours
- Phase 3: 2-3 hours
- Phase 4: 2-3 hours
- Phase 5: 2-3 hours
- Phase 6: 3-4 hours
- Phase 7: 2-3 hours
- Phase 8: 2-3 hours
- **Total: 15-22 hours of development**

**Approval Required Before Proceeding:**
- [  ] Confirm architecture understanding
- [  ] Approve proposed authentication technology choices
- [  ] Approve Phase 2 start

---

**Audit Completed:** October 27, 2026  
**Status:** Ready for Phase 2 Approval  
**Auditor:** Kiro AI Agent  

