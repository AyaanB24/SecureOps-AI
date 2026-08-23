# Duplicate Prevention Strategy

**Status:** ✅ Implemented

**Purpose:** Prevent duplicate projects and pipelines while maintaining backward compatibility with existing functionality.

---

## 0. Files Changed Summary

| File | From | To |
|------|------|-----|
| `Project.java` | No unique constraint | Added `@UniqueConstraint(columnNames = "name")` |
| `Pipeline.java` | No unique constraint | Added `@UniqueConstraint(columnNames = {"project_id", "job_name", "build_number"})` |
| `ProjectRepository.java` | No findByName method | Added `Optional<Project> findByName(String name)` |
| `PipelineRepository.java` | No composite query | Added `Optional<Pipeline> findByProjectIdAndJobNameAndBuildNumber(UUID, String, Long)` |
| `ProjectService.java` | Direct save on create | Added pre-query duplicate check before save |
| `PipelineService.java` | Direct save on create | Added pre-query duplicate check before save |
| `GlobalExceptionHandler.java` | Handled 2 exceptions | Added handlers for DuplicateProjectException & DuplicatePipelineException |
| **NEW** | N/A | `DuplicateProjectException.java` created |
| **NEW** | N/A | `DuplicatePipelineException.java` created |

---

## 1. Problem Statement

The original implementation allowed creating duplicate projects with the same name and duplicate pipelines with the same job name and build number within a project. This caused data integrity issues and poor user experience.

**Example Issue:**
```
POST /api/projects { "name": "Payment API" } → Success (ID: 123)
POST /api/projects { "name": "Payment API" } → Success (ID: 456) ← Duplicate!
```

---

## 2. Solution Overview

Implemented a **two-layer duplicate prevention strategy**:
1. **Database Layer:** Unique constraints in JPA annotations
2. **Service Layer:** Pre-query duplicate checks with custom exceptions
3. **API Layer:** HTTP 409 Conflict responses for duplicates

This approach ensures:
- No duplicates reach the database
- Clear error messages to users
- No disruption to existing code paths
- Transactional consistency

---

## 3. Implementation Details

### 3.1 Project Duplicate Prevention

#### Database Layer (Entity)
```java
@Entity
@Table(name = "project", uniqueConstraints = {
    @UniqueConstraint(name = "uk_project_name", columnNames = "name")
})
public class Project {
    // ... fields
}
```

**What it does:**
- Creates a unique index on the `name` column
- PostgreSQL enforces: `UNIQUE(name)`
- Prevents duplicate names at database level (last-resort protection)

#### Repository Layer
```java
@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    Optional<Project> findByName(String name);
}
```

**What it does:**
- Provides query method to check if project name exists
- Spring Data JPA auto-generates SQL: `SELECT * FROM project WHERE name = ?`

#### Service Layer (Business Logic)
```java
@Service
public class ProjectService {
    public ProjectResponse createProject(CreateProjectRequest request) {
        // Check BEFORE saving
        if (projectRepository.findByName(request.getName()).isPresent()) {
            throw new DuplicateProjectException(request.getName());
        }
        
        Project project = new Project(request.getName(), request.getRepositoryUrl());
        Project saved = projectRepository.save(project);
        return ProjectResponse.fromEntity(saved);
    }
}
```

**What it does:**
- Queries database: "Does this project name already exist?"
- If yes → throw `DuplicateProjectException` immediately
- If no → proceed with creation
- **Important:** This check happens BEFORE `save()`, preventing unnecessary database writes

#### Exception Layer
```java
public class DuplicateProjectException extends RuntimeException {
    public DuplicateProjectException(String projectName) {
        super("Project with name '" + projectName + "' already exists");
    }
}
```

#### Exception Handler (Controller Advice)
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(DuplicateProjectException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateProject(DuplicateProjectException ex) {
        ErrorResponse error = new ErrorResponse("DUPLICATE", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);  // 409
    }
}
```

**What it does:**
- Catches `DuplicateProjectException` thrown by service
- Converts to HTTP 409 Conflict response
- Returns structured error: `{"error_code":"DUPLICATE","message":"..."}`

---

### 3.2 Pipeline Duplicate Prevention

#### Database Layer (Entity)
```java
@Entity
@Table(name = "pipeline", uniqueConstraints = {
    @UniqueConstraint(
        name = "uk_pipeline_project_job_build",
        columnNames = {"project_id", "job_name", "build_number"}
    )
})
public class Pipeline {
    // ... fields
}
```

**What it does:**
- Creates a composite unique index: `UNIQUE(project_id, job_name, build_number)`
- Ensures each job's build number is unique **per project**
- Multiple projects can have the same job name and build number (isolation preserved)

**Example:**
```
Project 1 (Payment API):
  └── job_name: "payment-api-pipeline", build_number: 42 ✅ Allowed

Project 2 (Job Portal):
  └── job_name: "payment-api-pipeline", build_number: 42 ✅ Allowed (different project)

