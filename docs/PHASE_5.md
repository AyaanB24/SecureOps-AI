# Phase 5: Finding Domain - Normalized Security Findings

## Overview

Phase 5 implements a **normalized security finding model** independent of the security tool. Findings represent individual security issues detected by tools like Trivy, Semgrep, and OWASP Dependency Check, normalized into a unified data structure for analysis and tracking.

## Objective

Build a tool-agnostic finding domain that:
- Normalizes findings from different security tools into a common model
- Provides unique fingerprinting for deduplication across scans
- Enables finding status tracking (OPEN, RESOLVED)
- Maintains data integrity through comprehensive validation

## Data Model

### Finding Entity

```
Finding
├── id (UUID, PK)
├── scan (FK → Scan)              # Which scan detected this finding
├── report (FK → Report)          # Which report contains this finding
├── tool (String)                 # Tool name (TRIVY, SEMGREP, etc.)
├── ruleId (String)               # Tool's rule/CVE identifier
├── title (String)                # Human-readable title
├── description (Text)            # Detailed description
├── severity (Enum)               # CRITICAL, HIGH, MEDIUM, LOW
├── filePath (String, nullable)   # Source file (SAST findings)
├── lineNumber (Int, nullable)    # Source line (SAST findings)
├── packageName (String, nullable)# Package name (dependency findings)
├── packageVersion (String, nullable) # Package version
├── fingerprint (String)          # SHA-256 hash for deduplication
├── status (Enum)                 # OPEN, RESOLVED
└── createdAt (Timestamp)         # Finding creation timestamp
```

### Enums

**Severity**: CRITICAL, HIGH, MEDIUM, LOW  
**FindingStatus**: OPEN, RESOLVED

### Constraints

- Unique composite index on (scan_id, fingerprint) prevents duplicate findings
- Foreign keys to Scan and Report with CASCADE delete
- Indexes on scan_id, report_id, severity, status, fingerprint for query performance

## Architecture

### Key Components

1. **Finding Entity** (`Finding.java`)
   - JPA entity with UUID primary key
   - Dual foreign keys to Scan and Report
   - All normalized security finding fields

2. **FindingRepository** (`FindingRepository.java`)
   - Query methods: `findByScanId()`, `findByFingerprint()`, `findByReportIdAndSeverity()`
   - Custom queries for finding lookups

3. **FingerprintService** (`FingerprintService.java`)
   - Generates deterministic SHA-256 fingerprints
   - Input: tool|ruleId|filePath|lineNumber|packageName
   - Same vulnerability across scans = same fingerprint

4. **FindingService** (`FindingService.java`)
   - Business logic for finding creation and retrieval
   - Validates report belongs to scan (cross-project prevention)
   - Generates fingerprints automatically
   - Methods: `createFinding()`, `getFindingsByScan()`, `getFindingById()`, `resolveFinding()`

5. **FindingController** (`FindingController.java`)
   - REST API endpoints for finding management
   - Validates inputs and relationships

6. **FindingException** (`FindingNotFoundException.java`)
   - Runtime exception for missing findings
   - Handled by GlobalExceptionHandler

7. **DTOs** (`FindingResponse`, `CreateFindingRequest`)
   - Type-safe request/response contracts

### Security Validation

**Critical Cross-Project Prevention Check:**
```java
if (!report.getScan().getId().equals(scan.getId())) {
    throw new IllegalArgumentException(
        "Report does not belong to the specified scan");
}
```

This prevents users from associating a report from one scan to another scan, which would violate project isolation.

## API Endpoints

### 1. Get Findings for a Scan

```
GET /api/scans/{scanId}/findings
```

