package com.secureops.risk;

import com.secureops.finding.Finding;
import com.secureops.finding.FindingRepository;
import com.secureops.finding.Severity;
import com.secureops.scan.Scan;
import com.secureops.scan.ScanRepository;
import com.secureops.scan.ScanNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/risk/RiskEngine.java
 * PURPOSE: Calculate deterministic risk scores from normalized findings.
 * WHY IT EXISTS: Provides risk quantification for scans based on finding severity distribution.
 * DEPENDENCIES: Uses FindingRepository and ScanRepository for data access.
 * 
 * RISK CALCULATION LOGIC:
 * 1. Retrieve all findings for a scan
 * 2. Count findings by severity (CRITICAL, HIGH, MEDIUM, LOW)
 * 3. Apply weights: CRITICAL=10, HIGH=7, MEDIUM=4, LOW=1
 * 4. Sum weighted scores: total = (critical×10) + (high×7) + (medium×4) + (low×1)
 * 5. Return RiskScore DTO with all counts and total
 * 
 * DETERMINISM:
 * - Same findings always produce same score
 * - Independent of order or timestamp
 * - Based purely on finding severity distribution
 * - Reproducible across systems
 * 
 * IMPORTANT:
 * This is NOT an industry-standard security score (like CVSS or similar).
 * It is a simple weighted scoring mechanism for relative risk comparison.
 * 
 * EXAMPLE:
 * Scan with findings: CVE-2024-1086 (CRITICAL), CVE-2023-46604 (CRITICAL), CVE-2023-44487 (HIGH)
 * Calculation:
 *   - criticalCount = 2
 *   - highCount = 1
 *   - mediumCount = 0
 *   - lowCount = 0
 *   - riskScore = (2 × 10) + (1 × 7) + (0 × 4) + (0 × 1) = 20 + 7 = 27
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RiskEngine {

    private final FindingRepository findingRepository;
    private final ScanRepository scanRepository;

    /**
     * Calculate risk score for a specific scan.
     *
     * @param scanId UUID of the scan
     * @return RiskScore with total score and severity counts
     * @throws ScanNotFoundException if scan doesn't exist
     */
    public RiskScore calculateRiskScore(UUID scanId) {
        log.info("Calculating risk score for scan: {}", scanId);

        // Validate scan exists
        Scan scan = scanRepository.findById(scanId)
            .orElseThrow(() -> {
                log.error("Scan not found: {}", scanId);
                return new ScanNotFoundException(scanId.toString());
            });
        log.debug("Scan found: {}", scan.getId());

        // Retrieve all findings for scan
        List<Finding> findings = findingRepository.findByScanId(scanId);
        log.debug("Retrieved {} findings for scan: {}", findings.size(), scanId);

        // Count findings by severity
        int criticalCount = 0;
        int highCount = 0;
        int mediumCount = 0;
        int lowCount = 0;

        for (Finding finding : findings) {
            switch (finding.getSeverity()) {
                case CRITICAL:
                    criticalCount++;
                    break;
                case HIGH:
                    highCount++;
                    break;
                case MEDIUM:
                    mediumCount++;
                    break;
                case LOW:
                    lowCount++;
                    break;
                default:
                    log.warn("Unknown severity: {}", finding.getSeverity());
            }
        }

        log.debug("Severity counts - CRITICAL: {}, HIGH: {}, MEDIUM: {}, LOW: {}",
            criticalCount, highCount, mediumCount, lowCount);

        // Calculate weighted risk score
        // CRITICAL = 10, HIGH = 7, MEDIUM = 4, LOW = 1
        int riskScore = calculateScore(criticalCount, highCount, mediumCount, lowCount);
        log.info("Risk score calculated: {} (CRITICAL:{}, HIGH:{}, MEDIUM:{}, LOW:{})",
            riskScore, criticalCount, highCount, mediumCount, lowCount);

        return new RiskScore(scanId, riskScore, criticalCount, highCount, mediumCount, lowCount);
    }

    /**
     * Calculate weighted risk score from severity counts.
     *
     * @param criticalCount Number of CRITICAL findings
     * @param highCount Number of HIGH findings
     * @param mediumCount Number of MEDIUM findings
     * @param lowCount Number of LOW findings
     * @return Total weighted risk score
     */
    private int calculateScore(int criticalCount, int highCount, int mediumCount, int lowCount) {
        return (criticalCount * 10) + (highCount * 7) + (mediumCount * 4) + (lowCount * 1);
    }

}
