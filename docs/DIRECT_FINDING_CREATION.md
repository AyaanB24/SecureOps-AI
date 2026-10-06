# Direct Finding Creation (Phase 5)

## Overview

You can create findings **directly without uploading a report file**. This is useful for:
- Testing findings without report parsing
- Integrating with external APIs that already parse reports
- Creating findings programmatically
- Development and debugging

## API Endpoint

```
POST /api/scans/{scanId}/findings?reportId={reportId}
Content-Type: application/json
```

**Parameters:**
- `scanId` (path): UUID of the scan
- `reportId` (query): UUID of the report (findings must belong to a report)

**Request Body:**
```json
{
  "tool": "TRIVY",
  "ruleId": "CVE-2024-1234",
  "title": "Vulnerability Title",
  "description": "Detailed description of the vulnerability",
  "severity": "CRITICAL",
  "packageName": "libssl1.1",
  "packageVersion": "1.1.1",
  "filePath": null,
  "lineNumber": null
}
```

## Field Reference

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| tool | String | Yes | Security tool identifier | "TRIVY", "SEMGREP", "OWASP_DEPENDENCY_CHECK" |
| ruleId | String | Yes | Rule/CVE identifier from tool | "CVE-2024-1086", "SEMGREP-JAVA-001" |
| title | String | Yes | Human-readable title | "OpenSSL 1.1 Privilege Escalation" |
| description | String | Yes | Detailed finding description | "A use-after-free vulnerability..." |
| severity | String | Yes | Finding severity | "CRITICAL", "HIGH", "MEDIUM", "LOW" |
| packageName | String | No | Package/library name | "libssl1.1", "requests" |
| packageVersion | String | No | Package version | "1.1.1", "2.28.1" |
| filePath | String | No | Source file path (SAST) | "/src/main.py", "app/auth.js" |
| lineNumber | Integer | No | Source line number (SAST) | 42, 105 |

## Complete Workflow

### Step 1: Create Project
```
POST http://localhost:8080/api/projects
{
    "name": "Test Project",
    "repositoryUrl": "https://github.com/test/repo"
}
```
**Save:** `projectId`

---

### Step 2: Create Pipeline
```
POST http://localhost:8080/api/projects/{projectId}/pipelines
{
    "provider": "GITHUB_ACTIONS",
    "jobName": "Test Pipeline",
    "buildNumber": 1,
    "branch": "main",
    "commitSha": "abc123"
}
```
**Save:** `pipelineId`

---

### Step 3: Create Scan
```
POST http://localhost:8080/api/projects/{projectId}/scans
{
    "pipelineId": "{pipelineId}",
    "environment": "DEVELOPMENT"
}
```
**Save:** `scanId`

---

### Step 4: Create Empty Report (No file upload needed!)
```
POST http://localhost:8080/api/scans/{scanId}/reports?tool=TRIVY
Content-Type: multipart/form-data

Form Data:
- file: (upload any small JSON file, e.g., {})
```

**Response:**
```json
{
  "id": "report-id-here",
  "scanId": "scan-id-here",
  "tool": "TRIVY",
  "fileName": "test.json",
  "status": "RECEIVED",
  "receivedAt": "2026-10-06T..."
}
```
**Save:** `reportId`

---

### Step 5: Create Finding Directly (No Parsing!)
```
POST http://localhost:8080/api/scans/{scanId}/findings?reportId={reportId}
Content-Type: application/json

{
  "tool": "TRIVY",
  "ruleId": "CVE-2024-1086",
  "title": "Linux Kernel Privilege Escalation",
  "description": "A use-after-free vulnerability in the Linux kernel nf_tables subsystem could allow a local attacker to elevate privileges.",
  "severity": "CRITICAL",
  "packageName": "linux-image-generic",
  "packageVersion": "5.4.0-42-generic"
}
```

**Response (201 Created):**
```json
{
  "id": "finding-id-1",
  "scanId": "scan-id-here",
  "reportId": "report-id-here",
  "tool": "TRIVY",
  "ruleId": "CVE-2024-1086",
  "title": "Linux Kernel Privilege Escalation",
  "description": "A use-after-free vulnerability...",
  "severity": "CRITICAL",
  "packageName": "linux-image-generic",
  "packageVersion": "5.4.0-42-generic",
  "filePath": null,
  "lineNumber": null,
  "fingerprint": "sha256hash...",
  "status": "OPEN",
  "createdAt": "2026-10-06T..."
}
```

---

### Step 6: Create More Findings (Repeat Step 5)

```
POST http://localhost:8080/api/scans/{scanId}/findings?reportId={reportId}
{
  "tool": "TRIVY",
  "ruleId": "CVE-2023-46604",
  "title": "Apache HTTP Server Expression Parser",
  "description": "A flaw in the expression parser in Apache HTTP Server could allow remote attackers to execute arbitrary code.",
  "severity": "CRITICAL",
  "packageName": "apache2",
  "packageVersion": "2.4.41-1ubuntu1.5"
}
```

```
POST http://localhost:8080/api/scans/{scanId}/findings?reportId={reportId}
{
  "tool": "TRIVY",
  "ruleId": "CVE-2023-44487",
  "title": "OpenSSL HTTP/2 Rapid Reset Attack",
  "description": "The HTTP/2 protocol allows remote attackers to cause a denial of service by sending rapid requests and resets.",
  "severity": "HIGH",
  "packageName": "openssl",
  "packageVersion": "1.1.1f-1ubuntu2.16"
}
```

---

### Step 7: Query All Findings
```
GET http://localhost:8080/api/scans/{scanId}/findings
```

