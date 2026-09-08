package com.secureops.finding;

import com.secureops.scan.Scan;
import com.secureops.report.Report;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/finding/FindingService.java
 * PURPOSE: Business logic for finding management and validation.
 * WHY IT EXISTS: Enforces finding creation rules and provides retrieval operations.
 * DEPENDENCIES: Uses FindingRepository, FingerprintService, and entities.
 * 
 * FINDING CREATION WORKFLOW:
 * 1. Validate report belongs to the requested scan (security validation)
 * 2. Generate deterministic fingerprint from stable properties
 * 3. Create and persist finding entity
 * 4. Return FindingResponse DTO
 * 
 * SECURITY VALIDATION:
 * Before creating a finding, verify:
 * - Scan exists in database
 * - Report exists in database
 * - Report.scanId == Scan.id (report belongs to scan - critical for multi-tenancy)
 * Prevents accidental or malicious cross-project finding creation.
 */
@Service
@Slf4j
public class FindingService {

    private final FindingRepository findingRepository;
    private final FingerprintService fingerprintService;

    public FindingService(FindingRepository findingRepository, FingerprintService fingerprintService) {
        this.findingRepository = findingRepository;
        this.fingerprintService = fingerprintService;
    }

    /**
     * Create a new finding with validation.
     * 
     * @param scan The scan entity (must exist)
     * @param report The report entity (must exist and belong to scan)
     * @param tool Security tool name (TRIVY, SEMGREP, etc.)
     * @param ruleId Rule or CVE identifier
     * @param title Finding title
     * @param description Detailed description
     * @param severity Finding severity level
     * @param filePath Source file path (optional)
     * @param lineNumber Source line number (optional)
     * @param packageName Package/library name (optional)
     * @param packageVersion Package version (optional)
     * @return Created finding entity with generated ID
     * @throws IllegalArgumentException if report doesn't belong to scan
     */
    public Finding createFinding(Scan scan, Report report, String tool, String ruleId,
                                String title, String description, Severity severity,
                                String filePath, Integer lineNumber, String packageName,
                                String packageVersion) {
        log.info("Creating finding for scan: {}, report: {}, tool: {}", scan.getId(), report.getId(), tool);

        // CRITICAL SECURITY CHECK: Verify report belongs to scan (cross-project prevention)
        if (!report.getScan().getId().equals(scan.getId())) {
            log.error("Security validation failed: report {} doesn't belong to scan {}", report.getId(), scan.getId());
            throw new IllegalArgumentException(
                "Report does not belong to the specified scan. Cross-project finding creation prevented.");
        }

        // Generate deterministic fingerprint
        String fingerprint = fingerprintService.generateFingerprint(tool, ruleId, filePath, lineNumber, packageName);
        log.debug("Generated fingerprint for finding: {}", fingerprint);

        // Create finding entity
        Finding finding = new Finding(scan, report, tool, ruleId, title, description, severity,
                                     filePath, lineNumber, packageName, packageVersion, fingerprint);

        // Persist to database
        Finding savedFinding = findingRepository.save(finding);
        log.info("Finding created successfully with id: {}", savedFinding.getId());

        return savedFinding;
    }

    /**
     * Get all findings for a specific scan.
     * 
     * @param scanId UUID of the scan
     * @return List of findings for the scan
     */
    public List<Finding> getFindingsByScan(UUID scanId) {
        log.debug("Retrieving findings for scan: {}", scanId);
        return findingRepository.findByScanId(scanId);
    }

    /**
     * Get a specific finding by ID.
     * 
     * @param findingId UUID of the finding
     * @return Finding entity
     * @throws FindingNotFoundException if finding not found
     */
    public Finding getFindingById(UUID findingId) {
        log.debug("Retrieving finding by id: {}", findingId);
        return findingRepository.findById(findingId)
            .orElseThrow(() -> {
                log.warn("Finding not found: {}", findingId);
                return new FindingNotFoundException("Finding not found with id: " + findingId);
            });
    }

    /**
     * Get findings by report and severity.
     * 
     * @param reportId UUID of the report
     * @param severity Severity level to filter
     * @return List of findings with specified severity from report
     */
    public List<Finding> getFindingsByReportAndSeverity(UUID reportId, Severity severity) {
        log.debug("Retrieving findings for report: {} with severity: {}", reportId, severity);
        return findingRepository.findByReportIdAndSeverity(reportId, severity);
    }

    /**
     * Resolve a finding (mark as RESOLVED).
     * 
     * @param findingId UUID of the finding
     * @return Updated finding
     * @throws FindingNotFoundException if finding not found
     */
    public Finding resolveFinding(UUID findingId) {
        log.info("Resolving finding: {}", findingId);
        Finding finding = getFindingById(findingId);
        finding.setStatus(FindingStatus.RESOLVED);
        Finding updated = findingRepository.save(finding);
        log.info("Finding resolved: {}", findingId);
        return updated;
    }

}
