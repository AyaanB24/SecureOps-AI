# Phase 1: Project Management

## Objective
Implement multi-project isolation as the foundation for SecureOps. Each project represents an independent application/service in an organization, allowing isolated security scans and findings.

## Architecture

### Entity Model
```
PROJECT (top-level)
├── id: UUID (primary key)
├── name: String (required, unique in real-world, but not enforced here)
├── repositoryUrl: String (optional)
└── createdAt: LocalDateTime

Future relationships:
└── Pipeline* (1:N)
    └── Scan* (1:N)
        ├── Report* (1:N)
        │   └── Finding* (1:N)
        └── SecurityDecision (1:1)
```

### Layered Architecture
```
ProjectController (REST layer)
      ↓
ProjectService (business logic)
      ↓
ProjectRepository (data access)
      ↓
PostgreSQL (project table)
```

## Files Created

### Entity
**`project/Project.java`**
- JPA entity with @Entity, @Table annotations
- UUID primary key with @GeneratedValue(strategy = GenerationType.UUID)
- Fields: id, name, repositoryUrl, createdAt
- Index on name for query optimization
- Constructor for new project creation

### Repository
**`project/ProjectRepository.java`**
- Spring Data JPA interface extending JpaRepository<Project, UUID>
- Automatic CRUD operations: save(), findById(), findAll(), delete()
- Marked with @Repository for component scanning

### Service
**`project/ProjectService.java`**
- @Service layer with business logic
- Uses @RequiredArgsConstructor for constructor injection
- Methods:
  - `createProject(CreateProjectRequest)` → ProjectResponse
  - `getAllProjects()` → List<ProjectResponse>
  - `getProjectById(UUID)` → ProjectResponse (throws ProjectNotFoundException)
- Converts entities to DTOs using ProjectResponse.fromEntity()

### Controller
**`project/ProjectController.java`**
- @RestController mapped to /api/projects
- Uses @RequiredArgsConstructor for ProjectService injection
- Endpoints:
  - `POST /api/projects` → 201 Created
  - `GET /api/projects` → 200 OK
  - `GET /api/projects/{projectId}` → 200 OK or 404
- All methods return ResponseEntity<ProjectResponse>

### DTOs
**`project/dto/CreateProjectRequest.java`**
- Request DTO with @NotBlank validation on name
- Fields: name (required), repositoryUrl (optional)
- Jakarta Validation annotations for request validation

**`project/dto/ProjectResponse.java`**
- Response DTO for JSON serialization
- Fields: id, name, repositoryUrl, createdAt
- Static factory method: `fromEntity(Project)` for clean conversion
- @JsonFormat for date formatting (ISO 8601)

### Exception Handling
**`project/ProjectNotFoundException.java`**
- Custom exception extending RuntimeException
- Thrown by ProjectService when project not found
- Caught by GlobalExceptionHandler and converted to HTTP 404

**`common/exception/GlobalExceptionHandler.java`**
- @RestControllerAdvice for centralized exception handling
- Handlers:
  - ProjectNotFoundException → 404 Not Found
  - MethodArgumentNotValidException → 400 Bad Request
  - Generic Exception → 500 Internal Server Error
- Returns ErrorResponse DTO

**`common/exception/ErrorResponse.java`**
- Standardized error response DTO
- Fields: code (error code), message (human-readable)
- Used by GlobalExceptionHandler for all error responses

## Database Schema

```sql
CREATE TABLE project (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    repository_url VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_project_name ON project(name);
```

### Key Decisions
- UUID for project ID: distributed-friendly, no sequential guessing attacks
- Name field not enforced unique: flexibility for later modifications
- repository_url optional: supports projects without Git repos
- created_at immutable: audit trail of project creation

## API Specification

### Create Project
```
POST /api/projects
Content-Type: application/json

Request Body:
{
  "name": "Payment API",
  "repositoryUrl": "https://github.com/company/payment-api"
}

Response (201 Created):
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Payment API",
  "repositoryUrl": "https://github.com/company/payment-api",
  "createdAt": "2026-08-23T01:38:45"
}
```

### List All Projects
```
GET /api/projects

Response (200 OK):
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Payment API",
    "repositoryUrl": "https://github.com/company/payment-api",
    "createdAt": "2026-08-23T01:38:45"
  },
  {
    "id": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
    "name": "Job Portal",
    "repositoryUrl": "https://github.com/company/job-portal",
    "createdAt": "2026-08-23T01:38:47"
  }
]
```

### Get Specific Project
```
GET /api/projects/{projectId}

Success (200 OK):
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Payment API",
  "repositoryUrl": "https://github.com/company/payment-api",
  "createdAt": "2026-08-23T01:38:45"
}

Not Found (404 Not Found):
{
  "code": "NOT_FOUND",
  "message": "Project not found: 00000000-0000-0000-0000-000000000000"
}
```

### Validation Errors
```
POST /api/projects
Content-Type: application/json

Invalid Request:
{
  "repositoryUrl": "https://github.com/company/test"
}

Response (400 Bad Request):
{
  "code": "VALIDATION_ERROR",
  "message": "Project name is required"
}
```

## Multi-Project Isolation

### Example: Two Independent Projects

**Project 1: Payment API**
```
ID: 550e8400-e29b-41d4-a716-446655440000
Name: Payment API
Repository: https://github.com/company/payment-api

Future Structure:
└── Pipeline A
    ├── Scan 101 (Trivy)
    │   └── Findings (only Project 1 can access)
    └── Scan 102 (Semgrep)
        └── Findings (only Project 1 can access)
```

**Project 2: Job Portal**
```
ID: 6ba7b810-9dad-11d1-80b4-00c04fd430c8
Name: Job Portal
Repository: https://github.com/company/job-portal

Future Structure:
└── Pipeline B
    ├── Scan 201 (Trivy)
    │   └── Findings (only Project 2 can access)
    └── Scan 202 (Semgrep)
        └── Findings (only Project 2 can access)
```

### How Isolation Works

**Phase 1 (Current):**
- Each project has unique UUID
- Projects are independent entities

**Phase 2+ (Future):**
- Every Pipeline has `project_id` FK
- Every Scan has `project_id` FK
- Every Finding has `project_id` FK
- All queries filter by project_id at repository level

Example queries:
```
// Get all scans from Payment API
SELECT * FROM scan WHERE project_id = '550e8400-e29b-41d4-a716-446655440000'

// Get findings from Payment API's scan 101
SELECT * FROM finding 
WHERE project_id = '550e8400-e29b-41d4-a716-446655440000' 
AND scan_id = 'scan-101'
```

**Result:** Findings from Project 1 are NEVER visible when querying Project 2.

## Testing

### Postman Collection
1. Create Project 1: Payment API
2. Create Project 2: Job Portal
3. List all projects (verify both exist)
4. Get Project 1 by ID (verify isolation)
5. Get Project 2 by ID (verify isolation)
6. Try to get non-existent project (verify 404)
7. Try to create project without name (verify validation)

### Expected Behavior
- ✅ Two projects created with unique IDs
- ✅ Both projects retrievable independently
- ✅ No cross-project data leakage
- ✅ Validation enforced on request fields
- ✅ Not found errors return 404
- ✅ Validation errors return 400

## Status
✅ Phase 1 Complete
- Application compiles and runs
- All endpoints tested and working
- Multi-project isolation foundation in place
- Ready for Phase 2: Pipeline Management

## Next Phase
Phase 2 will introduce:
- Pipeline entity with project_id foreign key
- PipelineRepository and PipelineService
- Pipeline CRUD endpoints
- Enforcement of project_id in URLs
