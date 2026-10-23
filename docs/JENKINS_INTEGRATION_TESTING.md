# Jenkins Integration - Manual Testing Guide

**Purpose:** Step-by-step commands to test all Phase 11 APIs without Jenkins

**Prerequisites:**
- Backend running: `http://localhost:8080`
- PostgreSQL running on localhost:5432
- curl installed
- jq installed (for JSON parsing)

---

## Test 1: Find Project by Name (New in Phase 11)

### 1.1 Test: Project doesn't exist (404)

```bash
curl -s -X GET "http://localhost:8080/api/projects?name=nonexistent-project-$(date +%s)" \
  -H "Content-Type: application/json" | jq .
```

**Expected Response:**
```json
{
  "errorCode": "NOT_FOUND",
  "message": "Project not found with name: nonexistent-project-..."
}
```

**HTTP Status:** 404 Not Found

### 1.2 Test: List all projects (no name parameter)

```bash
curl -s -X GET "http://localhost:8080/api/projects" \
  -H "Content-Type: application/json" | jq .
```

**Expected Response:**
```json
[
  {
    "id": "...",
    "name": "...",
    "repositoryUrl": "...",
    "createdAt": "..."
  },
  ...
]
```

**HTTP Status:** 200 OK

### 1.3 Test: Create project

```bash
PROJECT_NAME="payment-service-$(date +%s)"
curl -s -X POST "http://localhost:8080/api/projects" \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"$PROJECT_NAME\",
    \"repositoryUrl\": \"https://github.com/company/payment-service.git\"
  }" | jq .
```

**Expected Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "payment-service-...",
  "repositoryUrl": "https://github.com/company/payment-service.git",
  "createdAt": "2026-10-19T..."
}
```

**HTTP Status:** 201 Created

**Save the PROJECT_ID for next tests:**
```bash
PROJECT_ID="550e8400-e29b-41d4-a716-446655440000"
```

### 1.4 Test: Find project by name (200 OK)

```bash
curl -s -X GET "http://localhost:8080/api/projects?name=payment-service-$(date +%s)" \
  -H "Content-Type: application/json" | jq .
```

**Expected Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "payment-service-...",
  "repositoryUrl": "https://github.com/company/payment-service.git",
  "createdAt": "2026-10-19T..."
}
```

**HTTP Status:** 200 OK

---

## Test 2: Create Pipeline (Duplicate Prevention)

### 2.1 Test: Create first pipeline

```bash
PIPELINE_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/projects/$PROJECT_ID/pipelines" \
  -H "Content-Type: application/json" \
  -d "{
    \"provider\": \"JENKINS\",
    \"jobName\": \"payment-service-build\",
    \"buildNumber\": 1,
    \"branch\": \"main\",
    \"commitSha\": \"abc123def456\"
  }")

echo "$PIPELINE_RESPONSE" | jq .
PIPELINE_ID=$(echo "$PIPELINE_RESPONSE" | jq -r '.id')
echo "Pipeline ID: $PIPELINE_ID"
```

**Expected Response:**
```json
{
  "id": "...",
  "projectId": "...",
  "provider": "JENKINS",
  "jobName": "payment-service-build",
  "buildNumber": 1,
  "branch": "main",
  "commitSha": "abc123def456",
  "createdAt": "2026-10-19T..."
}
```

**HTTP Status:** 201 Created

### 2.2 Test: Try to create duplicate pipeline (409 Conflict)

```bash
curl -s -X POST "http://localhost:8080/api/projects/$PROJECT_ID/pipelines" \
  -H "Content-Type: application/json" \
  -d "{
    \"provider\": \"JENKINS\",
    \"jobName\": \"payment-service-build\",
    \"buildNumber\": 1,
    \"branch\": \"main\",
    \"commitSha\": \"abc123def456\"
  }" | jq .
```

**Expected Response:**
```json
{
  "errorCode": "DUPLICATE",
  "message": "A pipeline with this job name and build number already exists for this project"
}
```

**HTTP Status:** 409 Conflict

---

## Test 3: Create Scan

### 3.1 Test: Create scan

