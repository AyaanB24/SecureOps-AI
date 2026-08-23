# Phase 3: Scan Management

**Status:** ✅ Complete

**Objective:** Implement security scan management. Each Jenkins pipeline execution produces a SecureOps security scan that captures security findings at a point in time.

---

## 1. Architecture Overview

### Relationship Model
```
Project (1)
    ↓
    └─→ Pipeline (N)
        ↓
        └─→ Scan (N)
```

A project has many pipelines. Each pipeline can have multiple scans (one per environment execution).

### PostgreSQL Foreign Key Relationships
```
CREATE TABLE scan (
    id uuid PRIMARY KEY,
    project_id uuid NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    pipeline_id uuid NOT NULL REFERENCES pipeline(id) ON DELETE CASCADE,
    ...
);
```

**Key Points:**
- `project_id`: Direct foreign key to project (enables fast project-level queries)
- `pipeline_id`: Foreign key to pipeline (enables pipeline-level queries)
- `ON DELETE CASCADE`: If pipeline deleted → all scans deleted; if project deleted → all scans deleted
- **Composite Unique Index:** `UNIQUE(project_id, pipeline_id)` - One scan per pipeline-project combo
- **Critical Validation:** Pipeline must belong to specified project (prevents cross-project data leakage)

---

## 2. Files Created / Modified

### New Files

| File | Purpose | Type |
|------|---------|------|
| `Environment.java` | Enum for deployment environments | Enum |
| `ScanStatus.java` | Enum for scan execution status | Enum |
| `Scan.java` | JPA entity representing a security scan | Entity |
| `ScanRepository.java` | Spring Data JPA repository for Scan CRUD | Repository |
| `ScanService.java` | Business logic for scan operations | Service |
| `ScanController.java` | REST controller exposing scan endpoints | Controller |
| `ScanNotFoundException.java` | Exception thrown when scan not found | Exception |
| `CreateScanRequest.java` | DTO for creating scans via API | DTO |
| `ScanResponse.java` | DTO for returning scan data via API | DTO |

### Modified Files

| File | Changes |
|------|---------|
| `GlobalExceptionHandler.java` | Added handlers for `ScanNotFoundException` and `IllegalArgumentException` (for cross-project validation) |

---

## 3. Enums

### Environment
Represents deployment environments where scans execute.

```java
public enum Environment {
    DEVELOPMENT,
    STAGING,
    PRODUCTION
}
```

**Usage:** Scan can be from dev, staging, or production environment.

### ScanStatus
Represents scan execution lifecycle.

```java
public enum ScanStatus {
    CREATED,      // Scan created, waiting to process
    PROCESSING,   // Scan currently executing security analysis
    COMPLETED,    // Scan finished successfully
    FAILED        // Scan failed during execution
}
```

**Workflow:**
```
CREATED → PROCESSING → COMPLETED
                    ↘ FAILED
```

---

## 4. Scan Entity

**Fields:**
- `id` (UUID, Primary Key, auto-generated)
- `project` (Foreign Key to Project, required)
- `pipeline` (Foreign Key to Pipeline, required)
- `environment` (Enum: DEVELOPMENT, STAGING, PRODUCTION, required)
- `status` (Enum: CREATED, PROCESSING, COMPLETED, FAILED, default: CREATED)
- `startedAt` (LocalDateTime, auto-set to now, not updatable)
- `completedAt` (LocalDateTime, nullable - set when scan finishes)

**Indexes:**
- `idx_scan_project_id` (on `project_id`) - for fast project filtering
- `idx_scan_pipeline_id` (on `pipeline_id`) - for fast pipeline filtering
- `idx_scan_status` (on `status`) - for filtering by status
- `uk_scan_project_pipeline_env` (UNIQUE on `project_id`, `pipeline_id`, `environment`) - one scan per pipeline-environment per project

---

## 5. Critical Validation

### Three-Step Validation in ScanService.createScan()

**Step 1: Validate Project Exists**
```java
Project project = projectRepository.findById(projectId)
    .orElseThrow(() -> new ProjectNotFoundException(...));
```
- If project doesn't exist → HTTP 404

**Step 2: Validate Pipeline Exists**
```java
Pipeline pipeline = pipelineRepository.findById(request.getPipelineId())
    .orElseThrow(() -> new PipelineNotFoundException(...));
```
- If pipeline doesn't exist → HTTP 404

**Step 3: CRITICAL - Validate Pipeline Belongs to Project**
```java
if (!pipeline.getProject().getId().equals(projectId)) {
    throw new IllegalArgumentException(
        "Pipeline X does not belong to project Y"
    );
}
```
- **This is essential security validation**
- Prevents: Project A scans being attached to Project B pipelines
- Response: HTTP 400 (INVALID_REQUEST)

### Example Attack Prevented
```
Attacker tries: POST /api/projects/{ProjectA}/scans
              { "pipelineId": "{PipelineFromProjectB}", ... }

Result: 400 Bad Request
        "Pipeline X does not belong to project A"
        ← Attachment prevented ✅
```

---

## 6. REST API Endpoints

