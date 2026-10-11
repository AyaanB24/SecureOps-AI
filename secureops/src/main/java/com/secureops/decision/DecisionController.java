package com.secureops.decision;

import com.secureops.decision.dto.SecurityDecisionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/decision/DecisionController.java
 * PURPOSE: REST API endpoints for security decision management.
 * WHY IT EXISTS: Exposes security decision endpoints through HTTP API.
 * DEPENDENCIES: Uses DecisionService for business logic.
 * 
 * API ENDPOINTS:
 * GET /api/scans/{scanId}/decision - Generate and return security decision
 * 
 * DECISION GENERATION FLOW:
 * 1. Load scan (includes project and environment)
 * 2. Load project policy for that environment
 * 3. Count findings by severity
 * 4. Calculate risk score
 * 5. Evaluate against policy thresholds
 * 6. Generate PASS or BLOCK verdict
 * 7. Store decision in database
 * 8. Return SecurityDecisionResponse
 * 
 * DETERMINISTIC:
 * - Same scan data always produces same decision
 * - No AI, no randomness involved
 * - Based purely on policy thresholds
 * - Reproducible across all systems
 * 
 * DECISION OUTCOMES:
 * PASS: All policy thresholds satisfied
 * BLOCK: One or more thresholds exceeded
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class DecisionController {

    private final DecisionService decisionService;

    /**
     * Generate security decision for a scan.
     * GET /api/scans/{scanId}/decision
     *
     * Combines findings, risk scoring, and policy evaluation to produce deterministic PASS/BLOCK verdict.
     *
     * The decision process:
     * 1. Retrieves scan with project and environment info
     * 2. Loads the correct project policy for the scan's environment
     * 3. Counts findings by severity (CRITICAL, HIGH, MEDIUM, LOW)
     * 4. Calculates risk score using Phase 7 engine
     * 5. Evaluates against policy thresholds:
     *    - CRITICAL findings ≤ policy.maxCritical
     *    - HIGH findings ≤ policy.maxHigh
     *    - Risk score ≤ policy.maxRiskScore
     * 6. Returns PASS if all conditions met, BLOCK if any violated
     * 7. Stores decision with full reasoning
     *
     * @param scanId UUID of the scan
     * @return ResponseEntity with SecurityDecisionResponse and HTTP 200 OK
     * @throws ScanNotFoundException if scan doesn't exist
     * @throws PolicyNotFoundException if policy for scan's environment doesn't exist
     *
     * EXAMPLE REQUEST:
     * GET http://localhost:8080/api/scans/e5fcba61-e7f2-4e85-aae3-30888a93401a/decision
     *
     * EXAMPLE RESPONSE (PASS - 200 OK):
     * {
     *   "scanId": "e5fcba61-e7f2-4e85-aae3-30888a93401a",
     *   "decision": "PASS",
     *   "riskScore": 38,
     *   "criticalCount": 0,
     *   "highCount": 1,
     *   "mediumCount": 2,
     *   "lowCount": 0,
     *   "reason": "Security policy satisfied",
     *   "createdAt": "2026-10-07T10:30:00"
     * }
     *
     * EXAMPLE RESPONSE (BLOCK - 200 OK):
     * {
     *   "scanId": "f1a2b3c4-d5e6-4f7g-8h9i-0j1k2l3m4n5o",
     *   "decision": "BLOCK",
     *   "riskScore": 85,
     *   "criticalCount": 1,
     *   "highCount": 2,
     *   "mediumCount": 1,
     *   "lowCount": 0,
     *   "reason": "Critical findings (1) exceed the configured production policy limit (0)",
     *   "createdAt": "2026-10-07T10:35:00"
     * }
     */
    @GetMapping("/scans/{scanId}/decision")
    public ResponseEntity<SecurityDecisionResponse> generateDecision(@PathVariable UUID scanId) {
        log.info("GET /api/scans/{}/decision - Generating security decision", scanId);

        SecurityDecisionResponse response = decisionService.generateDecision(scanId);
        log.debug("Security decision generated for scan: {} decision: {}", scanId, response.getDecision());

        return ResponseEntity.ok(response);
    }

}
