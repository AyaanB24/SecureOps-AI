# Phase 6: Real Trivy JSON Report Parsing

## Overview

Phase 6 implements **real Trivy JSON report parsing** that converts uploaded vulnerability reports into normalized Finding records. This bridges the gap between security tool output and SecureOps' unified finding model.

## Objective

Enable SecureOps to:
- Parse standard Trivy JSON vulnerability reports
- Convert tool-specific vulnerabilities to normalized findings
- Handle edge cases gracefully without crashing
- Track report processing status through the API
- Extract actionable security data automatically

## Architecture

### Parser Interface Pattern

```
SecurityReportParser (Interface)
    ↓
    TrivyParser (Implementation)
    ↓
ReportProcessingService (Orchestration)
    ↓
FindingRepository (Persistence)
```

### Data Models

**TrivyReport** (Root)
```json
{
  "SchemaVersion": 2,
  "ArtifactName": "container:tag",
  "ArtifactType": "container_image",
  "Results": [ TrivyResult[] ]
}
```

**TrivyResult** (Scan Target)
```json
{
  "Target": "package-name (os distro)",
  "Class": "os-pkgs",
  "Vulnerabilities": [ TrivyVulnerability[] ]
}
```

**TrivyVulnerability** (Individual CVE)
```json
{
  "VulnerabilityID": "CVE-2023-12345",
  "PkgName": "libssl1.1",
  "InstalledVersion": "1.1.1",
  "FixedVersion": "1.1.2",
  "Title": "OpenSSL vulnerability",
  "Description": "Detailed description",
  "Severity": "CRITICAL",
  "PrimaryURL": "https://nvd.nist.gov/vuln/detail/CVE-2023-12345"
}
```

## Field Mapping

| Trivy Field | Finding Field | Note |
|-------------|---------------|------|
| VulnerabilityID | ruleId | CVE identifier |
| PkgName | packageName | Package/library name |
| InstalledVersion | packageVersion | Current version |
| Title | title | Vulnerability title |
| Description | description | Detailed description |
| Severity | severity | Normalized to enum |
| PrimaryURL | description | Appended as reference |
| (none) | filePath | null (dependency findings) |
| (none) | lineNumber | null (dependency findings) |
| tool | tool | Always "TRIVY" |

## Key Components

### 1. SecurityReportParser Interface
```java
public interface SecurityReportParser {
    List<Finding> parse(String reportContent, UUID scanId, UUID reportId);
    boolean canParse(String reportContent);
}
```
**Purpose**: Pluggable parser interface enables future implementations (Semgrep, OWASP).

### 2. TrivyParser Implementation
- Deserializes JSON using Jackson ObjectMapper
- Normalizes severity enums with fallback to LOW
- Generates fingerprints for all findings
- Gracefully handles missing/optional fields
- Never crashes on individual vulnerability errors

### 3. ReportProcessingService
- Loads report file from filesystem (stored in Phase 4)
- Routes to appropriate parser
- Persists findings to database
- Updates report status (RECEIVED → PROCESSING → PROCESSED)

### 4. Model Classes (Jackson POJOs)
- **TrivyReport**: Root document
- **TrivyResult**: One scan target
- **TrivyVulnerability**: Individual CVE with @JsonIgnoreProperties(ignoreUnknown=true)

## API Endpoint

### Process Report

```
POST /api/scans/{scanId}/reports/{reportId}/process
```

**Purpose**: Trigger parsing of uploaded report and extraction of findings.

**Path Parameters**:
- `scanId`: UUID of the scan
- `reportId`: UUID of the report (must be RECEIVED status)

**Request Body**: None

**Response** (200 OK):
```json
{
  "reportId": "550e8400-e29b-41d4-a716-446655440002",
  "tool": "TRIVY",
  "findingsCreated": 5,
  "status": "PROCESSED"
}
```

**Status Codes**:
- 200 OK: Processing completed successfully
- 404 Not Found: Report doesn't exist
- 500 Internal Server Error: Processing failed (report status set to FAILED)

## Edge Cases Handled

| Edge Case | Behavior |
|-----------|----------|
| Empty Results array | Returns 0 findings (no crash) |
| Missing Vulnerabilities | Result skipped, process continues |
| Null/blank Severity | Defaults to LOW with warning log |
| Missing Title | Uses VulnerabilityID as title |
| Missing Description | Uses "Vulnerability {ID}" as description |
| Unknown Severity value | Defaults to LOW, logs warning |
| Malformed JSON | Throws ReportParsingException (HTTP 500) |
| Multiple targets | All processed in single call |
| Multiple vulnerabilities per target | All extracted |
| Duplicate CVE in same report | Same fingerprint detected (no dedup in Phase 6) |
| Optional fields (PrimaryURL, CweID) | Safely skipped if missing |

## Processing Flow

```
1. User uploads Trivy JSON via POST /api/scans/{scanId}/reports
   → Report stored with status=RECEIVED

2. User triggers processing via POST /api/scans/{scanId}/reports/{reportId}/process
   ↓
3. ReportProcessingService loads report file
   ↓
4. TrivyParser deserializes JSON into TrivyReport model
   ↓
5. For each TrivyResult → For each TrivyVulnerability:
   - Normalize fields (Title, Description, Severity)
   - Generate deterministic fingerprint
   - Create Finding entity
   ↓
6. FindingRepository persists all findings
   ↓
7. Report status updated to PROCESSED
   ↓
8. Return ReportProcessingResult with finding count

Result: All vulnerabilities now queryable via Finding APIs
```