```bash
SCAN_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/projects/$PROJECT_ID/scans" \
  -H "Content-Type: application/json" \
  -d "{
    \"pipelineId\": \"$PIPELINE_ID\",
    \"environment\": \"PRODUCTION\"
  }")

echo "$SCAN_RESPONSE" | jq .
SCAN_ID=$(echo "$SCAN_RESPONSE" | jq -r '.id')
echo "Scan ID: $SCAN_ID"
```

**Expected Response:**
```json
{
  "id": "...",
  "projectId": "...",
  "pipelineId": "...",
  "environment": "PRODUCTION",
  "status": "CREATED",
  "startedAt": "2026-10-19T...",
  "completedAt": null
}
```

**HTTP Status:** 201 Created

---

## Test 4: Upload Trivy Report

### 4.1 Setup: Create sample Trivy report

```bash
cat > /tmp/trivy-report.json << 'EOF'
{
  "ArtifactType": "container_image",
  "ArtifactName": "my-app:latest",
  "Results": [
    {
      "Target": "my-app:latest (alpine 3.17.0)",
      "Class": "os-pkgs",
      "Type": "apk",
      "Vulnerabilities": [
        {
          "VulnerabilityID": "CVE-2023-1234",
          "PkgName": "openssl",
          "InstalledVersion": "3.0.8-r1",
          "FixedVersion": "3.0.8-r2",
          "Severity": "HIGH",
          "Title": "OpenSSL vulnerability",
          "Description": "A high severity vulnerability in OpenSSL",
          "PrimaryURL": "https://nvd.nist.gov/vuln/detail/CVE-2023-1234"
        },
        {
          "VulnerabilityID": "CVE-2023-5678",
          "PkgName": "curl",
          "InstalledVersion": "7.88.1-r1",
          "FixedVersion": "7.88.1-r2",
          "Severity": "MEDIUM",
          "Title": "curl vulnerability",
          "Description": "A medium severity vulnerability in curl",
          "PrimaryURL": "https://nvd.nist.gov/vuln/detail/CVE-2023-5678"
        }
      ]
    }
  ]
}
EOF
```

### 4.2 Test: Upload report

```bash
REPORT_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/scans/$SCAN_ID/reports?tool=TRIVY" \
  -F "file=@/tmp/trivy-report.json")

echo "$REPORT_RESPONSE" | jq .
REPORT_ID=$(echo "$REPORT_RESPONSE" | jq -r '.id')
echo "Report ID: $REPORT_ID"
```

**Expected Response:**
```json
{
  "id": "...",
  "scanId": "...",
  "tool": "TRIVY",
  "fileName": "trivy-report.json",
  "status": "RECEIVED",
  "receivedAt": "2026-10-19T..."
}
```

**HTTP Status:** 201 Created

### 4.3 Test: Upload duplicate report (409 Conflict)

```bash
curl -s -X POST "http://localhost:8080/api/scans/$SCAN_ID/reports?tool=TRIVY" \
  -F "file=@/tmp/trivy-report.json" | jq .
```

**Expected Response:**
```json
{
  "errorCode": "INVALID_REQUEST",
  "message": "A report from TRIVY already exists for this scan"
}
```

**HTTP Status:** 400 Bad Request (or 409 Conflict)

---

## Test 5: Process Report (Extract Findings)

### 5.1 Test: Process report

```bash
PROCESS_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/scans/$SCAN_ID/reports/$REPORT_ID/process" \
  -H "Content-Type: application/json")

echo "$PROCESS_RESPONSE" | jq .
```

**Expected Response:**
```json
{
  "reportId": "...",
  "tool": "TRIVY",
  "findingsCreated": 2,
  "status": "PROCESSED"
}
```

**HTTP Status:** 200 OK

---

## Test 6: Get Findings

### 6.1 Test: List findings for scan

```bash
curl -s -X GET "http://localhost:8080/api/scans/$SCAN_ID/findings" \
  -H "Content-Type: application/json" | jq .
```

