# Phase 4: Security Report Ingestion

**Status:** ✅ Complete

**Objective:** Implement security report ingestion. Scans aggregate reports from multiple security tools (Trivy, Semgrep, OWASP Dependency Check).

---

## 1. Architecture Overview

### Relationship Model
```
Project (1)
    ↓
    └─→ Pipeline (N)
        ↓
        └─→ Scan (N)
            ↓
            └─→ Report (N)
```

A scan can receive multiple reports from different security tools.

### Report Association with Project
```
Project → Pipeline → Scan → Report
    ↑                           ↓
    └─────── Project Context ──┘

How Report reaches Project:
1. User creates Project
2. User creates Pipeline for Project
3. User creates Scan for Pipeline
4. User uploads Report to Scan
5. Report inherits project context through: Scan.project → Pipeline.project
```

### PostgreSQL Foreign Key Relationships
```
CREATE TABLE report (
    id uuid PRIMARY KEY,
    scan_id uuid NOT NULL REFERENCES scan(id) ON DELETE CASCADE,
    ...
);
```

**Key Points:**
- `scan_id`: Foreign key to scan (enables query of reports by scan)
- `ON DELETE CASCADE`: If scan deleted → all reports deleted
- Unique constraint: `UNIQUE(scan_id, tool)` - one report per tool per scan
- File storage: Filesystem (local directory structure)

---

## 2. Files Created / Modified

### New Files

| File | Purpose | Type |
|------|---------|------|
| `ReportTool.java` | Enum for security tools (TRIVY, SEMGREP, OWASP_DEPENDENCY_CHECK) | Enum |
| `ReportStatus.java` | Enum for report processing status | Enum |
| `Report.java` | JPA entity representing a security report | Entity |
| `ReportRepository.java` | Spring Data JPA repository for Report CRUD | Repository |
| `ReportService.java` | Business logic for report upload and storage | Service |
| `ReportController.java` | REST controller for report management | Controller |
| `ReportNotFoundException.java` | Exception thrown when report not found | Exception |
| `ReportResponse.java` | DTO for returning report data via API | DTO |

### Modified Files

| File | Changes |
|------|---------|
| `GlobalExceptionHandler.java` | Added handler for `ReportNotFoundException` |

---

## 3. Enums

### ReportTool
```java
TRIVY                    // Container vulnerability scanner
SEMGREP                  // Static analysis tool (parsing in future phase)
OWASP_DEPENDENCY_CHECK   // Dependency vulnerability scanner (parsing in future phase)
```

**Phase 4 Scope:** Ingestion only (no parsing yet)

### ReportStatus
```java
RECEIVED       // Report file uploaded to system
PROCESSING     // Report being parsed and findings extracted
PROCESSED      // Report successfully parsed, findings stored
FAILED         // Report processing failed
```

**Phase 4:** Reports created with status=RECEIVED (parsing in Phase 5+)

---

## 4. Report Entity

**Fields:**
- `id` (UUID, Primary Key, auto-generated)
- `scan` (Foreign Key to Scan, required)
- `tool` (Enum: TRIVY, SEMGREP, OWASP_DEPENDENCY_CHECK, required)
- `fileName` (String, max 255 chars, required) - original uploaded filename
- `filePath` (String, max 512 chars, required) - filesystem path where file stored
- `status` (Enum: RECEIVED, PROCESSING, PROCESSED, FAILED, default: RECEIVED)
- `receivedAt` (LocalDateTime, auto-set to now, not updatable)

**Indexes:**
- `idx_report_scan_id` (on `scan_id`) - for fast scan filtering
- `idx_report_tool` (on `tool`) - for tool filtering
- `idx_report_status` (on `status`) - for status filtering
- `uk_report_scan_tool` (UNIQUE on `scan_id`, `tool`) - one report per tool per scan

---

## 5. Filesystem Storage Strategy

### Rationale

**Why Filesystem Storage for MVP?**
1. **Development Simplicity:** No AWS account, API keys, or cloud setup needed
2. **Immediate Testing:** Upload, download, inspect reports locally
3. **Cost:** Free during development (no S3 charges)
4. **Transparency:** Easy to see what's being stored
5. **Future-Proof:** Storage layer is abstracted; can swap to S3/cloud later

**When to Migrate to Cloud?**
- Phase N: Implement cloud storage adapter
- No code changes needed in services (only storage implementation swapped)

