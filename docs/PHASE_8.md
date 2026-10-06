# Phase 8: Configurable Security Policy System

**Status:** ✅ Complete

**Objective:** Enable environment-specific, project-scoped security policies that determine scan acceptability.

---

## 1. Overview

Phase 8 introduces the **Security Policy System**, which allows projects to define acceptance thresholds for security scans based on environment (DEVELOPMENT, STAGING, PRODUCTION).

### Key Concept: Project-Level Isolation

Different projects can have **different policies** for the **same environment**:

```
Payment API (Production):
  maxCritical: 0  (zero-tolerance)
  maxHigh: 3
  maxRiskScore: 70

Job Portal (Production):
  maxCritical: 1  (allows 1 critical)
  maxHigh: 5
  maxRiskScore: 100
```

This enables each project to set its own security standards.

---

## 2. Architecture

### Components

#### Policy Entity
Represents a security policy for a project + environment combination.

**Fields:**
- `id`: UUID (primary key, auto-generated)
- `project`: Foreign key to Project
- `environment`: Enum (DEVELOPMENT, STAGING, PRODUCTION)
- `maxCritical`: Maximum allowed CRITICAL findings
- `maxHigh`: Maximum allowed HIGH findings
- `maxRiskScore`: Maximum allowed risk score (Phase 7)
- `createdAt`: Timestamp (auto-set)
- `updatedAt`: Timestamp (auto-updated)

**Unique Constraint:** `(project_id, environment)` - one policy per environment per project

#### PolicyEnvironment Enum
```java
DEVELOPMENT,
STAGING,
PRODUCTION
```

#### PolicyRepository
Spring Data JPA repository with custom queries:
- `findByProjectId(projectId)` - All policies for a project
- `findByProjectIdAndEnvironment(projectId, environment)` - Specific policy

#### PolicyService
Business logic for policy CRUD:
- `createPolicy()` - Create new policy with validation
- `getPoliciesByProject()` - List all policies for project
- `getPolicyByProjectAndEnvironment()` - Get specific policy
- `updatePolicy()` - Update thresholds

**Validation:**
- Project must exist
- All thresholds must be non-negative
- No hardcoded values

#### PolicyController
REST endpoints for policy management (CRUD operations)

#### PolicyEngine
Evaluates whether a scan meets its project's policy:
- Retrieves scan's project and environment
- Loads policy from database (not hardcoded)
- Counts findings by severity
- Calculates risk score (Phase 7)
- Compares against policy thresholds
- Returns evaluation result

#### PolicyEvaluation DTO
Response containing detailed evaluation breakdown.

---

## 3. Policy Data Model

### Database Schema

```sql
CREATE TABLE policy (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id uuid NOT NULL,
    environment varchar(50) NOT NULL,
    max_critical integer NOT NULL,
    max_high integer NOT NULL,
    max_risk_score integer NOT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_policy_project FOREIGN KEY (project_id) 
        REFERENCES project(id) ON DELETE CASCADE,
    CONSTRAINT uk_policy_project_environment UNIQUE (project_id, environment)
);

CREATE INDEX idx_policy_project_id ON policy(project_id);
CREATE INDEX idx_policy_environment ON policy(environment);
```

---

## 4. API Endpoints

### 4.1 Create Policy

```
POST /api/projects/{projectId}/policies
Content-Type: application/json
```

**Request Body:**
```json
{
  "environment": "PRODUCTION",
  "maxCritical": 0,
  "maxHigh": 3,
  "maxRiskScore": 70
}
```

**Response (201 Created):**
```json
{
  "id": "policy-uuid",
  "projectId": "project-uuid",
  "environment": "PRODUCTION",
  "maxCritical": 0,
  "maxHigh": 3,
  "maxRiskScore": 70,
  "createdAt": "2026-10-06T22:30:00",
  "updatedAt": "2026-10-06T22:30:00"
}
```

**Errors:**
- 400 Bad Request: Invalid/missing fields, negative thresholds
- 404 Not Found: Project doesn't exist
- 409 Conflict: Policy already exists for environment

---

### 4.2 Get All Policies for Project

```
GET /api/projects/{projectId}/policies
```

**Response (200 OK):**
```json
[
  {
    "id": "policy-uuid-1",
    "projectId": "project-uuid",
    "environment": "DEVELOPMENT",
    "maxCritical": 10,
    "maxHigh": 20,
    "maxRiskScore": 300,
    "createdAt": "...",
    "updatedAt": "..."
  },
  {
    "id": "policy-uuid-2",
    "projectId": "project-uuid",
    "environment": "PRODUCTION",
    "maxCritical": 0,
    "maxHigh": 3,
    "maxRiskScore": 70,
    "createdAt": "...",
    "updatedAt": "..."
  }
]
```

**Errors:**
- 404 Not Found: Project doesn't exist

---

### 4.3 Get Policy by Environment

```
GET /api/projects/{projectId}/policies/{environment}
```