**Response** (200 OK):
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "scanId": "550e8400-e29b-41d4-a716-446655440001",
    "reportId": "550e8400-e29b-41d4-a716-446655440002",
    "tool": "TRIVY",
    "ruleId": "CVE-2023-12345",
    "title": "High severity vulnerability in libssl1.1",
    "description": "OpenSSL 1.1.1 contains a vulnerability...",
    "severity": "CRITICAL",
    "filePath": null,
    "lineNumber": null,
    "packageName": "libssl1.1",
    "packageVersion": "1.1.1",
    "fingerprint": "a1b2c3d4e5f6...",
    "status": "OPEN",
    "createdAt": "2026-09-07T10:30:00"
  }
]
```

**Validation:**
- Scan must exist (returns 404 if not)

### 2. Get Specific Finding

```
GET /api/findings/{findingId}
```

**Response** (200 OK):
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "scanId": "550e8400-e29b-41d4-a716-446655440001",
  "reportId": "550e8400-e29b-41d4-a716-446655440002",
  "tool": "TRIVY",
  "ruleId": "CVE-2023-12345",
  "title": "High severity vulnerability in libssl1.1",
  "description": "OpenSSL 1.1.1 contains a vulnerability...",
  "severity": "CRITICAL",
  "filePath": null,
  "lineNumber": null,
  "packageName": "libssl1.1",
  "packageVersion": "1.1.1",
  "fingerprint": "a1b2c3d4e5f6...",
  "status": "OPEN",
  "createdAt": "2026-09-07T10:30:00"
}
```

**Error** (404 Not Found):
```json
{
  "code": "NOT_FOUND",
  "message": "Finding not found with id: 550e8400-e29b-41d4-a716-446655440000"
}
```

### 3. Create Finding (Development/Testing)

```
POST /api/scans/{scanId}/findings?reportId={reportId}
Content-Type: application/json

{
  "tool": "TRIVY",
  "ruleId": "CVE-2023-12345",
  "title": "High severity vulnerability in libssl1.1",
  "description": "OpenSSL 1.1.1 contains a vulnerability allowing RCE",
  "severity": "CRITICAL",
  "filePath": null,
  "lineNumber": null,
  "packageName": "libssl1.1",
  "packageVersion": "1.1.1"
}
```

**Response** (201 Created):
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "scanId": "550e8400-e29b-41d4-a716-446655440001",
  "reportId": "550e8400-e29b-41d4-a716-446655440002",
  "tool": "TRIVY",
  "ruleId": "CVE-2023-12345",
  "title": "High severity vulnerability in libssl1.1",
  "description": "OpenSSL 1.1.1 contains a vulnerability...",
  "severity": "CRITICAL",
  "filePath": null,
  "lineNumber": null,
  "packageName": "libssl1.1",
  "packageVersion": "1.1.1",
  "fingerprint": "a1b2c3d4e5f6...",
  "status": "OPEN",
  "createdAt": "2026-09-07T10:30:00"
}
```

**Errors:**
- (404 Not Found) Scan doesn't exist
- (404 Not Found) Report doesn't exist
- (400 Bad Request) Report doesn't belong to scan
- (400 Bad Request) Validation failed (missing required fields)

## Fingerprint Strategy

### Why Fingerprints Matter

**Deduplication:** Same vulnerability in same location = same fingerprint across scans.
```
Scan 1: CVE-2023-12345 in package libssl1.1 v1.1.1 → fingerprint = a1b2c3d4e5f6...
Scan 2: Same CVE in same package version → fingerprint = a1b2c3d4e5f6...
→ System recognizes this is the same vulnerability (not a duplicate alert)
```

**Tracking:** Verify vulnerability remediation over time.
```
Scan 1: Finding with fingerprint X marked OPEN
Scan 2: Same fingerprint X not found → Vulnerability was fixed
Scan 3: Same fingerprint X found again → Vulnerability regressed
```

**Trending:** Analyze vulnerability lifecycle and fix rates.

### Implementation

Fingerprint = SHA-256(`tool|ruleId|filePath|lineNumber|packageName`)

**Stable Properties:**
- `tool`: Security tool name (doesn't change)
- `ruleId`: Rule/CVE identifier (doesn't change)
- `filePath`: Source file path (stable for SAST)
- `lineNumber`: Source line (stable for SAST)
- `packageName`: Package name (stable for dependencies)

**Non-Included Properties:**
- `severity`: Can change as tool updates
- `description`: Can be updated or rephrased
- `packageVersion`: Changes as package is upgraded
- `createdAt`: Different for each scan

This ensures the fingerprint stays consistent for the same underlying vulnerability across scans and tool updates.

## Data Association Through Project

```
Project
└── Pipeline
    └── Scan
        ├── Report
        │   └── Finding
        │       └── Finding
        │       └── Finding
        └── Report
            └── Finding
