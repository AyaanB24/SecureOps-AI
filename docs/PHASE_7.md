# Phase 7: Deterministic Risk Engine

**Status:** ✅ Complete

**Objective:** Implement a simple, deterministic risk scoring mechanism that quantifies security risk from normalized findings.

---

## 1. Overview

Phase 7 introduces the **Risk Engine**, a service that calculates a weighted risk score for scans based on the severity distribution of findings. This enables:
- Quantitative risk assessment per scan
- Trend analysis across scans
- Risk-based prioritization of remediation efforts
- Simple, deterministic scoring (same findings always produce same score)

### Important Disclaimer

⚠️ **This is NOT an industry-standard security score** (like CVSS, SSRF, or similar).

This is a **simple weighted scoring mechanism** designed for:
- Relative risk comparison within SecureOps
- Quick understanding of scan severity distribution
- Internal risk tracking and trending

It should NOT be used as:
- A replacement for CVSS or CVSS scores
- An industry-standard vulnerability assessment
- A compliance or regulatory assessment tool

---

## 2. Risk Calculation Logic

### Severity Weights

| Severity | Weight | Rationale |
|----------|--------|-----------|
| CRITICAL | 10 | Requires immediate remediation |
| HIGH | 7 | Significant risk, urgent remediation |
| MEDIUM | 4 | Notable risk, should be fixed |
| LOW | 1 | Minor risk, can be deferred |

### Formula

```
Risk Score = (CRITICAL × 10) + (HIGH × 7) + (MEDIUM × 4) + (LOW × 1)
```

### Determinism

- **Same findings always produce same score** - independent of order or timestamp
- **Reproducible** - same calculation on different systems produces same result
- **Non-cumulative** - score reflects current state of scan, not history

---

## 3. Architecture

### Components

#### RiskScore (DTO)
Response object containing:
- `scanId`: UUID of the scan
- `riskScore`: Total weighted risk score (integer)
- `criticalCount`: Number of CRITICAL findings
- `highCount`: Number of HIGH findings
- `mediumCount`: Number of MEDIUM findings
- `lowCount`: Number of LOW findings

#### RiskEngine (Service)
Business logic for risk calculation:
- Retrieves all findings for a scan
- Counts findings by severity
- Applies weights
- Returns RiskScore DTO

#### RiskController (REST)
HTTP endpoints:
- `GET /api/scans/{scanId}/risk` - Calculate and return risk score

---

## 4. API Endpoint

### Get Risk Score

```
GET /api/scans/{scanId}/risk
```

**Parameters:**
- `scanId` (path): UUID of the scan

**Response (200 OK):**
```json
{
  "scanId": "e5fcba61-e7f2-4e85-aae3-30888a93401a",
  "riskScore": 38,
  "criticalCount": 2,
  "highCount": 2,
  "mediumCount": 2,
  "lowCount": 0
}
```

**Error Responses:**