**Parameters:**
- `projectId` (path): UUID
- `environment` (path): DEVELOPMENT, STAGING, or PRODUCTION

**Response (200 OK):**
```json
{
  "id": "policy-uuid",
  "projectId": "project-uuid",
  "environment": "PRODUCTION",
  "maxCritical": 0,
  "maxHigh": 3,
  "maxRiskScore": 70,
  "createdAt": "2026-10-06T22:30:00",
  "updatedAt": "2026-10-06T22:30:00"
}
```

**Errors:**
- 400 Bad Request: Invalid environment value
- 404 Not Found: Project or policy doesn't exist

---

### 4.4 Update Policy

```
PUT /api/projects/{projectId}/policies/{environment}
Content-Type: application/json
```

**Request Body:**
```json
{
  "environment": "PRODUCTION",
  "maxCritical": 0,
  "maxHigh": 5,
  "maxRiskScore": 80
}
```

**Response (200 OK):**
```json
{
  "id": "policy-uuid",
  "projectId": "project-uuid",
  "environment": "PRODUCTION",
  "maxCritical": 0,
  "maxHigh": 5,
  "maxRiskScore": 80,
  "createdAt": "2026-10-06T22:30:00",
  "updatedAt": "2026-10-06T23:00:00"
}
```

**Errors:**
- 400 Bad Request: Invalid/negative values
- 404 Not Found: Policy doesn't exist

---

## 5. Project-Level Isolation Example

### Scenario

Two projects, two different production policies:

**Project 1: Payment API**
```
POST /api/projects/payment-api-id/policies
{
  "environment": "PRODUCTION",
  "maxCritical": 0,      ← ZERO tolerance
  "maxHigh": 3,
  "maxRiskScore": 70
}
```

**Project 2: Job Portal**
```
POST /api/projects/job-portal-id/policies
{
  "environment": "PRODUCTION",
  "maxCritical": 1,      ← Allows 1 critical
  "maxHigh": 5,
  "maxRiskScore": 100
}
```

### Same Environment, Different Policies

Both projects have PRODUCTION environment, but different thresholds:

| Project | Env | Max Critical | Max High | Max Risk Score |
|---------|-----|--------------|----------|----------------|
| Payment API | PROD | 0 | 3 | 70 |
| Job Portal | PROD | 1 | 5 | 100 |

### Database Representation

```sql
-- Payment API production policy
INSERT INTO policy (project_id, environment, max_critical, max_high, max_risk_score)
VALUES ('payment-api-uuid', 'PRODUCTION', 0, 3, 70);

-- Job Portal production policy
INSERT INTO policy (project_id, environment, max_critical, max_high, max_risk_score)
VALUES ('job-portal-uuid', 'PRODUCTION', 1, 5, 100);
```

**Unique Constraint Ensures:**
- Payment API can't have two PRODUCTION policies
- Job Portal can't have two PRODUCTION policies
- Payment API and Job Portal can have different PRODUCTION policies ✓

---

## 6. PolicyEngine Evaluation

### Evaluation Logic

A scan is **ACCEPTED** if ALL conditions are met:

```
CRITICAL findings ≤ policy.maxCritical AND
HIGH findings ≤ policy.maxHigh AND
Risk score ≤ policy.maxRiskScore
```

Otherwise, scan is **REJECTED**.

### Evaluation Flow

```
1. Request: Evaluate scan {scanId}
2. Retrieve scan (includes environment and project)
3. Load policy from PostgreSQL (not hardcoded)
4. Count CRITICAL and HIGH findings
5. Calculate risk score (Phase 7)
6. Compare against policy thresholds
7. Return evaluation result
```

### Example Evaluation

**Scan Results:**
- 2 CRITICAL findings
- 5 HIGH findings
- Risk score: 85

**Policy (Payment API - PRODUCTION):**
- maxCritical: 0
- maxHigh: 3
- maxRiskScore: 70

**Evaluation:**
```
CRITICAL: 2 > 0 → FAIL ✗
HIGH: 5 > 3 → FAIL ✗
RiskScore: 85 > 70 → FAIL ✗

Result: REJECTED
```

---

## 7. Validation Rules

### Field Validation

| Field | Validation |
|-------|-----------|
| environment | Must be DEVELOPMENT, STAGING, or PRODUCTION |
| maxCritical | Must be ≥ 0 |
| maxHigh | Must be ≥ 0 |
| maxRiskScore | Must be ≥ 0 |

### Business Logic Validation

- Project must exist before creating policy
- Policy must exist before evaluation
- One policy per (project, environment) combination
- All thresholds must be non-negative (caught by @Min annotation)

---

## 8. Testing Workflow

### Prerequisites
- Phase 1-7 complete
- Projects and scans created
- Findings exist in scan

### Test Steps

**Step 1: Create Policy**
```
POST http://localhost:8080/api/projects/{projectId}/policies
{
  "environment": "PRODUCTION",
  "maxCritical": 0,
  "maxHigh": 3,
  "maxRiskScore": 70
}
```