```

**Finding → Project Relationship:**
1. Finding has FK to Report
2. Report has FK to Scan (which specifies environment)
3. Scan has FK to both Project and Pipeline
4. Pipeline has FK to Project
5. **Result**: Every finding traces back to its project through its scan

**Security**: Cross-project finding creation prevented by checking `report.getScan().getId() == requestedScanId`.

## Implementation Details

### Files Created

1. **Finding.java** - JPA entity with all fields
2. **FindingRepository.java** - Data access layer
3. **FingerprintService.java** - Deterministic fingerprint generation
4. **FindingService.java** - Business logic
5. **FindingController.java** - REST API endpoints
6. **FindingNotFoundException.java** - Exception class
7. **FindingResponse.java** - Response DTO
8. **CreateFindingRequest.java** - Request DTO
9. **GlobalExceptionHandler.java** - Added FindingNotFoundException handler

### Database

Table auto-created by Hibernate (ddl-auto=update):

```sql
CREATE TABLE finding (
    id UUID PRIMARY KEY,
    scan_id UUID NOT NULL REFERENCES scan(id) ON DELETE CASCADE,
    report_id UUID NOT NULL REFERENCES report(id) ON DELETE CASCADE,
    tool VARCHAR(50) NOT NULL,
    rule_id VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    severity VARCHAR(50) NOT NULL,
    file_path VARCHAR(512),
    line_number INTEGER,
    package_name VARCHAR(255),
    package_version VARCHAR(255),
    fingerprint VARCHAR(64) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(fingerprint),
    INDEX idx_finding_scan_id (scan_id),
    INDEX idx_finding_report_id (report_id),
    INDEX idx_finding_severity (severity),
    INDEX idx_finding_status (status),
    INDEX idx_finding_fingerprint (fingerprint)
);
```

## Testing with Postman

### Setup

1. Ensure project, pipeline, and scan exist
2. Ensure report exists for the scan
3. Create sample finding using POST endpoint
4. Verify with GET endpoints

### Test Scenarios

**Test 1: Create Finding**
```
POST /api/scans/{scanId}/findings?reportId={reportId}
Body:
{
  "tool": "TRIVY",
  "ruleId": "CVE-2023-12345",
  "title": "Critical vulnerability",
  "description": "Test finding",
  "severity": "CRITICAL",
  "packageName": "libssl1.1",
  "packageVersion": "1.1.1"
}
```

**Test 2: Get Findings for Scan**
```
GET /api/scans/{scanId}/findings
```

**Test 3: Get Specific Finding**
```
GET /api/findings/{findingId}
```

**Test 4: Cross-Project Prevention (should fail)**
```
POST /api/scans/{scanId1}/findings?reportId={reportFromScanId2}
→ Should return 400: "Report does not belong to the specified scan"
```

## Validation Rules

| Field | Required | Constraint |
|-------|----------|-----------|
| tool | Yes | Must match report's tool |
| ruleId | Yes | Non-empty string |
| title | Yes | Non-empty string |
| description | Yes | Non-empty string |
| severity | Yes | CRITICAL, HIGH, MEDIUM, LOW |
| filePath | No | Optional for SAST findings |
| lineNumber | No | Optional for SAST findings |
| packageName | No | Optional for dependency findings |
| packageVersion | No | Optional for dependency findings |

## Future Enhancements

Not implemented in Phase 5:
- Automated finding parsing from Trivy, Semgrep, OWASP reports
- Risk scoring (CVSS calculation, exploit availability)
- Policy-based remediation (SLA tracking, auto-resolution)
- Finding comments and audit trail
- Mass finding resolution
- Finding filtering and search

## Summary

Phase 5 introduces a **tool-independent finding model** that normalizes security findings from multiple tools into a unified database structure. The deterministic fingerprint strategy enables **deduplication across scans** and **vulnerability tracking over time**, forming the foundation for future advanced security analytics and remediation workflows.

The implementation includes comprehensive validation to prevent cross-project data leakage and provides a clean REST API for finding management and analysis.
