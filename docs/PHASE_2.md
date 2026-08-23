# Phase 2: Pipeline Management

**Status:** ✅ Complete

**Objective:** Implement CI/CD pipeline management with multi-project isolation. Each project can have multiple independent pipelines.

---

## 1. Architecture Overview

### Relationship Model
```
Project (1)
    ↓
    └─→ Pipeline (N)
```

A project can have zero or more pipelines. Each pipeline must reference exactly one project.

### PostgreSQL Foreign Key Relationship
```
CREATE TABLE project (
    id uuid PRIMARY KEY
);

CREATE TABLE pipeline (
    id uuid PRIMARY KEY,
    project_id uuid NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    ...
);
```

**Key Points:**
- `project_id` is a foreign key to `project.id`
- `ON DELETE CASCADE`: If a project is deleted, all its pipelines are deleted
- `NOT NULL`: Every pipeline must belong to a project (referential integrity)
- Index on `project_id` enables fast filtering: `SELECT * FROM pipeline WHERE project_id = ?`

---

## 2. Files Created / Modified

### New Files

| File | Purpose | Type |
|------|---------|------|
| `Pipeline.java` | JPA entity representing a CI/CD pipeline | Entity |
| `PipelineProvider.java` | Enum for CI/CD providers (JENKINS, GITHUB_ACTIONS, etc.) | Enum |
| `PipelineRepository.java` | Spring Data JPA repository for Pipeline CRUD | Repository |
| `PipelineService.java` | Business logic for pipeline operations | Service |
| `PipelineController.java` | REST controller exposing pipeline endpoints | Controller |
| `PipelineNotFoundException.java` | Exception thrown when pipeline not found | Exception |
| `CreatePipelineRequest.java` | DTO for creating pipelines via API | DTO |
| `PipelineResponse.java` | DTO for returning pipeline data via API | DTO |

### Modified Files

| File | Changes |
|------|---------|
| `GlobalExceptionHandler.java` | Added handler for `PipelineNotFoundException` |

---

## 3. Pipeline Entity

**Fields:**
- `id` (UUID, Primary Key, auto-generated)
- `project` (Foreign Key to Project, required)
- `provider` (Enum: JENKINS, GITHUB_ACTIONS, GITLAB_CI, AZURE_PIPELINES, CIRCLECI, TRAVIS_CI)
- `jobName` (String, max 255 chars, required)
- `buildNumber` (Long, required, positive)
- `branch` (String, max 255 chars, required)
- `commitSha` (String, max 255 chars, optional)
- `createdAt` (LocalDateTime, auto-set, not updatable)

**Indexes:**
- `idx_pipeline_project_id` (on `project_id`) - for fast project filtering
- `idx_pipeline_job_name` (on `job_name`) - for job lookup

---

## 4. REST API Endpoints

### 1. Create Pipeline
```
POST /api/projects/{projectId}/pipelines
Content-Type: application/json

Request Body:
{
    "provider": "JENKINS",
    "jobName": "payment-api-pipeline",
    "buildNumber": 42,
    "branch": "main",
    "commitSha": "abc123"
}

Response (201 Created):
{
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "projectId": "1234567-89ab-cdef-0123-456789abcdef",
    "provider": "JENKINS",
    "jobName": "payment-api-pipeline",
    "buildNumber": 42,
    "branch": "main",
    "commitSha": "abc123",
    "createdAt": "2026-08-23T10:30:00"
}

Errors:
- 404 Not Found: If projectId doesn't exist → {"error_code":"NOT_FOUND","message":"Project not found: ..."}
- 400 Bad Request: If validation fails → {"error_code":"VALIDATION_ERROR","message":"Provider is required"}
```

### 2. Get Pipelines by Project
```
GET /api/projects/{projectId}/pipelines

Response (200 OK):
[
    {
        "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        "projectId": "1234567-89ab-cdef-0123-456789abcdef",
        "provider": "JENKINS",
        "jobName": "payment-api-pipeline",
        "buildNumber": 42,
        "branch": "main",
        "commitSha": "abc123",
        "createdAt": "2026-08-23T10:30:00"
    }
]

Errors:
- 404 Not Found: If projectId doesn't exist
```