**Step 2: List All Policies**
```
GET http://localhost:8080/api/projects/{projectId}/policies
```

**Step 3: Get Specific Policy**
```
GET http://localhost:8080/api/projects/{projectId}/policies/PRODUCTION
```

**Step 4: Update Policy**
```
PUT http://localhost:8080/api/projects/{projectId}/policies/PRODUCTION
{
  "environment": "PRODUCTION",
  "maxCritical": 0,
  "maxHigh": 5,
  "maxRiskScore": 80
}
```

---

## 9. Postman Examples

### Example 1: Create Development Policy (Lenient)
```
POST http://localhost:8080/api/projects/{projectId}/policies
{
  "environment": "DEVELOPMENT",
  "maxCritical": 10,
  "maxHigh": 20,
  "maxRiskScore": 300
}
```

### Example 2: Create Production Policy (Strict)
```
POST http://localhost:8080/api/projects/{projectId}/policies
{
  "environment": "PRODUCTION",
  "maxCritical": 0,
  "maxHigh": 3,
  "maxRiskScore": 70
}
```

### Example 3: Create Staging Policy (Medium)
```
POST http://localhost:8080/api/projects/{projectId}/policies
{
  "environment": "STAGING",
  "maxCritical": 1,
  "maxHigh": 5,
  "maxRiskScore": 100
}
```

---

## 10. Implementation Details

### Files Created
1. **Policy.java** - JPA entity
2. **PolicyEnvironment.java** - Enum
3. **PolicyRepository.java** - Data access layer
4. **CreatePolicyRequest.java** - Request DTO
5. **PolicyResponse.java** - Response DTO
6. **PolicyService.java** - Business logic
7. **PolicyController.java** - REST endpoints
8. **PolicyEngine.java** - Evaluation engine
9. **PolicyEvaluation.java** - Evaluation result DTO
10. **PolicyNotFoundException.java** - Exception

### Files Modified
- **GlobalExceptionHandler.java** - Added PolicyNotFoundException handler

### Database Changes
- New `policy` table with unique constraint `(project_id, environment)`
- Foreign key: `project_id` → `project.id` ON DELETE CASCADE
- Indexes: `project_id`, `environment`

---

## 11. Key Design Decisions

### 1. Database-Driven Policies
- Policies are **NOT hardcoded**
- Each policy is **read from PostgreSQL**
- Easy to change thresholds without code deployment

### 2. Project-Level Isolation
- Each project has its own set of policies
- Policies enforce multi-tenancy at the policy level
- Different projects can have different standards

### 3. Environment-Specific Policies
- Same project can have different policies per environment
- PRODUCTION typically stricter than DEVELOPMENT
- One policy per (project, environment) pair

### 4. Validation at Service Layer
- All validation in PolicyService, not just annotations
- Clear error messages for missing projects
- Prevents negative thresholds

### 5. No SecurityDecision Yet
- PolicyEngine only evaluates acceptance
- Does not implement SecurityDecision (Phase 9)
- Keeps Phase 8 focused and testable

---

## 12. Future Enhancement (Phase 9)

Not implemented in Phase 8:
- SecurityDecision entity (stores evaluation result linked to scan)
- Automatic decision enforcement (blocking/allowing deployments)
- Policy versioning
- Policy auditing
- Approval workflows
- Custom threshold calculations (e.g., weighted by category)

---

## 13. Phase 8 Completion Checklist

- [x] Policy entity with project + environment + thresholds
- [x] PolicyEnvironment enum
- [x] PolicyRepository with custom queries
- [x] CreatePolicyRequest and PolicyResponse DTOs
- [x] PolicyService with CRUD + validation
- [x] PolicyController with REST endpoints (POST, GET, PUT)
- [x] PolicyEngine evaluation logic
- [x] PolicyEvaluation result DTO
- [x] PolicyNotFoundException exception
- [x] GlobalExceptionHandler updated
- [x] Database schema with unique constraint
- [x] Foreign key with CASCADE delete
- [x] Project-level isolation documented
- [x] Validation rules documented
- [x] Build verified

---

## 14. Quick Reference

### Create Policy
```
POST /api/projects/{projectId}/policies
{
  "environment": "PRODUCTION",
  "maxCritical": 0,
  "maxHigh": 3,
  "maxRiskScore": 70
}
```

### Get Policies
```
GET /api/projects/{projectId}/policies
GET /api/projects/{projectId}/policies/{environment}
```

### Update Policy
```
PUT /api/projects/{projectId}/policies/{environment}
{
  "environment": "PRODUCTION",
  "maxCritical": 0,
  "maxHigh": 5,
  "maxRiskScore": 80
}
```

### Evaluate Scan (Phase 9 - Not Yet Implemented)
```
(Will be): POST /api/scans/{scanId}/evaluate-policy
```

---

**Phase 8: Configurable Security Policy System — Complete ✅**

**Ready for Phase 9 (SecurityDecision) →**