## Severity Normalization

| Trivy Severity | Maps To | Fallback |
|----------------|---------|----------|
| CRITICAL | Severity.CRITICAL | - |
| HIGH | Severity.HIGH | - |
| MEDIUM | Severity.MEDIUM | - |
| LOW | Severity.LOW | - |
| UNKNOWN | (unknown) | Severity.LOW |
| null/blank | (missing) | Severity.LOW |

## Testing Workflow

### Prerequisites
1. Project, Pipeline, Scan created (Phases 1-3)
2. Report uploaded (Phase 4)
3. Sample Trivy JSON available

### Test Steps

**Step 1: Upload Trivy Report**
```
POST http://localhost:8080/api/scans/{scanId}/reports?tool=TRIVY
Form-data:
  file: trivy-sample.json
```

Response:
```json
{
  "id": "{reportId}",
  "scanId": "{scanId}",
  "tool": "TRIVY",
  "fileName": "trivy-sample.json",
  "status": "RECEIVED",
  "receivedAt": "2026-09-08T14:00:00"
}
```

**Step 2: Process Report**
```
POST http://localhost:8080/api/scans/{scanId}/reports/{reportId}/process
```

Response:
```json
{
  "reportId": "{reportId}",
  "tool": "TRIVY",
  "findingsCreated": 5,
  "status": "PROCESSED"
}
```

**Step 3: Verify Findings Created**
```
GET http://localhost:8080/api/scans/{scanId}/findings
```

Response:
```json
[
  {
    "id": "{findingId}",
    "tool": "TRIVY",
    "ruleId": "CVE-2023-12345",
    "title": "OpenSSL 1.1.1 vulnerability",
    "severity": "CRITICAL",
    "packageName": "libssl1.1",
    "packageVersion": "1.1.1",
    "fingerprint": "...",
    "status": "OPEN"
  },
  ...
]
```

**Step 4: Verify Finding Details**
```
GET http://localhost:8080/api/findings/{findingId}
```

## Sample Trivy Report

Located at: `Sample_Reports/trivy-sample.json`

Contains:
- 3 OS package vulnerabilities (libssl, openssl, libc)
- 2 Python dependency vulnerabilities (requests, jinja2)
- Mix of severities: CRITICAL, HIGH, MEDIUM
- Various optional fields present and missing

## Implementation Details

### Files Created
1. **SecurityReportParser.java** - Interface for pluggable parsers
2. **ReportParsingException.java** - Exception for parsing failures
3. **TrivyReport.java** - Jackson POJO for root document
4. **TrivyResult.java** - Jackson POJO for scan target
5. **TrivyVulnerability.java** - Jackson POJO for CVE
6. **TrivyParser.java** - Trivy implementation with field mapping
7. **ReportProcessingService.java** - Orchestrates parsing and persistence
8. **ReportProcessingResult.java** - API response DTO
9. **trivy-sample.json** - Sample test report

### Files Modified
1. **ReportController.java** - Added POST /api/scans/{scanId}/reports/{reportId}/process
2. **pom.xml** - Added jackson-databind dependency

### Database Changes
No schema changes (uses existing Finding table from Phase 5).

## Error Handling Strategy

### Graceful Degradation
- **Single finding parse error**: Logged as warning, processing continues
- **Invalid Severity**: Defaults to LOW, warning logged
- **Missing optional field**: Safely skipped with null check
- **Invalid JSON**: Throws ReportParsingException, sets report to FAILED status
- **File not found**: Reports ReportNotFoundException

### Logging
- DEBUG: Field-level parsing details
- INFO: Summary statistics (findings count)
- WARN: Edge cases (unknown severity, missing fields)
- ERROR: Critical failures (JSON parse error, file not found)

## Fingerprint Consistency

Fingerprint input: `tool|ruleId|filePath|lineNumber|packageName`

For Trivy reports:
- tool = "TRIVY"
- ruleId = VulnerabilityID (e.g., "CVE-2023-12345")
- filePath = null
- lineNumber = null
- packageName = PkgName

**Result**: Same CVE in same package = same fingerprint across scans.

## Future Enhancements

Not implemented in Phase 6:
- Semgrep parser for SAST findings (filePath, lineNumber populated)
- OWASP Dependency Check parser
- Automatic finding deduplication using fingerprints
- Concurrent report processing for large files
- Partial report retry on failure
- Custom field mapping configuration

## Summary

Phase 6 bridges the gap between Trivy JSON reports and SecureOps' finding model through:
- **Pluggable parser architecture** enabling multi-tool support
- **Robust JSON parsing** with graceful edge case handling
- **Real-time finding extraction** converting CVEs to actionable records
- **Processing API** with status tracking and result reporting
- **Comprehensive logging** for debugging and audit trails

The implementation prioritizes resilience—no missing optional field crashes the system, no individual bad CVE blocks others from processing.

All vulnerabilities detected by Trivy are now normalized, findable, and analyzable within SecureOps.