**Expected Response:**
```json
[
  {
    "id": "...",
    "scanId": "...",
    "reportId": "...",
    "tool": "TRIVY",
    "ruleId": "CVE-2023-1234",
    "title": "OpenSSL vulnerability",
    "description": "A high severity vulnerability in OpenSSL",
    "severity": "HIGH",
    "status": "OPEN",
    "fingerprint": "...",
    "packageName": "openssl",
    "packageVersion": "3.0.8-r1",
    "createdAt": "2026-10-19T..."
  },
  {
    "id": "...",
    "scanId": "...",
    "reportId": "...",
    "tool": "TRIVY",
    "ruleId": "CVE-2023-5678",
    "title": "curl vulnerability",
    "description": "A medium severity vulnerability in curl",
    "severity": "MEDIUM",
    "status": "OPEN",
    "fingerprint": "...",
    "packageName": "curl",
    "packageVersion": "7.88.1-r1",
    "createdAt": "2026-10-19T..."
  }
]
```

**HTTP Status:** 200 OK

**Finding Count:** Should be 2 (HIGH + MEDIUM)

---

## Test 7: Create Policy

### 7.1 Test: Create production policy

```bash
curl -s -X POST "http://localhost:8080/api/projects/$PROJECT_ID/policies" \
  -H "Content-Type: application/json" \
  -d "{
    \"environment\": \"PRODUCTION\",
    \"maxCritical\": 0,
    \"maxHigh\": 1,
    \"maxRiskScore\": 100
  }" | jq .
```

**Expected Response:**
```json
{
  "id": "...",
  "projectId": "...",
  "environment": "PRODUCTION",
  "maxCritical": 0,
  "maxHigh": 1,
  "maxRiskScore": 100,
  "createdAt": "2026-10-19T..."
}
```

**HTTP Status:** 201 Created

---

## Test 8: Get Risk Score

### 8.1 Test: Get risk score for scan

```bash
curl -s -X GET "http://localhost:8080/api/scans/$SCAN_ID/risk" \
  -H "Content-Type: application/json" | jq .
```

**Expected Response:**
```json
{
  "scanId": "...",
  "riskScore": 18,
  "criticalCount": 0,
  "highCount": 1,
  "mediumCount": 1,
  "lowCount": 0
}
```

**HTTP Status:** 200 OK

**Calculation:** (0×10) + (1×7) + (1×4) + (0×1) = 11 (or similar based on actual findings)

---

## Test 9: Get Security Decision (PASS Scenario)

### 9.1 Test: Get decision for scan with findings below thresholds

```bash
DECISION_RESPONSE=$(curl -s -X GET "http://localhost:8080/api/scans/$SCAN_ID/decision" \
  -H "Content-Type: application/json")

echo "$DECISION_RESPONSE" | jq .
```

**Expected Response (PASS):**
```json
{
  "scanId": "...",
  "decision": "PASS",
  "riskScore": 11,
  "criticalCount": 0,
  "highCount": 1,
  "mediumCount": 1,
  "lowCount": 0,
  "reason": "Security policy satisfied",
  "createdAt": "2026-10-19T..."
}
```

**HTTP Status:** 200 OK

**Explanation:** 
- CRITICAL: 0 ≤ 0 ✓
- HIGH: 1 ≤ 1 ✓  
- RISK: 11 ≤ 100 ✓
- Result: PASS

---

## Test 10: Block Scenario

### 10.1 Setup: Create strict policy and findings