Project 1 (Payment API):
  └── job_name: "payment-api-pipeline", build_number: 42 ❌ Duplicate (same project, same job+build)
```

#### Repository Layer
```java
@Repository
public interface PipelineRepository extends JpaRepository<Pipeline, UUID> {
    List<Pipeline> findByProjectId(UUID projectId);
    
    // NEW: Check for duplicates
    Optional<Pipeline> findByProjectIdAndJobNameAndBuildNumber(
        UUID projectId, String jobName, Long buildNumber
    );
}
```

**What it does:**
- Provides query to check if pipeline exists for a specific project+job+build combination
- Spring Data auto-generates: `SELECT * FROM pipeline WHERE project_id = ? AND job_name = ? AND build_number = ?`

#### Service Layer
```java
@Service
public class PipelineService {
    public PipelineResponse createPipeline(UUID projectId, CreatePipelineRequest request) {
        // Validate project exists
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ProjectNotFoundException(projectId.toString()));
        
        // Check BEFORE saving (new code)
        if (pipelineRepository.findByProjectIdAndJobNameAndBuildNumber(
            projectId, request.getJobName(), request.getBuildNumber()
        ).isPresent()) {
            throw new DuplicatePipelineException(request.getJobName(), request.getBuildNumber());
        }
        
        // Proceed with creation
        Pipeline pipeline = new Pipeline(...);
        Pipeline saved = pipelineRepository.save(pipeline);
        return PipelineResponse.fromEntity(saved);
    }
}
```

---

## 4. HTTP Response Examples

### 4.1 Project Duplicate Response
```http
POST /api/projects
Content-Type: application/json

{
    "name": "Payment API",
    "repositoryUrl": "https://github.com/company/payment-api"
}

HTTP/1.1 409 Conflict
Content-Type: application/json

{
    "error_code": "DUPLICATE",
    "message": "Project with name 'Payment API' already exists"
}
```

### 4.2 Pipeline Duplicate Response
```http
POST /api/projects/{projectId}/pipelines
Content-Type: application/json

{
    "provider": "JENKINS",
    "jobName": "payment-api-pipeline",
    "buildNumber": 42,
    "branch": "main",
    "commitSha": "abc123"
}

HTTP/1.1 409 Conflict
Content-Type: application/json

{
    "error_code": "DUPLICATE",
    "message": "Pipeline with job name 'payment-api-pipeline' and build number 42 already exists for this project"
}
```

---

## 5. Why This Design?

### 5.1 Two-Layer Protection (Database + Service)
- **Service Layer:** Fast failure before database write; better UX with clear messages
- **Database Layer:** Last-resort protection; prevents concurrent race conditions
- Both layers prevent duplicates; no single point of failure

### 5.2 Pre-Query Before Save
```java
// ✅ GOOD: Check before save
if (repository.findByName(name).isPresent()) {
    throw new DuplicateException();
}
repository.save(entity);

// ❌ BAD: Let database reject
try {
    repository.save(entity);
} catch (SQLIntegrityConstraintViolationException e) {
    throw new DuplicateException();
}
```

**Benefits:**
- Clear, immediate error messages
- No wasted database write attempt
- Easier to test and debug
- Better exception handling before database interaction

### 5.3 Unique Constraints Design

**Project:**
- `UNIQUE(name)` - Simple, global constraint
- Project names are globally unique across entire system

**Pipeline:**
- `UNIQUE(project_id, job_name, build_number)` - Composite constraint
- Allows same job+build in different projects (isolation)
- Prevents same job+build within a project

---

## 6. Testing Duplicate Prevention

### 6.1 Project Duplication Test
```bash
# Test 1: Create first project (should succeed)
curl -X POST http://localhost:8080/api/projects \
  -H "Content-Type: application/json" \
  -d '{"name": "Payment API", "repositoryUrl": "..."}'

Expected: 201 Created

# Test 2: Create duplicate project (should fail)
curl -X POST http://localhost:8080/api/projects \
  -H "Content-Type: application/json" \
  -d '{"name": "Payment API", "repositoryUrl": "..."}'

Expected: 409 Conflict
Response: {"error_code":"DUPLICATE","message":"Project with name 'Payment API' already exists"}
```

### 6.2 Pipeline Duplication Test
```bash
# Test 1: Create first pipeline (should succeed)
curl -X POST http://localhost:8080/api/projects/{projectId}/pipelines \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "JENKINS",
    "jobName": "payment-api-pipeline",
    "buildNumber": 42,
    "branch": "main",
    "commitSha": "abc123"
  }'

Expected: 201 Created

# Test 2: Create duplicate pipeline (should fail)
curl -X POST http://localhost:8080/api/projects/{projectId}/pipelines \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "JENKINS",
    "jobName": "payment-api-pipeline",
    "buildNumber": 42,
    "branch": "main",
    "commitSha": "abc123"
  }'

Expected: 409 Conflict
Response: {"error_code":"DUPLICATE","message":"Pipeline with job name 'payment-api-pipeline' and build number 42 already exists for this project"}
```

### 6.3 Multi-Project Isolation (Allowed)
```bash
# Create Project 1
POST /api/projects { "name": "Payment API" } → 201