### Directory Structure
```
./reports/
├── {scanId}/
│   ├── TRIVY/
│   │   └── 1692801604523_trivy-report.json
│   ├── SEMGREP/
│   │   └── 1692801650123_semgrep-report.json
│   └── OWASP_DEPENDENCY_CHECK/
│       └── 1692801700456_dependency-check-report.json
├── {scanId2}/
│   └── TRIVY/
│       └── 1692802000111_trivy-report.json
```

**Benefits:**
- Organized by scan (easy to manage per-scan files)
- Organized by tool (easy to find specific tool reports)
- Timestamp-prefixed filename (prevents overwrites)

### File Paths Stored in Database
```
./reports/a1b2c3d4-e5f6-7890-abcd-ef1234567890/TRIVY/1692801604523_trivy-report.json
```

---

## 6. REST API Endpoints

### 1. Upload Report (Multipart Form Data)
```
POST /api/scans/{scanId}/reports
Content-Type: multipart/form-data

Form Parameters:
- tool (required): ReportTool enum value (TRIVY, SEMGREP, OWASP_DEPENDENCY_CHECK)
- file (required): Binary file content (JSON format)

Example curl:
curl -X POST http://localhost:8080/api/scans/{scanId}/reports \
  -F "tool=TRIVY" \
  -F "file=@trivy-report.json"

Response (201 Created):
{
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "scanId": "1234567-89ab-cdef-0123-456789abcdef",
    "tool": "TRIVY",
    "fileName": "trivy-report.json",
    "status": "RECEIVED",
    "receivedAt": "2026-08-23T10:30:00"
}

Errors:
- 404 Not Found: Scan doesn't exist
- 400 Bad Request: Missing tool/file, invalid file format, duplicate report
- 409 Conflict: Report from same tool already exists for this scan
```

### 2. Get Reports by Scan
```
GET /api/scans/{scanId}/reports

Response (200 OK):
[
    {
        "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        "scanId": "1234567-89ab-cdef-0123-456789abcdef",
        "tool": "TRIVY",
        "fileName": "trivy-report.json",
        "status": "RECEIVED",
        "receivedAt": "2026-08-23T10:30:00"
    },
    {
        "id": "7890abcd-ef12-3456-7890-abcdef123456",
        "scanId": "1234567-89ab-cdef-0123-456789abcdef",
        "tool": "SEMGREP",
        "fileName": "semgrep-report.json",
        "status": "RECEIVED",
        "receivedAt": "2026-08-23T10:35:00"
    }
]

Errors:
- 404 Not Found: Scan doesn't exist
```

### 3. Get Specific Report
```
GET /api/reports/{reportId}

Response (200 OK):
{
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "scanId": "1234567-89ab-cdef-0123-456789abcdef",
    "tool": "TRIVY",
    "fileName": "trivy-report.json",
    "status": "RECEIVED",
    "receivedAt": "2026-08-23T10:30:00"
}

Errors:
- 404 Not Found: Report doesn't exist
```

---

## 7. Validation Rules

### File Upload Validation
- File must not be empty or null
- File must be JSON format (.json extension or application/json MIME type)
- File name must not exceed 255 characters

### Report Validation
- Scan must exist (404 if not)
- Tool must be valid enum value (TRIVY, SEMGREP, OWASP_DEPENDENCY_CHECK)
- One report per tool per scan (409 Conflict if duplicate)

**Error Examples:**
```json
{
    "error_code": "INVALID_REQUEST",
    "message": "Report file is required and cannot be empty"
}

{
    "error_code": "INVALID_REQUEST",
    "message": "Invalid report file. Supported formats: JSON"
}

{
    "error_code": "INVALID_REQUEST",
    "message": "A report from TRIVY already exists for this scan"
}
```

---

## 8. Database Schema

```sql
CREATE TABLE report (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    scan_id uuid NOT NULL,
    tool varchar(50) NOT NULL,
    file_name varchar(255) NOT NULL,
    file_path varchar(512) NOT NULL,
    status varchar(50) NOT NULL DEFAULT 'RECEIVED',
    received_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_report_scan FOREIGN KEY (scan_id) 
        REFERENCES scan(id) ON DELETE CASCADE,
    CONSTRAINT uk_report_scan_tool UNIQUE (scan_id, tool)
);

CREATE INDEX idx_report_scan_id ON report(scan_id);
CREATE INDEX idx_report_tool ON report(tool);
CREATE INDEX idx_report_status ON report(status);
```

---

## 9. Postman Testing Steps

### Setup
- Base URL: `http://localhost:8080`
- Test with sample JSON report file