```bash
# Create new project
PROJECT_NAME_BLOCK="block-test-$(date +%s)"
PROJECT_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/projects" \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"$PROJECT_NAME_BLOCK\",
    \"repositoryUrl\": \"https://github.com/test/block.git\"
  }")

PROJECT_ID_BLOCK=$(echo "$PROJECT_RESPONSE" | jq -r '.id')

# Create pipeline
PIPELINE_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/projects/$PROJECT_ID_BLOCK/pipelines" \
  -H "Content-Type: application/json" \
  -d "{
    \"provider\": \"JENKINS\",
    \"jobName\": \"security-service-build\",
    \"buildNumber\": 1,
    \"branch\": \"develop\",
    \"commitSha\": \"xyz789abc123\"
  }")

PIPELINE_ID_BLOCK=$(echo "$PIPELINE_RESPONSE" | jq -r '.id')

# Create scan
SCAN_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/projects/$PROJECT_ID_BLOCK/scans" \
  -H "Content-Type: application/json" \
  -d "{
    \"pipelineId\": \"$PIPELINE_ID_BLOCK\",
    \"environment\": \"STAGING\"
  }")

SCAN_ID_BLOCK=$(echo "$SCAN_RESPONSE" | jq -r '.id')

# Create STRICT policy (no CRITICAL allowed)
curl -s -X POST "http://localhost:8080/api/projects/$PROJECT_ID_BLOCK/policies" \
  -H "Content-Type: application/json" \
  -d "{
    \"environment\": \"STAGING\",
    \"maxCritical\": 0,
    \"maxHigh\": 0,
    \"maxRiskScore\": 50
  }" | jq .

# Create report with CRITICAL finding
cat > /tmp/trivy-critical.json << 'EOF'
{
  "ArtifactType": "container_image",
  "ArtifactName": "vulnerable-app:latest",
  "Results": [
    {
      "Target": "vulnerable-app:latest",
      "Class": "os-pkgs",
      "Type": "apk",
      "Vulnerabilities": [
        {
          "VulnerabilityID": "CVE-2023-9999",
          "PkgName": "glibc",
          "InstalledVersion": "2.36-r0",
          "FixedVersion": "2.36-r1",
          "Severity": "CRITICAL",
          "Title": "Remote Code Execution in glibc",
          "Description": "A critical RCE vulnerability in glibc",
          "PrimaryURL": "https://nvd.nist.gov/vuln/detail/CVE-2023-9999"
        }
      ]
    }
  ]
}
EOF

# Upload report
REPORT_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/scans/$SCAN_ID_BLOCK/reports?tool=TRIVY" \
  -F "file=@/tmp/trivy-critical.json")

REPORT_ID_BLOCK=$(echo "$REPORT_RESPONSE" | jq -r '.id')

# Process report
curl -s -X POST "http://localhost:8080/api/scans/$SCAN_ID_BLOCK/reports/$REPORT_ID_BLOCK/process" \
  -H "Content-Type: application/json" | jq .
```

### 10.2 Test: Get decision (should be BLOCK)

```bash
curl -s -X GET "http://localhost:8080/api/scans/$SCAN_ID_BLOCK/decision" \
  -H "Content-Type: application/json" | jq .
```

**Expected Response (BLOCK):**
```json
{
  "scanId": "...",
  "decision": "BLOCK",
  "riskScore": 10,
  "criticalCount": 1,
  "highCount": 0,
  "mediumCount": 0,
  "lowCount": 0,
  "reason": "Critical findings (1) exceed the configured staging policy limit (0)",
  "createdAt": "2026-10-19T..."
}
```

**HTTP Status:** 200 OK

**Explanation:**
- CRITICAL: 1 > 0 ✗ (FAILED - reason returned)
- Result: BLOCK

---

## Test 11: Error Scenarios

### 11.1 Test: Missing project

```bash
curl -s -X GET "http://localhost:8080/api/scans/00000000-0000-0000-0000-000000000000/decision" \
  -H "Content-Type: application/json" | jq .
```

**Expected Response:**
```json
{
  "errorCode": "NOT_FOUND",
  "message": "Scan not found with id: 00000000-0000-0000-0000-000000000000"
}
```

**HTTP Status:** 404 Not Found

### 11.2 Test: Missing policy

```bash
# Create scan without creating policy first
SCAN_NO_POLICY=$(curl -s -X POST "http://localhost:8080/api/projects/$PROJECT_ID_BLOCK/scans" \
  -H "Content-Type: application/json" \
  -d "{
    \"pipelineId\": \"$PIPELINE_ID_BLOCK\",
    \"environment\": \"DEVELOPMENT\"
  }" | jq -r '.id')

# Try to get decision (policy for DEVELOPMENT not created)
curl -s -X GET "http://localhost:8080/api/scans/$SCAN_NO_POLICY/decision" \
  -H "Content-Type: application/json" | jq .
```

**Expected Response:**
```json
{
  "errorCode": "NOT_FOUND",
  "message": "Policy not found for project ... environment: DEVELOPMENT"
}
```