### 1. Create Scan
```
POST /api/projects/{projectId}/scans
Content-Type: application/json

Request Body:
{
    "pipelineId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "environment": "DEVELOPMENT"
}

Response (201 Created):
{
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "projectId": "1234567-89ab-cdef-0123-456789abcdef",
    "pipelineId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "environment": "DEVELOPMENT",
    "status": "CREATED",
    "startedAt": "2026-08-23T10:30:00",
    "completedAt": null
}

Errors:
- 404 Not Found: Project or Pipeline doesn't exist
- 400 Bad Request: Pipeline doesn't belong to project OR validation fails
- 409 Conflict: Scan already exists for this pipeline-project combo
```

### 2. Get Scans by Project
```
GET /api/projects/{projectId}/scans

Response (200 OK):
[
    {
        "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        "projectId": "1234567-89ab-cdef-0123-456789abcdef",
        "pipelineId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "environment": "DEVELOPMENT",
        "status": "CREATED",
        "startedAt": "2026-08-23T10:30:00",
        "completedAt": null
    },
    {
        "id": "7890abcd-ef12-3456-7890-abcdef123456",
        "projectId": "1234567-89ab-cdef-0123-456789abcdef",
        "pipelineId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "environment": "STAGING",
        "status": "COMPLETED",
        "startedAt": "2026-08-23T09:00:00",
        "completedAt": "2026-08-23T09:15:00"
    }
]

Errors:
- 404 Not Found: If projectId doesn't exist
```

### 3. Get Specific Scan
```
GET /api/scans/{scanId}

Response (200 OK):
{
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "projectId": "1234567-89ab-cdef-0123-456789abcdef",
    "pipelineId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "environment": "DEVELOPMENT",
    "status": "CREATED",
    "startedAt": "2026-08-23T10:30:00",
    "completedAt": null
}

Errors:
- 404 Not Found: If scanId doesn't exist
```

---

## 7. Validation Rules

### CreateScanRequest Validation
- `pipelineId`: Required, must be valid UUID
- `environment`: Required, must be valid enum (DEVELOPMENT, STAGING, PRODUCTION)

**Error Example:**
```json
{
    "error_code": "VALIDATION_ERROR",
    "message": "Pipeline ID is required"
}
```

---

## 8. Database Schema

After Hibernate DDL-auto=update:

```sql
CREATE TABLE scan (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id uuid NOT NULL,
    pipeline_id uuid NOT NULL,
    environment varchar(50) NOT NULL,
    status varchar(50) NOT NULL DEFAULT 'CREATED',
    started_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at timestamp,
    
    CONSTRAINT fk_scan_project FOREIGN KEY (project_id) 
        REFERENCES project(id) ON DELETE CASCADE,
    CONSTRAINT fk_scan_pipeline FOREIGN KEY (pipeline_id) 
        REFERENCES pipeline(id) ON DELETE CASCADE,
    CONSTRAINT uk_scan_project_pipeline_env UNIQUE (project_id, pipeline_id, environment)
);

CREATE INDEX idx_scan_project_id ON scan(project_id);
CREATE INDEX idx_scan_pipeline_id ON scan(pipeline_id);
CREATE INDEX idx_scan_status ON scan(status);
```

**Verify in PostgreSQL:**
```sql
psql -U postgres -d secureops

-- View scan table structure
\d scan

-- View all scans
SELECT * FROM scan;

-- View scans with project and pipeline names
SELECT s.id, p.name as project_name, pl.job_name, s.environment, s.status
FROM scan s
JOIN project p ON s.project_id = p.id
JOIN pipeline pl ON s.pipeline_id = pl.id;
```

---

## 9. Postman Testing Steps

### Setup
- Base URL: `http://localhost:8080`
- Content-Type: `application/json`

### Test Scenario: Complete Workflow

**Test 1: Create Project 1 (Payment API)**
```
POST /api/projects
{
    "name": "Payment API",
    "repositoryUrl": "https://github.com/company/payment-api"
}

Expected: 201 Created
Save: projectId1 = (from response)
```

**Test 2: Create Pipeline for Project 1**
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
Save: pipelineId1 = (from response)
```

**Test 3: Create Scan for Project 1 (DEVELOPMENT)**
```
POST /api/projects/{projectId1}/scans
{
    "pipelineId": "{pipelineId1}",
    "environment": "DEVELOPMENT"
}

Expected: 201 Created
Response includes:
{
    "id": "(scan UUID)",
    "projectId": "{projectId1}",
    "pipelineId": "{pipelineId1}",
    "environment": "DEVELOPMENT",
    "status": "CREATED",
    "startedAt": "2026-08-23T...",
    "completedAt": null
}
Save: scanId1 = (from response)
```

**Test 4: Create Scan for Project 1 (STAGING)**
```
POST /api/projects/{projectId1}/scans
{
    "pipelineId": "{pipelineId1}",
    "environment": "STAGING"
}

Expected: 201 Created
Response includes: environment: "STAGING"
Save: scanId2 = (from response)
```

**Test 5: List Scans for Project 1**
```
GET /api/projects/{projectId1}/scans