### Test 1: Create Project
```
POST http://localhost:8080/api/projects
{
    "name": "Payment API",
    "repositoryUrl": "https://github.com/company/payment-api"
}

Expected: 201 Created
Save: projectId
```

### Test 2: Create Pipeline
```
POST http://localhost:8080/api/projects/{projectId}/pipelines
{
    "provider": "JENKINS",
    "jobName": "payment-api-pipeline",
    "buildNumber": 42,
    "branch": "main",
    "commitSha": "abc123"
}

Expected: 201 Created
Save: pipelineId
```

### Test 3: Create Scan
```
POST http://localhost:8080/api/projects/{projectId}/scans
{
    "pipelineId": "{pipelineId}",
    "environment": "DEVELOPMENT"
}

Expected: 201 Created
Save: scanId
```

### Test 4: Upload Trivy Report
```
POST http://localhost:8080/api/scans/{scanId}/reports
Content-Type: multipart/form-data

Form Data:
- tool: TRIVY
- file: (select trivy-report.json file)

Expected: 201 Created
Response:
{
    "id": "report-uuid",
    "scanId": "{scanId}",
    "tool": "TRIVY",
    "fileName": "trivy-report.json",
    "status": "RECEIVED",
    "receivedAt": "2026-08-23T..."
}

Save: reportId1
```

### Test 5: Upload Semgrep Report (Same Scan)
```
POST http://localhost:8080/api/scans/{scanId}/reports
Content-Type: multipart/form-data

Form Data:
- tool: SEMGREP
- file: (select semgrep-report.json file)

Expected: 201 Created
Response: Different reportId, same scanId, tool=SEMGREP
Save: reportId2
```

### Test 6: List Reports for Scan
```
GET http://localhost:8080/api/scans/{scanId}/reports

Expected: 200 OK
Response: Array with 2 reports (TRIVY, SEMGREP)
```

### Test 7: Get Specific Report
```
GET http://localhost:8080/api/reports/{reportId1}

Expected: 200 OK
Response: Single report details
```

### Test 8: Upload Duplicate Report (Should Fail)
```
POST http://localhost:8080/api/scans/{scanId}/reports
Content-Type: multipart/form-data

Form Data:
- tool: TRIVY (same tool as Test 4)
- file: (select trivy-report.json file)

Expected: 400 Bad Request
Response:
{
    "error_code": "INVALID_REQUEST",
    "message": "A report from TRIVY already exists for this scan"
}
```

### Test 9: Upload to Non-Existent Scan
```
POST http://localhost:8080/api/scans/00000000-0000-0000-0000-000000000000/reports
Content-Type: multipart/form-data

Form Data:
- tool: TRIVY
- file: (select trivy-report.json file)

Expected: 404 Not Found
Response:
{
    "error_code": "NOT_FOUND",
    "message": "Scan not found: 00000000-0000-0000-0000-000000000000"
}
```

### Test 10: Upload Empty File
```
POST http://localhost:8080/api/scans/{scanId}/reports
Content-Type: multipart/form-data

Form Data:
- tool: OWASP_DEPENDENCY_CHECK
- file: (empty file)

Expected: 400 Bad Request
Response:
{
    "error_code": "INVALID_REQUEST",
    "message": "Report file is required and cannot be empty"
}
```

### Test 11: Upload Invalid File Format
```
POST http://localhost:8080/api/scans/{scanId}/reports
Content-Type: multipart/form-data

Form Data:
- tool: TRIVY
- file: (select binary/image file)

Expected: 400 Bad Request
Response:
{
    "error_code": "INVALID_REQUEST",
    "message": "Invalid report file. Supported formats: JSON"
}
```

### Test 12: Missing Tool Parameter
```
POST http://localhost:8080/api/scans/{scanId}/reports
Content-Type: multipart/form-data

Form Data:
- file: (select trivy-report.json file)
(missing tool parameter)

Expected: 400 Bad Request
Response: Spring validation error about missing tool parameter
```

---

## 10. Sample Test Report (trivy-report.json)

```json
{
  "SchemaVersion": 2,
  "ArtifactName": "payment-api:latest",
  "ArtifactType": "container_image",
  "Metadata": {
    "OS": {
      "Family": "ubuntu",
      "Name": "20.04"
    },
    "ImageConfig": {
      "created": "2026-08-23T10:00:00Z",
      "user": "root"
    }
  },
  "Results": [
    {
      "Target": "python:3.9-slim (ubuntu 20.04)",
      "Class": "os-pkgs",
      "Type": "ubuntu",
      "Vulnerabilities": [
        {
          "VulnerabilityID": "CVE-2023-12345",
          "PkgName": "libssl1.1",
          "Severity": "HIGH",
          "Title": "OpenSSL vulnerability",
          "Description": "Sample vulnerability description"
        }
      ]
    }
  ]
}
```