### 3. Get Specific Pipeline
```
GET /api/pipelines/{pipelineId}

Response (200 OK):
{
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "projectId": "1234567-89ab-cdef-0123-456789abcdef",
    "provider": "JENKINS",
    "jobName": "payment-api-pipeline",
    "buildNumber": 42,
    "branch": "main",
    "commitSha": "abc123",
    "createdAt": "2026-08-23T10:30:00"
}

Errors:
- 404 Not Found: If pipelineId doesn't exist
```

---

## 5. Validation Rules

### CreatePipelineRequest Validation
- `provider`: Required, must be valid enum value
- `jobName`: Required, 1-255 characters
- `buildNumber`: Required, must be positive integer
- `branch`: Required, 1-255 characters
- `commitSha`: Optional, max 255 characters

**Error Example:**
```json
{
    "error_code": "VALIDATION_ERROR",
    "message": "Build number must be positive"
}
```

---

## 6. Multi-Project Isolation

### How It Works
Each pipeline has a `project_id` foreign key. When creating a pipeline:
1. Service validates that the project exists
2. If project doesn't exist → throw `ProjectNotFoundException` → HTTP 404
3. If project exists → create pipeline with explicit project reference
4. Pipeline can only be queried by project or by pipeline ID
5. Pipelines from different projects are completely isolated

### Example: Project Isolation
```
Project 1: "Payment API"
  └── Pipeline A: "payment-api-pipeline" (build #42)
  └── Pipeline B: "payment-api-tests" (build #43)

Project 2: "Job Portal"
  └── Pipeline C: "job-portal-pipeline" (build #1)

Query: GET /api/projects/{projectId1}/pipelines
Returns: [Pipeline A, Pipeline B] ← Only Project 1's pipelines

Query: GET /api/projects/{projectId2}/pipelines
Returns: [Pipeline C] ← Only Project 2's pipelines

Query: GET /api/pipelines/{pipelineAId}
Returns: Pipeline A ← Single pipeline by ID
```

---

## 7. Postman Test Scenarios

### Setup
- Base URL: `http://localhost:8080`
- Content-Type: `application/json`

### Test 1: Create Project 1 (Payment API)
```
POST /api/projects
{
    "name": "Payment API",
    "repositoryUrl": "https://github.com/company/payment-api"
}

Expected: 201 Created
Response includes: projectId1 (save this value)
```

### Test 2: Create Pipeline for Payment API
```
POST /api/projects/{projectId1}/pipelines
{
    "provider": "JENKINS",
    "jobName": "payment-api-pipeline",
    "buildNumber": 42,
    "branch": "main",
    "commitSha": "abc123"
}

Expected: 201 Created
Response includes: pipelineId1 (save this value)
```

### Test 3: Create Project 2 (Job Portal)
```
POST /api/projects
{
    "name": "Job Portal",
    "repositoryUrl": "https://github.com/company/job-portal"
}

Expected: 201 Created
Response includes: projectId2 (save this value)
```

### Test 4: Create Pipeline for Job Portal
```
POST /api/projects/{projectId2}/pipelines
{
    "provider": "GITHUB_ACTIONS",
    "jobName": "job-portal-pipeline",
    "buildNumber": 1,
    "branch": "develop",
    "commitSha": "def456"
}

Expected: 201 Created
Response includes: pipelineId2 (save this value)
```

### Test 5: List Pipelines for Payment API
```
GET /api/projects/{projectId1}/pipelines

Expected: 200 OK
Response: Array with 1 pipeline (payment-api-pipeline)
```

### Test 6: List Pipelines for Job Portal
```
GET /api/projects/{projectId2}/pipelines

Expected: 200 OK
Response: Array with 1 pipeline (job-portal-pipeline)
```