**Response:**
```json
[
  {
    "id": "finding-id-1",
    "ruleId": "CVE-2024-1086",
    "title": "Linux Kernel Privilege Escalation",
    "severity": "CRITICAL",
    "status": "OPEN"
  },
  {
    "id": "finding-id-2",
    "ruleId": "CVE-2023-46604",
    "title": "Apache HTTP Server Expression Parser",
    "severity": "CRITICAL",
    "status": "OPEN"
  },
  {
    "id": "finding-id-3",
    "ruleId": "CVE-2023-44487",
    "title": "OpenSSL HTTP/2 Rapid Reset Attack",
    "severity": "HIGH",
    "status": "OPEN"
  }
]
```

---

## Postman Collection Examples

### Example 1: TRIVY Dependency Finding
```json
{
  "tool": "TRIVY",
  "ruleId": "CVE-2024-22195",
  "title": "Requests Library SSRF Vulnerability",
  "description": "A server-side request forgery (SSRF) vulnerability in requests library could allow attackers to make unauthorized HTTP requests to internal services.",
  "severity": "HIGH",
  "packageName": "requests",
  "packageVersion": "2.28.1"
}
```

### Example 2: SEMGREP SAST Finding (With File Location)
```json
{
  "tool": "SEMGREP",
  "ruleId": "python.lang.security.injection.sql.sql-injection",
  "title": "SQL Injection Vulnerability",
  "description": "User input is directly concatenated into SQL query without parameterization, leading to potential SQL injection attacks.",
  "severity": "CRITICAL",
  "filePath": "src/database.py",
  "lineNumber": 42
}
```

### Example 3: OWASP Dependency Finding
```json
{
  "tool": "OWASP_DEPENDENCY_CHECK",
  "ruleId": "jackson-databind-RCE",
  "title": "Jackson Databind Remote Code Execution",
  "description": "The jackson-databind library is vulnerable to remote code execution through deserialization of untrusted data.",
  "severity": "CRITICAL",
  "packageName": "com.fasterxml.jackson.core:jackson-databind",
  "packageVersion": "2.13.0"
}
```

---

## Key Differences: Report Upload vs Direct Creation

### Report Upload + Parse (Phases 4 & 6)
```
1. Upload Trivy JSON file (Phase 4)
   POST /api/scans/{scanId}/reports?tool=TRIVY
   
2. Process report via parser (Phase 6)
   POST /api/scans/{scanId}/reports/{reportId}/process
   
3. Findings created automatically from JSON
```

**Pros:**
- Automatic extraction from tool reports
- Bulk findings creation
- Tool-agnostic (Trivy, Semgrep, OWASP)

**Cons:**
- Requires report file upload
- Parsing takes time
- Debug complex JSON structures

### Direct Creation (Phase 5)
```
1. Create report (minimal, just metadata)
   POST /api/scans/{scanId}/reports?tool=TRIVY
   
2. Create finding directly
   POST /api/scans/{scanId}/findings?reportId={reportId}
   
3. No parsing, instant response
```

**Pros:**
- Fast, no file upload
- Perfect for testing
- Good for programmatic creation
- Debug individual findings
- Test before running full parser

**Cons:**
- Manual per-finding creation
- Not for bulk operations
- No automatic extraction

---

## Use Cases

| Use Case | Method | Why |
|----------|--------|-----|
| Upload real Trivy report → Extract 100 CVEs | Report Upload + Parse | Bulk processing, automatic |
| Test 1-2 findings quickly | Direct Creation | Fast, no file upload |
| Debug parser issues | Direct Creation | Test findings independently |
| Integration with external API | Direct Creation | Programmatic control |
| Development/Testing | Direct Creation | Quick iteration |
| Production security scanning | Report Upload + Parse | Reliable, automated |

---

## Validation Rules

- **Scan must exist:** Use valid `scanId` from Step 3
- **Report must exist:** Use valid `reportId` from Step 4
- **Report must belong to scan:** `reportId` must be from the same scan
- **Required fields:** tool, ruleId, title, description, severity (all case-sensitive)
- **Severity values:** "CRITICAL", "HIGH", "MEDIUM", "LOW" (uppercase)
- **Tool values:** "TRIVY", "SEMGREP", "OWASP_DEPENDENCY_CHECK"

---

## Error Handling

### Scan Not Found
```
GET /api/scans/00000000-0000-0000-0000-000000000000/findings

Response (404):
{
  "error_code": "NOT_FOUND",
  "message": "Scan not found: 00000000-0000-0000-0000-000000000000"
}
```

### Report Not Found
```
POST /api/scans/{scanId}/findings?reportId=00000000-0000-0000-0000-000000000000

Response (404):
{
  "error_code": "NOT_FOUND",
  "message": "Report not found: 00000000-0000-0000-0000-000000000000"
}
```

### Report Doesn't Belong to Scan
```
POST /api/scans/{scanId1}/findings?reportId={reportId2}
(where reportId2 belongs to scanId2, not scanId1)

Response (400):
{
  "error_code": "INVALID_REQUEST",
  "message": "Report does not belong to the specified scan"
}
```

### Missing Required Field
```
{
  "tool": "TRIVY",
  "ruleId": "CVE-2024-1234"
  (missing title, description, severity)
}

Response (400):
{
  "error_code": "INVALID_REQUEST",
  "message": "Title is required"
}
```

---

## Summary

**Yes, you can directly post findings!** Use:

```
POST /api/scans/{scanId}/findings?reportId={reportId}
```

with a JSON body containing tool, ruleId, title, description, severity, and optional fields.

This is **ideal for testing** and **development** without waiting for report parsing.