**HTTP Status:** 404 Not Found

### 11.3 Test: Invalid JSON

```bash
curl -s -X POST "http://localhost:8080/api/projects/$PROJECT_ID/scans" \
  -H "Content-Type: application/json" \
  -d "{ INVALID JSON }" | jq .
```

**Expected Response:**
```json
{
  "errorCode": "VALIDATION_ERROR",
  "message": "Validation failed"
}
```

**HTTP Status:** 400 Bad Request

---

## Test 12: Complete End-to-End Workflow Script

### 12.1 Automated Test Script

Save as `jenkins-integration-test.sh`:

```bash
#!/bin/bash
set -e

BACKEND_URL="http://localhost:8080"
PROJECT_NAME="e2e-test-$(date +%s)"
PIPELINE_JOB="test-pipeline"
BUILD_NUMBER=1

echo "=== Jenkins Integration End-to-End Test ==="
echo "Project: $PROJECT_NAME"
echo "Backend: $BACKEND_URL"
echo

# Step 1: Find or create project
echo "Step 1: Finding or creating project..."
PROJECT_RESPONSE=$(curl -s -X GET "$BACKEND_URL/api/projects?name=$PROJECT_NAME" \
  -H "Content-Type: application/json")

if echo "$PROJECT_RESPONSE" | jq -e '.errorCode' > /dev/null; then
  echo "  → Project not found, creating..."
  PROJECT_RESPONSE=$(curl -s -X POST "$BACKEND_URL/api/projects" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"$PROJECT_NAME\",\"repositoryUrl\":\"https://github.com/test/repo.git\"}")
else
  echo "  → Project found"
fi

PROJECT_ID=$(echo "$PROJECT_RESPONSE" | jq -r '.id')
echo "  ✓ Project ID: $PROJECT_ID"
echo

# Step 2: Create pipeline
echo "Step 2: Creating pipeline..."
PIPELINE_RESPONSE=$(curl -s -X POST "$BACKEND_URL/api/projects/$PROJECT_ID/pipelines" \
  -H "Content-Type: application/json" \
  -d "{\"provider\":\"JENKINS\",\"jobName\":\"$PIPELINE_JOB\",\"buildNumber\":$BUILD_NUMBER,\"branch\":\"main\",\"commitSha\":\"abc123\"}")

PIPELINE_ID=$(echo "$PIPELINE_RESPONSE" | jq -r '.id')
echo "  ✓ Pipeline ID: $PIPELINE_ID"
echo

# Step 3: Create scan
echo "Step 3: Creating scan..."
SCAN_RESPONSE=$(curl -s -X POST "$BACKEND_URL/api/projects/$PROJECT_ID/scans" \
  -H "Content-Type: application/json" \
  -d "{\"pipelineId\":\"$PIPELINE_ID\",\"environment\":\"PRODUCTION\"}")

SCAN_ID=$(echo "$SCAN_RESPONSE" | jq -r '.id')
echo "  ✓ Scan ID: $SCAN_ID"
echo

# Step 4: Create policy
echo "Step 4: Creating policy..."
curl -s -X POST "$BACKEND_URL/api/projects/$PROJECT_ID/policies" \
  -H "Content-Type: application/json" \
  -d "{\"environment\":\"PRODUCTION\",\"maxCritical\":0,\"maxHigh\":5,\"maxRiskScore\":200}" > /dev/null

echo "  ✓ Policy created"
echo

# Step 5: Upload report
echo "Step 5: Uploading Trivy report..."

cat > /tmp/trivy-test.json << 'REPORT_EOF'
{
  "ArtifactType": "container_image",
  "ArtifactName": "test:latest",
  "Results": [{
    "Target": "test:latest",
    "Class": "os-pkgs",
    "Type": "apk",
    "Vulnerabilities": [{
      "VulnerabilityID": "CVE-TEST-001",
      "PkgName": "test-lib",
      "InstalledVersion": "1.0.0",
      "FixedVersion": "1.0.1",
      "Severity": "MEDIUM",
      "Title": "Test vulnerability",
      "Description": "A test vulnerability",
      "PrimaryURL": "https://example.com"
    }]
  }]
}
REPORT_EOF

REPORT_RESPONSE=$(curl -s -X POST "$BACKEND_URL/api/scans/$SCAN_ID/reports?tool=TRIVY" \
  -F "file=@/tmp/trivy-test.json")

REPORT_ID=$(echo "$REPORT_RESPONSE" | jq -r '.id')
echo "  ✓ Report ID: $REPORT_ID"
echo

# Step 6: Process report
echo "Step 6: Processing report..."
PROCESS_RESPONSE=$(curl -s -X POST "$BACKEND_URL/api/scans/$SCAN_ID/reports/$REPORT_ID/process" \
  -H "Content-Type: application/json")

FINDINGS_COUNT=$(echo "$PROCESS_RESPONSE" | jq '.findingsCreated')
echo "  ✓ Findings created: $FINDINGS_COUNT"
echo

# Step 7: Get decision
echo "Step 7: Getting security decision..."
DECISION_RESPONSE=$(curl -s -X GET "$BACKEND_URL/api/scans/$SCAN_ID/decision" \
  -H "Content-Type: application/json")

DECISION=$(echo "$DECISION_RESPONSE" | jq -r '.decision')
REASON=$(echo "$DECISION_RESPONSE" | jq -r '.reason')
echo "  ✓ Decision: $DECISION"
echo "  ✓ Reason: $REASON"
echo

# Step 8: Enforce
echo "Step 8: Enforcing decision..."
if [ "$DECISION" = "PASS" ]; then
  echo "  ✅ Pipeline PASSED - continuing"
  exit 0
elif [ "$DECISION" = "BLOCK" ]; then
  echo "  ❌ Pipeline BLOCKED - stopping"
  exit 1
else
  echo "  ❌ Unknown decision: $DECISION"
  exit 2
fi
```

