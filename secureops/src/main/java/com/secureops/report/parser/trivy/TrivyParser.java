package com.secureops.report.parser.trivy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secureops.finding.Finding;
import com.secureops.finding.FindingStatus;
import com.secureops.finding.Severity;
import com.secureops.finding.FingerprintService;
import com.secureops.report.parser.SecurityReportParser;
import com.secureops.report.parser.ReportParsingException;
import com.secureops.scan.Scan;
import com.secureops.scan.ScanRepository;
import com.secureops.report.Report;
import com.secureops.report.ReportRepository;
import com.secureops.report.ReportStatus;
import com.secureops.report.ReportTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * FILE: src/main/java/com/secureops/report/parser/trivy/TrivyParser.java
 * PURPOSE: Parse Trivy JSON vulnerability reports into normalized findings.
 * WHY IT EXISTS: Converts Trivy's tool-specific output into SecureOps Finding model.
 * DEPENDENCIES: Uses FingerprintService for fingerprint generation, Jackson for JSON parsing.
 * 
 * FIELD MAPPING:
 * Trivy VulnerabilityID → Finding.ruleId
 * Trivy PkgName → Finding.packageName
 * Trivy InstalledVersion → Finding.packageVersion
 * Trivy Title → Finding.title
 * Trivy Description → Finding.description (fallback to Title if missing)
 * Trivy Severity → Finding.severity (with validation/normalization)
 * Trivy PrimaryURL → Finding.description (append as reference)
 * 
 * EDGE CASES HANDLED:
 * - Empty Results array: Returns empty findings list (no crash)
 * - Missing Vulnerabilities: Skipped gracefully
 * - Unknown Severity: Defaults to "LOW" with warning
 * - Missing optional fields: Handled with null-safe checks
 * - Malformed JSON: Throws ReportParsingException (not RuntimeException)
 * - Duplicate vulnerabilities: Same fingerprint detected
 * - Multiple targets: All processed in single parse call
 * - Multiple vulnerabilities per result: All extracted
 */
@Component
@Slf4j
public class TrivyParser implements SecurityReportParser {

    private final FingerprintService fingerprintService;
    private final ScanRepository scanRepository;
    private final ReportRepository reportRepository;
    private final ObjectMapper objectMapper;

    public TrivyParser(FingerprintService fingerprintService, ScanRepository scanRepository, ReportRepository reportRepository) {
        this.fingerprintService = fingerprintService;
        this.scanRepository = scanRepository;
        this.reportRepository = reportRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public boolean canParse(String reportContent) {
        try {
            TrivyReport report = objectMapper.readValue(reportContent, TrivyReport.class);
            return report.getResults() != null;
        } catch (Exception e) {
            log.debug("Content is not a Trivy report", e);
            return false;
        }
    }

    @Override
    public List<Finding> parse(String reportContent, UUID scanId, UUID reportId) throws IOException {
        log.info("Parsing Trivy report for scan: {}, report: {}", scanId, reportId);

        List<Finding> findings = new ArrayList<>();

        try {
            // Parse JSON into Trivy model
            TrivyReport trivyReport = objectMapper.readValue(reportContent, TrivyReport.class);
            log.debug("Successfully deserialized Trivy JSON");
            
            // Retrieve entities
            Scan scan = scanRepository.findById(scanId)
                .orElseThrow(() -> {
                    log.error("Scan not found during parsing: {}", scanId);
                    return new ReportParsingException("Scan not found: " + scanId);
                });
            log.debug("Scan entity retrieved");
            
            Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> {
                    log.error("Report not found during parsing: {}", reportId);
                    return new ReportParsingException("Report not found: " + reportId);
                });
            log.debug("Report entity retrieved");

            // Handle empty results
            if (trivyReport.getResults() == null || trivyReport.getResults().isEmpty()) {
                log.info("No results found in Trivy report");
                return findings;
            }

            log.info("Processing {} results from Trivy report", trivyReport.getResults().size());

            // Process each result (target)
            for (TrivyResult result : trivyReport.getResults()) {
                if (result == null || result.getVulnerabilities() == null) {
                    log.debug("Skipping result with no vulnerabilities");
                    continue;
                }

                log.debug("Processing target: {}", result.getTarget());

                // Process each vulnerability in result
                for (TrivyVulnerability vuln : result.getVulnerabilities()) {
                    try {
                        Finding finding = convertToFinding(vuln, scan, report);
                        findings.add(finding);
                        log.debug("Created finding from Trivy vulnerability: {}", vuln.getVulnerabilityID());
                    } catch (Exception e) {
                        log.warn("Failed to convert vulnerability {}: {}", 
                            vuln != null && vuln.getVulnerabilityID() != null ? vuln.getVulnerabilityID() : "unknown", 
                            e.getMessage(), e);
                        // Don't crash - continue processing other vulnerabilities
                    }
                }
            }

            log.info("Trivy report parsed successfully. Found {} vulnerabilities", findings.size());
            return findings;

        } catch (IOException e) {
            log.error("Failed to parse Trivy report as JSON", e);
            throw new ReportParsingException("Invalid Trivy JSON format: " + e.getMessage(), e);
        } catch (ReportParsingException e) {
            // Re-throw parsing exceptions
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error parsing Trivy report", e);
            throw new ReportParsingException("Error parsing Trivy report: " + e.getMessage(), e);
        }
    }

    /**
     * Convert Trivy vulnerability to Finding entity.
     * 
     * @param vuln Trivy vulnerability
     * @param scan Scan entity
     * @param report Report entity
     * @return Finding entity with all fields normalized
     */
    private Finding convertToFinding(TrivyVulnerability vuln, Scan scan, Report report) {
        // Extract and normalize fields
        String ruleId = vuln.getVulnerabilityID() != null ? vuln.getVulnerabilityID() : "UNKNOWN";
        String packageName = vuln.getPkgName();
        String packageVersion = vuln.getInstalledVersion();
        String title = vuln.getTitle() != null ? vuln.getTitle() : ruleId;
        
        // Build description with URL reference if available
        String description = vuln.getDescription();
        if (description == null || description.isBlank()) {
            description = "Vulnerability " + ruleId;
        }
        if (vuln.getPrimaryURL() != null && !vuln.getPrimaryURL().isBlank()) {
            description += "\n\nReference: " + vuln.getPrimaryURL();
        }

        // Normalize severity
        Severity severity = normalizeSeverity(vuln.getSeverity());

        // Generate fingerprint
        String fingerprint = fingerprintService.generateFingerprint(
            "TRIVY", ruleId, null, null, packageName
        );

        // Create Finding entity
        Finding finding = new Finding(
            scan,
            report,
            "TRIVY",
            ruleId,
            title,
            description,
            severity,
            null,  // filePath (Trivy dependency reports don't have source locations)
            null,  // lineNumber
            packageName,
            packageVersion,
            fingerprint
        );

        return finding;
    }

    /**
     * Normalize Trivy severity to Finding severity enum.
     * 
     * @param trivySeverity Trivy severity string (e.g., "CRITICAL", "HIGH", "UNKNOWN")
     * @return Normalized Severity enum
     */
    private Severity normalizeSeverity(String trivySeverity) {
        if (trivySeverity == null || trivySeverity.isBlank()) {
            log.warn("Missing severity in Trivy vulnerability, defaulting to LOW");
            return Severity.LOW;
        }

        try {
            return Severity.valueOf(trivySeverity.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown Trivy severity: {}, defaulting to LOW", trivySeverity);
            return Severity.LOW;
        }
    }

}
