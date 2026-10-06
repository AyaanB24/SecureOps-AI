package com.secureops.policy;

import com.secureops.risk.RiskEngine;
import com.secureops.risk.RiskScore;
import com.secureops.finding.FindingRepository;
import com.secureops.finding.Severity;
import com.secureops.scan.Scan;
import com.secureops.scan.ScanRepository;
import com.secureops.scan.ScanNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/policy/PolicyEngine.java
 * PURPOSE: Evaluate whether a scan meets its project's security policy.
 * WHY IT EXISTS: Determines if a scan is acceptable based on configurable, database-driven policies.
 * DEPENDENCIES: Uses PolicyRepository, FindingRepository, RiskEngine, ScanRepository.
 * 
 * POLICY EVALUATION:
 * 1. Retrieve scan and project
 * 2. Get policy for project + scan environment
 * 3. Count critical and high findings
 * 4. Calculate risk score (Phase 7)
 * 5. Compare against policy thresholds
 * 6. Return evaluation result
 * 
 * EVALUATION RESULT:
 * A scan is ACCEPTED if ALL of the following are true:
 * - CRITICAL findings count ≤ policy.maxCritical
 * - HIGH findings count ≤ policy.maxHigh
 * - Total risk score ≤ policy.maxRiskScore
 * 
 * Otherwise, scan is REJECTED.
 * 
 * DATABASE-DRIVEN:
 * Policies are NOT hardcoded. Each policy is read from PostgreSQL.
 * Different projects can have different policies.
 * Same project can have different policies per environment.
 * 
 * EXAMPLES:
 * Payment API (Production): maxCritical=0, maxHigh=3, maxRiskScore=70
 *   → CRITICAL findings > 0 = REJECT
 *   → HIGH findings > 3 = REJECT
 *   → Risk score > 70 = REJECT
 * 
 * Job Portal (Production): maxCritical=1, maxHigh=5, maxRiskScore=100
 *   → CRITICAL findings > 1 = REJECT
 *   → HIGH findings > 5 = REJECT
 *   → Risk score > 100 = REJECT
 * 
 * Development (any project): maxCritical=10, maxHigh=20, maxRiskScore=300
 *   → More lenient thresholds for development
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyEngine {

    private final PolicyRepository policyRepository;
    private final FindingRepository findingRepository;
    private final RiskEngine riskEngine;
    private final ScanRepository scanRepository;

    /**
     * Evaluate if a scan meets its project's security policy.
     *
     * @param scanId UUID of the scan
     * @return PolicyEvaluation with acceptance decision and detailed breakdown
     * @throws ScanNotFoundException if scan doesn't exist
     * @throws PolicyNotFoundException if policy for scan environment doesn't exist
     */
    public PolicyEvaluation evaluatePolicy(UUID scanId) {
        log.info("Evaluating policy for scan: {}", scanId);

        // Retrieve scan
        Scan scan = scanRepository.findById(scanId)
            .orElseThrow(() -> {
                log.error("Scan not found: {}", scanId);
                return new ScanNotFoundException(scanId.toString());
            });
        log.debug("Scan found: {} environment: {}", scan.getId(), scan.getEnvironment());

        // Retrieve project
        UUID projectId = scan.getPipeline().getProject().getId();
        log.debug("Project ID: {}", projectId);

        // Convert Scan environment to Policy environment (same enum names)
        PolicyEnvironment policyEnvironment = PolicyEnvironment.valueOf(scan.getEnvironment().toString());
        log.debug("Policy environment: {}", policyEnvironment);

        // Retrieve policy for project + environment
        Policy policy = policyRepository.findByProjectIdAndEnvironment(projectId, policyEnvironment)
            .orElseThrow(() -> {
                log.error("Policy not found for project: {} environment: {}", projectId, policyEnvironment);
                return new PolicyNotFoundException("No policy defined for project: " + projectId + " environment: " + policyEnvironment);
            });
        log.debug("Policy found: {} thresholds - CRITICAL: {}, HIGH: {}, RiskScore: {}",
            policy.getId(), policy.getMaxCritical(), policy.getMaxHigh(), policy.getMaxRiskScore());

        // Count findings by severity
        long criticalCount = findingRepository.findByScanId(scanId).stream()
            .filter(f -> f.getSeverity() == Severity.CRITICAL)
            .count();
        long highCount = findingRepository.findByScanId(scanId).stream()
            .filter(f -> f.getSeverity() == Severity.HIGH)
            .count();
        log.debug("Finding counts - CRITICAL: {}, HIGH: {}", criticalCount, highCount);

        // Calculate risk score from Phase 7
        RiskScore riskScore = riskEngine.calculateRiskScore(scanId);
        log.debug("Risk score calculated: {}", riskScore.getRiskScore());

        // Evaluate against policy
        boolean criticalAccepted = criticalCount <= policy.getMaxCritical();
        boolean highAccepted = highCount <= policy.getMaxHigh();
        boolean riskAccepted = riskScore.getRiskScore() <= policy.getMaxRiskScore();

        boolean overallAccepted = criticalAccepted && highAccepted && riskAccepted;
        log.info("Policy evaluation result for scan: {} = {} (CRITICAL: {}, HIGH: {}, Risk: {})",
            scanId, overallAccepted ? "ACCEPTED" : "REJECTED", criticalAccepted, highAccepted, riskAccepted);

        return new PolicyEvaluation(
            scanId,
            projectId,
            policyEnvironment,
            policy.getId(),
            overallAccepted,
            criticalCount,
            policy.getMaxCritical(),
            criticalAccepted,
            highCount,
            policy.getMaxHigh(),
            highAccepted,
            riskScore.getRiskScore(),
            policy.getMaxRiskScore(),
            riskAccepted
        );
    }

}