---

## 11. Key Design Decisions

1. **Filesystem Storage:** MVP uses local filesystem for simplicity and transparency
   - Future cloud migration requires only storage layer change
   - No cloud provider dependencies during development

2. **Unique Constraint (scan_id, tool):** One report per tool per scan
   - Prevents duplicate ingestion from same tool
   - Allows multiple different tools per scan

3. **Status Lifecycle:** RECEIVED → PROCESSING → PROCESSED/FAILED
   - Phase 4: Only RECEIVED status
   - Phase 5+: Implement parsing and status transitions

4. **File Storage Path:** Hierarchical by scan and tool
   - Easy to organize and find reports
   - Simple to clean up (delete by scan)

5. **Eager Loading:** FetchType.EAGER on Scan relationship
   - Necessary for response DTO conversion
   - Prevents N+1 queries

---

## 12. Multi-Level Isolation Through Report

```
Project A (Payment API)
  ├── Pipeline A1
  │   └── Scan A1-DEV
  │       ├── Report: TRIVY
  │       └── Report: SEMGREP
  │
  └── Pipeline A2
      └── Scan A2-STAGE
          └── Report: TRIVY

Project B (Job Portal)
  └── Pipeline B1
      └── Scan B1-PROD
          └── Report: TRIVY
```

**Report Reaches Project Context:**
```
Project A Report Flow:
  Project A
    └── Pipeline A1 (belongs to Project A)
        └── Scan A1-DEV (belongs to Pipeline A1)
            └── Report: TRIVY (belongs to Scan A1-DEV)

Result: Report is associated with Project A through chain
```

---

## 13. Future Phases

**Phase 5: Report Parsing**
- Parse Trivy JSON reports
- Extract vulnerabilities
- Create Finding entities from vulnerabilities

**Phase 6+: Semgrep & Dependency Check Parsing**
- Support Semgrep parsing
- Support OWASP Dependency Check parsing
- Normalize all findings to common model

---

## 14. Phase Completion Checklist

- [x] ReportTool enum created (TRIVY, SEMGREP, OWASP_DEPENDENCY_CHECK)
- [x] ReportStatus enum created (RECEIVED, PROCESSING, PROCESSED, FAILED)
- [x] Report entity with foreign key to Scan
- [x] ReportRepository with custom queries
- [x] ReportService with file upload and storage
- [x] ReportController with multipart upload endpoint
- [x] ReportNotFoundException exception
- [x] GlobalExceptionHandler updated
- [x] Filesystem storage strategy implemented
- [x] One report per tool per scan enforced
- [x] File validation (JSON format)
- [x] All 12 Postman tests documented
- [x] PostgreSQL foreign keys verified
- [x] Phase 4 documentation complete
- [x] Application builds successfully
- [x] No Finding parsing (reserved for Phase 5)

---

**Phase 4 Complete ✅**

---

## 15. Issues & Solutions

### Issue: Multipart Boundary Not Found in Postman

**Problem:**
- Postman was sending multipart requests without proper boundary headers
- Error: `FileUploadException: the request was rejected because no multipart boundary was found`
- All attempts to upload files failed with 500 Internal Server Error

**Root Cause:**
- Postman's form-data handling had issues with the multipart boundary encoding
- Spring was unable to parse the multipart request properly

**Solution:**
1. Changed ReportController to accept `tool` as query parameter: `?tool=TRIVY`
2. Only `file` remains in form-data body
3. Removed strict `consumes` attribute from `@PostMapping`
4. Added multipart configuration to `application.properties`:
   - `spring.servlet.multipart.enabled=true`
   - `spring.servlet.multipart.max-file-size=10MB`
   - `spring.servlet.multipart.max-request-size=10MB`
5. For reliable testing, use **curl** instead of Postman:
   ```
   curl -X POST "http://localhost:8080/api/scans/{scanId}/reports?tool=TRIVY" -F "file=@trivy-report.json"
   ```

**Result:**
- ✅ File uploads now work correctly
- ✅ Query parameter approach more reliable than form-data parameters
- ✅ Curl works perfectly for multipart uploads