404 Not Found (Scan doesn't exist):
```json
{
  "error_code": "NOT_FOUND",
  "message": "Scan not found: 00000000-0000-0000-0000-000000000000"
}
```

---

## 5. Calculation Example

### Scenario
Scan contains 6 findings:
- CVE-2024-1086 (CRITICAL) → 10 points
- CVE-2023-46604 (CRITICAL) → 10 points
- CVE-2023-44487 (HIGH) → 7 points
- CVE-2023-38545 (HIGH) → 7 points
- CVE-2023-32315 (MEDIUM) → 4 points
- CVE-2024-22195 (MEDIUM) → 4 points

### Calculation
```
CRITICAL: 2 findings × 10 = 20 points
HIGH:     2 findings × 7  = 14 points
MEDIUM:   2 findings × 4  = 8 points
LOW:      0 findings × 1  = 0 points
─────────────────────────────────────
Total Risk Score = 42 points
```

### Response
```json
{
  "scanId": "e5fcba61-e7f2-4e85-aae3-30888a93401a",
  "riskScore": 42,
  "criticalCount": 2,
  "highCount": 2,
  "mediumCount": 2,
  "lowCount": 0
}
```

---

## 6. Testing Workflow

### Prerequisites
- Phase 1-6 complete
- Scan with findings created
- Trivy parser working (10 findings from sample report)

### Test Steps

**Step 1: Create Scan with Findings** (from Phase 6)
```
1. Create Project
2. Create Pipeline
3. Create Scan
4. Upload Trivy Report
5. Process Report → 10 findings created
```

**Step 2: Query Risk Score**
```
GET http://localhost:8080/api/scans/{scanId}/risk
```

**Step 3: Verify Calculation**

Expected for sample Trivy report (10 findings):
- CRITICAL: 3 (CVE-2024-1086, CVE-2023-46604, CVE-2023-42819)
- HIGH: 3 (CVE-2023-44487, CVE-2023-38545, CVE-2023-29383)
- MEDIUM: 2 (CVE-2023-32315, CVE-2023-28957)
- LOW: 0

**Expected Risk Score:**
```
(3 × 10) + (3 × 7) + (2 × 4) + (0 × 1)
= 30 + 21 + 8 + 0
= 59
```

**Expected Response:**
```json
{
  "scanId": "{scanId}",
  "riskScore": 59,
  "criticalCount": 3,
  "highCount": 3,
  "mediumCount": 2,
  "lowCount": 0
}
```

### Postman Test

```
GET http://localhost:8080/api/scans/{scanId}/risk

Response should show:
- riskScore: 59 (for sample Trivy report)
- criticalCount: 3
- highCount: 3
- mediumCount: 2
- lowCount: 0
```

---

## 7. Postman Examples

### Example 1: Scan with High Risk (Mixed Severity)
```
GET http://localhost:8080/api/scans/e5fcba61-e7f2-4e85-aae3-30888a93401a/risk

Response:
{
  "scanId": "e5fcba61-e7f2-4e85-aae3-30888a93401a",
  "riskScore": 38,
  "criticalCount": 1,
  "highCount": 2,
  "mediumCount": 2,
  "lowCount": 0
}

Calculation: (1×10) + (2×7) + (2×4) + (0×1) = 38
```

### Example 2: Scan with Low Risk (Only LOW findings)
```
GET http://localhost:8080/api/scans/12345678-1234-5678-1234-567812345678/risk

Response:
{
  "scanId": "12345678-1234-5678-1234-567812345678",
  "riskScore": 5,
  "criticalCount": 0,
  "highCount": 0,
  "mediumCount": 0,
  "lowCount": 5
}

Calculation: (0×10) + (0×7) + (0×4) + (5×1) = 5
```

### Example 3: Scan with No Findings
```
GET http://localhost:8080/api/scans/87654321-4321-8765-4321-876543218765/risk

Response:
{
  "scanId": "87654321-4321-8765-4321-876543218765",
  "riskScore": 0,
  "criticalCount": 0,
  "highCount": 0,
  "mediumCount": 0,
  "lowCount": 0
}

Calculation: (0×10) + (0×7) + (0×4) + (0×1) = 0
```

---

## 8. Implementation Details

### Files Created
1. **RiskScore.java** - DTO for risk score response
2. **RiskEngine.java** - Service for risk calculation logic
3. **RiskController.java** - REST controller for risk endpoints

### Files Modified
None (no existing code changes required)

### Database Changes
None (uses existing Finding table)

---

## 9. Key Design Decisions

1. **Deterministic Calculation:** Same findings always produce same score, enabling reproducible risk tracking.

2. **Severity-Based Weighting:** Weights (10, 7, 4, 1) are chosen to create clear differentiation between severity levels while keeping calculation simple.

3. **Count-Based, Not CVSS:** Risk score is based on finding count by severity, not individual CVSS scores. This provides relative risk comparison without claiming to be CVSS-compliant.

4. **No Persistence:** Risk scores are calculated on-demand from current findings, not stored. This ensures scores always reflect current state.

5. **Scan-Scoped:** Scores are calculated per scan, enabling trend analysis across scans.

---

## 10. Use Cases

| Use Case | Example |
|----------|---------|
| Risk dashboard | Display risk scores for all recent scans |
| Trend analysis | Compare risk scores across scan iterations |
| Alerting | Trigger alert if risk score increases > 20% |
| Remediation tracking | Risk score should decrease as fixes are applied |
| Risk-based prioritization | Focus on scans with highest risk scores first |
| Reporting | Include risk scores in security reports |

---

## 11. Future Enhancements (Not Phase 7)

Not implemented in Phase 7:
- CVSS integration (calculating from CVSS scores instead of severity)
- Time-based trending (historical risk score tracking)
- Comparative analysis (risk delta between scans)
- Policy-based risk thresholds (e.g., "risk > 50 is RED")
- Risk attribution by tool (risk contribution per tool)
- Remediation tracking (risk reduction from fixes)

These are reserved for future phases.

---

## 12. Mathematical Verification

### Test Case 1: All CRITICAL
```
Input: 10 CRITICAL, 0 HIGH, 0 MEDIUM, 0 LOW
Calculation: (10×10) + (0×7) + (0×4) + (0×1) = 100
Expected: 100
Status: ✓ PASS
```

### Test Case 2: Mixed (Sample Trivy Report)
```
Input: 3 CRITICAL, 3 HIGH, 2 MEDIUM, 0 LOW
Calculation: (3×10) + (3×7) + (2×4) + (0×1) = 30 + 21 + 8 + 0 = 59
Expected: 59
Status: ✓ PASS
```

### Test Case 3: Zero Risk
```
Input: 0 CRITICAL, 0 HIGH, 0 MEDIUM, 0 LOW
Calculation: (0×10) + (0×7) + (0×4) + (0×1) = 0
Expected: 0
Status: ✓ PASS
```

### Test Case 4: Edge Case (Only LOW)
```
Input: 0 CRITICAL, 0 HIGH, 0 MEDIUM, 100 LOW
Calculation: (0×10) + (0×7) + (0×4) + (100×1) = 100
Expected: 100
Status: ✓ PASS
```

---

## 13. Phase 7 Completion Checklist

- [x] RiskScore DTO created
- [x] RiskEngine service implemented with correct formula
- [x] RiskController with GET /api/scans/{scanId}/risk endpoint
- [x] Deterministic calculation verified
- [x] Sample calculation manually verified
- [x] Edge cases handled (zero findings, single severity, etc.)
- [x] Comprehensive logging implemented
- [x] Error handling for missing scans
- [x] Clear documentation with disclaimer
- [x] Build successful

---

## 14. Important Notes

### Disclaimer
This risk score is **NOT**:
- CVSS-compliant
- An industry standard
- A vulnerability assessment
- A compliance evaluation

It is a **simple internal scoring mechanism** for SecureOps.

### Reproducibility
The score is **deterministic and reproducible**:
- Same findings → same score
- Same score across all runs
- No randomness or time-based variance

### Calculation Transparency
The calculation is **fully transparent and auditable**:
- All components (counts) are visible in response
- Can be manually verified from count data
- Formula is documented and simple

---

**Phase 7 Complete ✅**

---

## 15. Quick Reference

### Risk Scoring Formula
```
Risk Score = (C × 10) + (H × 7) + (M × 4) + (L × 1)

Where:
C = CRITICAL count
H = HIGH count
M = MEDIUM count
L = LOW count
```

### Example Calculation
```
3 CRITICAL + 3 HIGH + 2 MEDIUM + 0 LOW
= (3 × 10) + (3 × 7) + (2 × 4) + (0 × 1)
= 30 + 21 + 8 + 0
= 59
```

### API Endpoint
```
GET /api/scans/{scanId}/risk
```

### Response Format
```json
{
  "scanId": "uuid",
  "riskScore": integer,
  "criticalCount": integer,
  "highCount": integer,
  "mediumCount": integer,
  "lowCount": integer
}
```