# Create Project 2
POST /api/projects { "name": "Job Portal" } → 201

# Create Pipeline in Project 1
POST /api/projects/{project1Id}/pipelines {
  "jobName": "payment-api-pipeline",
  "buildNumber": 42
} → 201

# Create Pipeline in Project 2 with SAME job name and build number
POST /api/projects/{project2Id}/pipelines {
  "jobName": "payment-api-pipeline",
  "buildNumber": 42
} → 201 ✅ (Different project, so allowed!)
```

---

## 7. Files Modified/Created

### New Files
- `DuplicateProjectException.java` - Exception for project duplicates
- `DuplicatePipelineException.java` - Exception for pipeline duplicates
- `docs/DUPLICATE_PREVENTION.md` - This documentation

### Modified Files
| File | Change |
|------|--------|
| `Project.java` | Added `@UniqueConstraint(columnNames = "name")` |
| `Pipeline.java` | Added `@UniqueConstraint(columnNames = {"project_id", "job_name", "build_number"})` |
| `ProjectRepository.java` | Added `findByName(String name)` query method |
| `PipelineRepository.java` | Added `findByProjectIdAndJobNameAndBuildNumber()` query method |
| `ProjectService.java` | Added duplicate check before save in `createProject()` |
| `PipelineService.java` | Added duplicate check before save in `createPipeline()` |
| `GlobalExceptionHandler.java` | Added handlers for `DuplicateProjectException` and `DuplicatePipelineException` |

---

## 8. Backward Compatibility

✅ **Zero Breaking Changes:**
- Existing project/pipeline listing endpoints unchanged
- Existing get-by-ID endpoints unchanged
- Existing DTO validation unchanged
- Only addition: duplicate checks during creation
- All other functionality unaffected

**Impact Summary:**
- ✅ `GET /api/projects` - No change
- ✅ `GET /api/projects/{projectId}` - No change
- ✅ `GET /api/projects/{projectId}/pipelines` - No change
- ✅ `GET /api/pipelines/{pipelineId}` - No change
- ✅ `POST /api/projects` - Now rejects duplicates (expected behavior)
- ✅ `POST /api/projects/{projectId}/pipelines` - Now rejects duplicates (expected behavior)

---

## 9. PostgreSQL Verification

After applying migrations/DDL:

```sql
-- View project table constraints
SELECT constraint_name, constraint_type 
FROM information_schema.table_constraints 
WHERE table_name = 'project';

-- Output:
-- constraint_name       | constraint_type
-- ----------------------+-----------------
-- uk_project_name       | UNIQUE
-- pk_project            | PRIMARY KEY

-- View pipeline table constraints
SELECT constraint_name, constraint_type 
FROM information_schema.table_constraints 
WHERE table_name = 'pipeline';

-- Output:
-- constraint_name                | constraint_type
-- --------------------------------+-----------------
-- uk_pipeline_project_job_build   | UNIQUE
-- pk_pipeline                    | PRIMARY KEY
-- fk_pipeline_project            | FOREIGN KEY

-- Verify unique constraint on pipeline
\d pipeline
-- Indexes:
--     "pk_pipeline" PRIMARY KEY, btree (id)
--     "uk_pipeline_project_job_build" UNIQUE, btree (project_id, job_name, build_number)
--     "idx_pipeline_project_id" btree (project_id)
```

---

## 10. Performance Considerations

### Query Performance
- **Project duplicate check:** `findByName()` uses unique index → O(log n) → Very fast
- **Pipeline duplicate check:** `findByProjectIdAndJobNameAndBuildNumber()` uses composite index → O(log n) → Very fast

### Impact on Create Operations
```
Original:   save() → 1 DB write
Updated:    findBy*() → 1 DB read + save() → 2 DB operations
           (But same transaction, minimal overhead)
```

---

## 11. Future Enhancements

1. **Case-Insensitive Project Names:**
   ```java
   @UniqueConstraint(name = "uk_project_name_lower", 
       columnNames = "LOWER(name)")
   ```

2. **Soft Deletes:** Allow "deleted" projects to have same names

3. **Versioning:** Allow same pipeline name but different build numbers

4. **Scheduling:** Automatically increment build numbers

---

## 12. Summary

| Aspect | Before | After |
|--------|--------|-------|
| Duplicate Projects Allowed | ✗ Yes | ✓ No |
| Duplicate Pipelines Allowed | ✗ Yes | ✓ No |
| Multi-Project Isolation | ✓ Yes | ✓ Yes |
| HTTP Error Code | N/A | 409 Conflict |
| Service Layer Check | ✗ No | ✓ Yes |
| Database Constraint | ✗ No | ✓ Yes |
| Error Message Quality | N/A | ✓ Clear & Specific |
| Backward Compatible | N/A | ✓ Yes |

---

**Duplicate Prevention Implemented ✅**

All duplicates now rejected cleanly with HTTP 409 Conflict responses.
