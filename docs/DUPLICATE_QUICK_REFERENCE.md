# Duplicate Prevention - Quick Reference

## What Changed?

✅ **Projects:** Cannot create two projects with the same name
✅ **Pipelines:** Cannot create two pipelines with the same job name + build number in the same project

## How to Test

### Test 1: Duplicate Project (Should Fail)
```bash
# First request - Success
POST /api/projects
{
    "name": "Payment API",
    "repositoryUrl": "https://github.com/company/payment-api"
}
→ 201 Created

# Second request - FAILS (duplicate)
POST /api/projects
{
    "name": "Payment API",
    "repositoryUrl": "https://github.com/company/payment-api"
}
→ 409 Conflict
{
    "error_code": "DUPLICATE",
    "message": "Project with name 'Payment API' already exists"
}
```

### Test 2: Duplicate Pipeline in Same Project (Should Fail)
```bash
# First request - Success
POST /api/projects/{projectId}/pipelines
{
    "provider": "JENKINS",
    "jobName": "payment-api-pipeline",
    "buildNumber": 42,
    "branch": "main",
    "commitSha": "abc123"
}
→ 201 Created

# Second request - FAILS (duplicate job + build in same project)
POST /api/projects/{projectId}/pipelines
{
    "provider": "JENKINS",
    "jobName": "payment-api-pipeline",
    "buildNumber": 42,
    "branch": "develop",
    "commitSha": "def456"
}
→ 409 Conflict
{
    "error_code": "DUPLICATE",
    "message": "Pipeline with job name 'payment-api-pipeline' and build number 42 already exists for this project"
}
```

### Test 3: Same Pipeline in Different Projects (Should Succeed)
```bash
# Project 1: Create project
POST /api/projects { "name": "Payment API", ... } → 201 (id: proj-123)

# Project 2: Create different project
POST /api/projects { "name": "Job Portal", ... } → 201 (id: proj-456)

# Project 1: Create pipeline
POST /api/projects/proj-123/pipelines
{
    "jobName": "payment-api-pipeline",
    "buildNumber": 42,
    "branch": "main"
}
→ 201 Created

# Project 2: Create SAME pipeline (but in different project)
POST /api/projects/proj-456/pipelines
{
    "jobName": "payment-api-pipeline",
    "buildNumber": 42,
    "branch": "main"
}
→ 201 Created ✅ (ALLOWED - different project)
```

## Files Changed

### New Exception Classes
- `ProjectService.java` → Added duplicate check
- `DuplicateProjectException.java` → New exception
- `PipelineService.java` → Added duplicate check
- `DuplicatePipelineException.java` → New exception
- `GlobalExceptionHandler.java` → Added 409 handlers

### Database Constraints
- `Project.java` → Added `@UniqueConstraint(columnNames = "name")`
- `Pipeline.java` → Added `@UniqueConstraint(columnNames = {"project_id", "job_name", "build_number"})`

### Repository Methods
- `ProjectRepository.findByName(String name)` → Check project exists
- `PipelineRepository.findByProjectIdAndJobNameAndBuildNumber(...)` → Check pipeline exists

## HTTP Status Codes

| Scenario | Status | Error Code |
|----------|--------|-----------|
| Duplicate project name | 409 | DUPLICATE |
| Duplicate pipeline (same project, job, build) | 409 | DUPLICATE |
| Project not found | 404 | NOT_FOUND |
| Pipeline not found | 404 | NOT_FOUND |
| Validation error | 400 | VALIDATION_ERROR |

## Zero Breaking Changes

✅ All existing endpoints work the same
✅ Only rejection of duplicates added
✅ All existing data unaffected
✅ Full backward compatibility maintained

---

See `DUPLICATE_PREVENTION.md` for detailed implementation explanation.