Expected: 200 OK
Response: Array with 2 scans (DEVELOPMENT, STAGING)
```

**Test 6: Get Specific Scan**
```
GET /api/scans/{scanId1}

Expected: 200 OK
Response: Single scan details
```

**Test 7: Create Project 2 (Job Portal)**
```
POST /api/projects
{
    "name": "Job Portal",
    "repositoryUrl": "https://github.com/company/job-portal"
}

Expected: 201 Created
Save: projectId2 = (from response)
```

**Test 8: Create Pipeline for Project 2**
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
Save: pipelineId2 = (from response)
```

**Test 9: Create Scan for Project 2**
```
POST /api/projects/{projectId2}/scans
{
    "pipelineId": "{pipelineId2}",
    "environment": "PRODUCTION"
}

Expected: 201 Created
Response includes: projectId: "{projectId2}", pipelineId: "{pipelineId2}"
Save: scanId3 = (from response)
```

**Test 10: CRITICAL - Cross-Project Attack Prevention**
```
POST /api/projects/{projectId1}/scans
{
    "pipelineId": "{pipelineId2}",
    "environment": "DEVELOPMENT"
}

Expected: 400 Bad Request (INVALID_REQUEST)
Response: {"error_code":"INVALID_REQUEST","message":"Pipeline ... does not belong to project ..."}

✅ Attack prevented! Project A cannot scan Project B's pipeline
```

**Test 11: Non-Existent Project**
```
POST /api/projects/{non-existent-uuid}/scans
{
    "pipelineId": "{pipelineId1}",
    "environment": "DEVELOPMENT"
}

Expected: 404 Not Found
Response: {"error_code":"NOT_FOUND","message":"Project not found: ..."}
```

**Test 12: Non-Existent Pipeline**
```
POST /api/projects/{projectId1}/scans
{
    "pipelineId": "{non-existent-uuid}",
    "environment": "DEVELOPMENT"
}

Expected: 404 Not Found
Response: {"error_code":"NOT_FOUND","message":"Pipeline not found: ..."}
```

**Test 13: Non-Existent Scan**
```
GET /api/scans/{non-existent-uuid}

Expected: 404 Not Found
Response: {"error_code":"NOT_FOUND","message":"Scan not found: ..."}
```

**Test 14: Validation Error - Missing Environment**
```
POST /api/projects/{projectId1}/scans
{
    "pipelineId": "{pipelineId1}"
    // Missing: environment
}

Expected: 400 Bad Request
Response: {"error_code":"VALIDATION_ERROR","message":"Environment is required"}
```

---

## 10. Key Design Decisions

1. **Direct Foreign Key to Project:** Scan has both project_id and pipeline_id (denormalized)
   - Enables fast project-level queries
   - Redundant but improves query performance

2. **Three-Step Validation:** Project → Pipeline → Pipeline Ownership
   - Security: Prevents cross-project data leakage
   - Clear error messages for debugging

3. **Composite Unique Index:** `UNIQUE(project_id, pipeline_id)`
   - Ensures one scan per pipeline-project combination
   - Allows multiple scans per pipeline (different projects)

4. **Enum for Status:** Type-safe status values
   - Prevents invalid status strings
   - Future phases can implement status transitions

5. **Eager Loading:** FetchType.EAGER on Project and Pipeline relationships
   - Necessary for response DTO conversion
   - Prevents N+1 queries

---

## 11. Multi-Level Isolation

### Project Level
```
Project A (Payment API)
  └── Pipeline A1
      └── Scan A1-DEV
      └── Scan A1-STAGE

Project B (Job Portal)
  └── Pipeline B1
      └── Scan B1-PROD
```

### Pipeline Level
```
Pipeline A1 can have:
  ├── Scan DEVELOPMENT (created from pipeline execution)
  ├── Scan STAGING (created from pipeline execution)
  └── Scan PRODUCTION (created from pipeline execution)
```

### Cross-Project Prevention
```
Pipeline A1 (from Project A)
  ├── Can be scanned with Project A ✅
  ├── Cannot be scanned with Project B ❌ (validated in service)
```

---

## 12. Next Phase (Phase 4)

Phase 4 will introduce Report Management:
- Reports aggregate findings from multiple scans
- Relationship: Scan (1) → Report (1)
- Initial findings storage and normalization

---

## 13. Phase Completion Checklist

- [x] Environment enum created (DEVELOPMENT, STAGING, PRODUCTION)
- [x] ScanStatus enum created (CREATED, PROCESSING, COMPLETED, FAILED)
- [x] Scan entity with dual foreign keys (project, pipeline)
- [x] ScanRepository with custom queries
- [x] ScanService with three-step validation
- [x] ScanController with 3 endpoints
- [x] CreateScanRequest DTO with validation
- [x] ScanResponse DTO
- [x] ScanNotFoundException exception
- [x] GlobalExceptionHandler updated
- [x] Cross-project security validation enforced
- [x] All 14 Postman tests verified
- [x] PostgreSQL foreign keys and indexes verified
- [x] Phase 3 documentation complete
- [x] Application builds successfully
- [x] No Report implementation (reserved for Phase 4)

---

**Phase 3 Complete ✅**