### Test 7: Get Specific Pipeline
```
GET /api/pipelines/{pipelineId1}

Expected: 200 OK
Response: Payment API pipeline details
```

### Test 8: Verify Project Isolation - Get Non-Existent Pipeline
```
GET /api/pipelines/{non-existent-uuid}

Expected: 404 Not Found
Response: {"error_code":"NOT_FOUND","message":"Pipeline not found: ..."}
```

### Test 9: Create Pipeline for Non-Existent Project
```
POST /api/projects/{non-existent-uuid}/pipelines
{
    "provider": "JENKINS",
    "jobName": "test-pipeline",
    "buildNumber": 1,
    "branch": "main",
    "commitSha": "aaa111"
}

Expected: 404 Not Found
Response: {"error_code":"NOT_FOUND","message":"Project not found: ..."}
```

### Test 10: Validation Test - Missing Required Field
```
POST /api/projects/{projectId1}/pipelines
{
    "provider": "JENKINS",
    "jobName": "test-pipeline"
    // Missing: buildNumber, branch
}

Expected: 400 Bad Request
Response: {"error_code":"VALIDATION_ERROR","message":"Build number is required"}
```

---

## 8. Database Schema

After Hibernate DDL-auto=update:

```sql
CREATE TABLE pipeline (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id uuid NOT NULL,
    provider varchar(50) NOT NULL,
    job_name varchar(255) NOT NULL,
    build_number bigint NOT NULL,
    branch varchar(255) NOT NULL,
    commit_sha varchar(255),
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_pipeline_project FOREIGN KEY (project_id) 
        REFERENCES project(id) ON DELETE CASCADE
);

CREATE INDEX idx_pipeline_project_id ON pipeline(project_id);
CREATE INDEX idx_pipeline_job_name ON pipeline(job_name);
```

**Verify in PostgreSQL:**
```sql
psql -U postgres -d secureops
\dt  -- List all tables
\d pipeline  -- Show pipeline table structure
SELECT * FROM pipeline;  -- View all pipelines
```

---

## 9. Build and Run

```bash
cd d:\SecureOps-AI\secureops

# Clean build
mvn clean compile

# Package
mvn clean package -DskipTests

# Run
java -jar target/secureops-0.0.1-SNAPSHOT.jar
```

---

## 10. Key Design Decisions

1. **Foreign Key Constraint:** `ON DELETE CASCADE` ensures data integrity; deleting a project automatically deletes its pipelines.

2. **Enum for Provider:** Type-safe provider values prevent invalid data; easier to validate and display in UI.

3. **Eager Loading:** `FetchType.EAGER` on Project relationship means the project is loaded with each pipeline fetch (necessary for response DTO conversion).

4. **Validation in Service:** Project existence check in `PipelineService.createPipeline()` prevents orphaned pipelines.

5. **DTO Conversion:** `PipelineResponse.fromEntity()` decouples API response from JPA entity structure.

6. **Indexes:** Fast queries on `project_id` and `job_name` for common filtering operations.

---

## 11. Next Phase (Phase 3)

Phase 3 will introduce Scan Management:
- Each pipeline can trigger multiple scans
- Scans will analyze application security
- Relationship: Pipeline (1) → Scan (N)

---

## 12. Phase Completion Checklist

- [x] PipelineProvider enum created
- [x] Pipeline entity with foreign key to Project
- [x] PipelineRepository with custom query
- [x] PipelineService with validation
- [x] PipelineController with 3 endpoints
- [x] CreatePipelineRequest DTO with validation
- [x] PipelineResponse DTO
- [x] PipelineNotFoundException exception
- [x] GlobalExceptionHandler updated
- [x] Multi-project isolation enforced
- [x] All 10 Postman tests verified
- [x] PostgreSQL foreign key verified
- [x] Phase 2 documentation complete
- [x] Application builds successfully
- [x] No Scan implementation (reserved for Phase 3)

---

**Phase 2 Complete ✅**
