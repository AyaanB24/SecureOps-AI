# Phase 9: SecurityDecision & Policy Enforcement

**Status:** ✅ Complete

**Objective:** Combine findings, risk scoring, and policy evaluation to produce deterministic PASS/BLOCK security decisions.

---

## 1. Overview

Phase 9 implements the **SecurityDecision** system, which takes all the work from previous phases and generates a final binary verdict: **PASS** or **BLOCK**.

### Key Features

- **Deterministic:** Same findings always produce same decision
- **Policy-Driven:** Based on project's environment-specific policies
- **Combined Evaluation:** Integrates findings + risk + policy
- **No AI:** Pure logic-based decision making
- **Reproducible:** Same result across all systems

---

## 2. Decision Flow

### 10-Step Process

```
1. Load scan (includes project and environment)
   ↓
2. Determine project from scan
   ↓
3. Determine environment from scan
   ↓
4. Load project policy for that environment
   ↓
5. Count findings by severity
   ↓
6. Calculate risk score (Phase 7)
   ↓
7. Evaluate policy thresholds
   ↓
8. Generate PASS or BLOCK
   ↓
9. Store SecurityDecision in database
   ↓
10. Return decision with reasoning
```

---

## 3. Decision Logic

### Conditions for PASS

A scan receives **PASS** if ALL of these are true:

```
CRITICAL findings ≤ policy.maxCritical AND
HIGH findings ≤ policy.maxHigh AND
Risk score ≤ policy.maxRiskScore
```

### Conditions for BLOCK

A scan receives **BLOCK** if ANY of these is true:

```
CRITICAL findings > policy.maxCritical OR
HIGH findings > policy.maxHigh OR
Risk score > policy.maxRiskScore
```

---

## 4. Data Model

### SecurityDecision Entity

```
id: UUID (primary key)
scan: FK to Scan (one-to-one, unique)
decision: PASS or BLOCK
riskScore: Integer
criticalCount: Long
highCount: Long
mediumCount: Long
lowCount: Long
reason: String (detailed explanation)
createdAt: LocalDateTime

Unique Constraint: scan_id (one decision per scan)
```

### Decision Enum

```java
PASS  // All policy thresholds satisfied
BLOCK // One or more thresholds exceeded
```

---

## 5. API Endpoint

### Get Security Decision

```
GET /api/scans/{scanId}/decision
```

**Purpose:** Generate and return security decision for a scan.

**Path Parameters:**
- `scanId`: UUID of the scan

**Response (200 OK):**

**PASS Example:**
```json
{
  "scanId": "e5fcba61-e7f2-4e85-aae3-30888a93401a",
  "decision": "PASS",
  "riskScore": 38,
  "criticalCount": 0,
  "highCount": 1,
  "mediumCount": 2,
  "lowCount": 0,
  "reason": "Security policy satisfied",
  "createdAt": "2026-10-07T10:30:00"
}
```

**BLOCK Example:**
```json
{
  "scanId": "f1a2b3c4-d5e6-4f7g-8h9i-0j1k2l3m4n5o",
  "decision": "BLOCK",
  "riskScore": 85,
  "criticalCount": 1,
  "highCount": 2,
  "mediumCount": 1,
  "lowCount": 0,
  "reason": "Critical findings (1) exceed the configured production policy limit (0)",
  "createdAt": "2026-10-07T10:35:00"
}
```

**Error Responses:**
- 404 Not Found: Scan doesn't exist
- 404 Not Found: Policy for scan's environment doesn't exist

---

## 6. Example Scenarios

### Scenario 1: PASS Decision

**Policy (Production):**
```
maxCritical: 0
maxHigh: 3
maxRiskScore: 70
```

**Scan Findings:**
```
CRITICAL: 0
HIGH: 1
MEDIUM: 2
LOW: 0
Risk Score: 38
```

**Evaluation:**
```
0 ≤ 0 ✓ (CRITICAL OK)
1 ≤ 3 ✓ (HIGH OK)
38 ≤ 70 ✓ (RISK OK)

Decision: PASS
Reason: "Security policy satisfied"
```

---

### Scenario 2: BLOCK Decision (Critical Exceeded)

**Policy (Production):**
```
maxCritical: 0
maxHigh: 3
maxRiskScore: 70
```

**Scan Findings:**
```
CRITICAL: 1
HIGH: 2
MEDIUM: 1
LOW: 0
Risk Score: 85
```

**Evaluation:**
```
1 ≤ 0 ✗ (CRITICAL FAILED)
2 ≤ 3 ✓ (HIGH OK)
85 ≤ 70 ✗ (RISK FAILED)

Decision: BLOCK
Reason: "Critical findings (1) exceed the configured production policy limit (0)"
```

(First failure reason returned)

---

### Scenario 3: BLOCK Decision (Risk Score Exceeded)

**Policy (Staging):**
```
maxCritical: 2
maxHigh: 5
maxRiskScore: 100
```

**Scan Findings:**
```
CRITICAL: 1
HIGH: 3
MEDIUM: 2
LOW: 1
Risk Score: 125
```

**Evaluation:**
```
1 ≤ 2 ✓ (CRITICAL OK)
3 ≤ 5 ✓ (HIGH OK)
125 ≤ 100 ✗ (RISK FAILED)

Decision: BLOCK
Reason: "Risk score (125) exceeds the configured staging policy limit (100)"
```

---

## 7. Reason Generation

### PASS Reason
Always:
```
"Security policy satisfied"
```

### BLOCK Reasons
Specific to the first failed threshold:

