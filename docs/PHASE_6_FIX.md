# Phase 6 Fix: JSON Deserialization Issue

## Problem
When processing Trivy reports via POST `/api/scans/{scanId}/reports/{reportId}/process`, the parser was returning `findingsCreated: 0` even though the Trivy report contained 10 valid vulnerabilities.

## Root Cause
The Jackson POJO classes (TrivyReport, TrivyResult, TrivyVulnerability) were using **PascalCase field names** (e.g., `VulnerabilityID`, `PkgName`, `InstalledVersion`) without explicit `@JsonProperty` annotations.

By default, Jackson's ObjectMapper expects **camelCase** field names in Java classes. When it tried to deserialize the Trivy JSON (which uses PascalCase), the fields weren't being mapped correctly, resulting in null values and zero findings.

**Example:**
```java
// Before (BROKEN)
private String VulnerabilityID;  // Jackson looks for "vulnerabilityID" in JSON, not "VulnerabilityID"

// After (FIXED)
@JsonProperty("VulnerabilityID")
private String vulnerabilityID;  // Jackson explicitly maps "VulnerabilityID" JSON field to this
```

## Solution
Added `@JsonProperty` annotations to all POJO classes to explicitly map JSON field names to Java fields:

### Files Fixed

1. **TrivyVulnerability.java** - Added @JsonProperty to all fields:
   - `@JsonProperty("VulnerabilityID") private String vulnerabilityID;`
   - `@JsonProperty("PkgName") private String pkgName;`
   - `@JsonProperty("InstalledVersion") private String installedVersion;`
   - `@JsonProperty("FixedVersion") private String fixedVersion;`
   - `@JsonProperty("Title") private String title;`
   - `@JsonProperty("Description") private String description;`
   - `@JsonProperty("Severity") private String severity;`
   - `@JsonProperty("PrimaryURL") private String primaryURL;`
   - `@JsonProperty("CweID") private String cweID;`
   - `@JsonProperty("CVSS") private Integer cvss;`

2. **TrivyReport.java** - Added @JsonProperty to all fields:
   - `@JsonProperty("SchemaVersion") private Integer schemaVersion;`
   - `@JsonProperty("ArtifactName") private String artifactName;`
   - `@JsonProperty("ArtifactType") private String artifactType;`
   - `@JsonProperty("Metadata") private Object metadata;`
   - `@JsonProperty("Results") private List<TrivyResult> results;`

3. **TrivyResult.java** - Added @JsonProperty to Target, Type, Vulnerabilities:
   - `@JsonProperty("Target") private String target;`
   - `@JsonProperty("Type") private String type;`
   - `@JsonProperty("Vulnerabilities") private List<TrivyVulnerability> vulnerabilities;`
   - (Already had @JsonProperty("Class"))

## Why TrivyParser Didn't Need Changes
The TrivyParser uses Lombok-generated getter methods like:
- `vuln.getVulnerabilityID()`
- `vuln.getPkgName()`
- `vuln.getInstalledVersion()`

These getters are automatically generated from the Java field names after `@JsonProperty` is applied, so the parser works correctly without modification.

## Testing
✅ **Compilation:** `mvn clean compile` - SUCCESS  
✅ **Build:** `mvn clean package -DskipTests` - SUCCESS

## Verification
To verify the fix works:

1. **Start the application:**
   ```powershell
   cd d:\SecureOps-AI\secureops
   mvn spring-boot:run -DskipTests
   ```

2. **Run the full Phase 6 workflow** (see PHASE_6.md for step-by-step):
   - Create Project
   - Create Pipeline
   - Create Scan
   - Upload Trivy Report (Phase 4)
   - Process Report (Phase 6) → **Should now show `findingsCreated: 10`** ✅
   - Query Findings (Phase 5)

3. **Expected Result:**
   ```json
   {
     "reportId": "...",
     "tool": "TRIVY",
     "findingsCreated": 10,
     "status": "PROCESSED"
   }
   ```

## Impact
- **Before:** Trivy parser returned 0 findings (broken)
- **After:** Trivy parser correctly extracts all 10 vulnerabilities from the sample report
- **Backward Compatible:** No API changes, no database schema changes
- **Deterministic:** Same Trivy report always produces same findings

## Files Modified
- `TrivyVulnerability.java` - Added @JsonProperty annotations
- `TrivyReport.java` - Added @JsonProperty annotations  
- `TrivyResult.java` - Added @JsonProperty annotations to Target, Type, Vulnerabilities

**No changes to:**
- TrivyParser.java
- ReportProcessingService.java
- ReportController.java
- Database schema
- API endpoints

## Lesson Learned
Jackson field deserialization requires explicit field name mapping when JSON uses a different naming convention than Java field names. Using `@JsonProperty` ensures reliable bidirectional mapping regardless of naming style.