### 12.2 Run the test script

```bash
chmod +x jenkins-integration-test.sh
./jenkins-integration-test.sh
```

**Expected Output:**
```
=== Jenkins Integration End-to-End Test ===
Project: e2e-test-1666190000
Backend: http://localhost:8080

Step 1: Finding or creating project...
  → Project not found, creating...
  ✓ Project ID: 550e8400-e29b-41d4-a716-446655440000

Step 2: Creating pipeline...
  ✓ Pipeline ID: 660e8400-e29b-41d4-a716-446655440001

Step 3: Creating scan...
  ✓ Scan ID: 770e8400-e29b-41d4-a716-446655440002

Step 4: Creating policy...
  ✓ Policy created

Step 5: Uploading Trivy report...
  ✓ Report ID: 880e8400-e29b-41d4-a716-446655440003

Step 6: Processing report...
  ✓ Findings created: 1

Step 7: Getting security decision...
  ✓ Decision: PASS
  ✓ Reason: Security policy satisfied

Step 8: Enforcing decision...
  ✅ Pipeline PASSED - continuing
```

---

## Troubleshooting

### Issue: Connection refused

**Error:** `curl: (7) Failed to connect to localhost port 8080`

**Solution:** Start backend
```bash
cd secureops
mvn spring-boot:run
```

### Issue: 404 on /api/projects

**Error:** `{"errorCode":"NOT_FOUND"}`

**Solution:** Check project name spelling and URL encoding

### Issue: Multipart upload fails

**Error:** `400 Bad Request`

**Solution:** Ensure file exists and is valid JSON
```bash
jq empty /tmp/trivy-report.json  # Validates JSON
```

### Issue: Database connection error

**Error:** Connection timeout

**Solution:** Start PostgreSQL
```bash
# Linux
sudo service postgresql start

# macOS
brew services start postgresql

# Windows
net start PostgreSQL
```

---

## Success Criteria

✅ All 12 tests pass  
✅ PASS decisions generated for valid policies  
✅ BLOCK decisions generated for policy violations  
✅ Error responses correct (404, 409, 400)  
✅ Duplicate prevention working (409 Conflict)  
✅ Findings extracted from report  
✅ Policies enforced correctly  

---

**Jenkins Integration Testing Ready ✅**

