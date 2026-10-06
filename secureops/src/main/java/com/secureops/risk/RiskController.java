package com.secureops.risk;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/risk/RiskController.java
 * PURPOSE: REST API endpoints for risk score calculation.
 * WHY IT EXISTS: Exposes risk engine functionality through HTTP API.
 * DEPENDENCIES: Uses RiskEngine for calculations.
 * 
 * API ENDPOINTS:
 * GET /api/scans/{scanId}/risk - Calculate and return risk score for a scan
 * 
 * RISK CALCULATION:
 * The risk score is deterministic and based solely on the count of findings by severity:
 * - CRITICAL: 10 points each
 * - HIGH: 7 points each
 * - MEDIUM: 4 points each
 * - LOW: 1 point each
 * 
 * Total Risk Score = (CRITICAL × 10) + (HIGH × 7) + (MEDIUM × 4) + (LOW × 1)
 * 
 * IMPORTANT: This is NOT an industry-standard security score. It is a simple weighted
 * scoring mechanism for relative risk comparison within SecureOps.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class RiskController {

    private final RiskEngine riskEngine;

    /**
     * Calculate risk score for a specific scan.
     * GET /api/scans/{scanId}/risk
     *
     * The risk score is calculated from all findings in the scan, weighted by severity:
     * - Each CRITICAL finding contributes 10 points
     * - Each HIGH finding contributes 7 points
     * - Each MEDIUM finding contributes 4 points
     * - Each LOW finding contributes 1 point
     *
     * @param scanId UUID of the scan
     * @return ResponseEntity with RiskScore and HTTP 200 OK
     * @throws ScanNotFoundException if scan does not exist (converted to HTTP 404)
     *
     * EXAMPLE REQUEST:
     * GET http://localhost:8080/api/scans/e5fcba61-e7f2-4e85-aae3-30888a93401a/risk
     *
     * EXAMPLE RESPONSE (200 OK):
     * {
     *   "scanId": "e5fcba61-e7f2-4e85-aae3-30888a93401a",
     *   "riskScore": 38,
     *   "criticalCount": 2,
     *   "highCount": 2,
     *   "mediumCount": 2,
     *   "lowCount": 0
     * }
     *
     * CALCULATION:
     * (2 × 10) + (2 × 7) + (2 × 4) + (0 × 1) = 20 + 14 + 8 + 0 = 38
     */
    @GetMapping("/scans/{scanId}/risk")
    public ResponseEntity<RiskScore> calculateRiskScore(@PathVariable UUID scanId) {
        log.info("GET /api/scans/{}/risk - Calculating risk score", scanId);

        RiskScore riskScore = riskEngine.calculateRiskScore(scanId);
        log.debug("Risk score calculated for scan: {} = {}", scanId, riskScore.getRiskScore());

        return ResponseEntity.ok(riskScore);
    }

}