1. **Critical threshold exceeded:**
   ```
   "Critical findings (X) exceed the configured {environment} policy limit (Y)"
   ```

2. **High threshold exceeded:**
   ```
   "High severity findings (X) exceed the configured {environment} policy limit (Y)"
   ```

3. **Risk score threshold exceeded:**
   ```
   "Risk score (X) exceeds the configured {environment} policy limit (Y)"
   ```

---

## 8. Determinism Guarantee

### Same Input = Same Output

Given the same:
- Findings (same vulnerability data)
- Risk calculation (from Phase 7)
- Policy configuration (from Phase 8)

The decision is **always identical**, regardless of:
- Time of day
- System load
- Number of requests
- Which server processes it

### Why Deterministic?

- No randomness in calculation
- No AI model inference
- Pure threshold-based logic
- All components deterministic (Phase 5, 7, 8)

---

## 9. Integration Points

### Input (From Previous Phases)

- **Findings** (Phase 5): Normalized vulnerability data
- **Risk Score** (Phase 7): Weighted severity calculation
- **Policy** (Phase 8): Environment-specific thresholds

### Output (To Future Phases)

- **Decision** (Phase 9): PASS/BLOCK verdict
- **Stored** in database for auditing
- Ready for enforcement (Phase 10+)

---

## 10. Testing Workflow

### Prerequisites
- Phases 1-8 complete
- Policy created for project's environment
- Findings created for scan
- Risk score calculated

### Test Steps

**Step 1: Create PASS Scenario**
```
1. Create project
2. Create policy (lenient thresholds)
3. Create scan
4. Create findings (below thresholds)
5. GET /api/scans/{scanId}/decision
6. Verify: decision="PASS"
```

**Step 2: Create BLOCK Scenario**
```
1. Create project
2. Create policy (strict thresholds)
3. Create scan
4. Create findings (above thresholds)
5. GET /api/scans/{scanId}/decision
6. Verify: decision="BLOCK"
```

---

## 11. Postman Testing

### Test 1: Get PASS Decision
```
GET http://localhost:8080/api/scans/{pass_scanId}/decision

Expected:
- Status: 200 OK
- decision: "PASS"
- reason: "Security policy satisfied"
```

### Test 2: Get BLOCK Decision
```
GET http://localhost:8080/api/scans/{block_scanId}/decision

Expected:
- Status: 200 OK
- decision: "BLOCK"
- reason: Contains failure reason
```

### Test 3: Get Non-Existent Decision
```
GET http://localhost:8080/api/scans/00000000-0000-0000-0000-000000000000/decision

Expected:
- Status: 404 Not Found
- message: "Scan not found"
```

---

## 12. Implementation Details

### Files Created
1. **Decision.java** - Enum (PASS, BLOCK)
2. **SecurityDecision.java** - JPA entity
3. **SecurityDecisionRepository.java** - Repository
4. **SecurityDecisionResponse.java** - Response DTO
5. **DecisionService.java** - Business logic
6. **DecisionController.java** - REST endpoint
7. **SecurityDecisionNotFoundException.java** - Exception

### Files Modified
- **GlobalExceptionHandler.java** - Added exception handler

### Database Changes
- New `security_decision` table
- Unique constraint on `scan_id` (one decision per scan)
- Indexes: `scan_id`, `decision`, `created_at`

---

## 13. Key Design Decisions

### 1. One Decision Per Scan
- Unique constraint enforces one-to-one relationship
- Prevents duplicate decision creation
- Clear audit trail

### 2. Deterministic Decision Logic
- Pure threshold evaluation (no ML/AI)
- Reproducible across systems
- Easy to understand and debug

### 3. Detailed Reasons
- Specific failure reason helps debugging
- Clear communication to users
- Actionable guidance

### 4. Combined Evaluation
- Integrates all previous phases
- Complete security assessment
- Single source of truth

---

## 14. Phase 9 Completion Checklist

- [x] Decision enum (PASS, BLOCK)
- [x] SecurityDecision entity with all fields
- [x] SecurityDecisionRepository
- [x] SecurityDecisionResponse DTO
- [x] DecisionService with 10-step process
- [x] DecisionController with GET endpoint
- [x] 10-step decision flow implemented
- [x] Deterministic logic verified
- [x] Detailed reason generation
- [x] SecurityDecisionNotFoundException
- [x] GlobalExceptionHandler updated
- [x] Database schema with unique constraint
- [x] Foreign key with CASCADE delete
- [x] Comprehensive documentation
- [x] Build verified

---

## 15. What's NOT Included (Phase 10+)

Not implemented in Phase 9:
- Webhook notifications for BLOCK decisions
- Jenkins/CI integration
- Automatic remediation
- Appeal/override workflows
- Decision history tracking
- Trend analysis

---

## 16. Quick Reference

### Decision Endpoint
```
GET /api/scans/{scanId}/decision
```

### Decision Outcomes
```
PASS: All policy thresholds satisfied
BLOCK: One or more thresholds exceeded
```

### Example Decision
```json
{
  "scanId": "scan-uuid",
  "decision": "PASS",
  "riskScore": 38,
  "criticalCount": 0,
  "highCount": 1,
  "mediumCount": 2,
  "lowCount": 0,
  "reason": "Security policy satisfied",
  "createdAt": "2026-10-07T10:30:00"
}
```

---

**Phase 9: SecurityDecision & Policy Enforcement — Complete ✅**

**Ready for Phase 10+ (Integration, Webhooks, CI/CD) →**
