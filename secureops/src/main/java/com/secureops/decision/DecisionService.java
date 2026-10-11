package com.secureops.decision;

import com.secureops.decision.dto.SecurityDecisionResponse;
import com.secureops.finding.FindingRepository;
import com.secureops.finding.Severity;
import com.secureops.policy.Policy;
import com.secureops.policy.PolicyEnvironment;
import com.secureops.policy.PolicyNotFoundException;
import com.secureops.policy.PolicyRepository;
import com.secureops.risk.RiskEngine;
import com.secureops.risk.RiskScore;
import com.secureops.scan.Scan;
import com.secureops.scan.ScanRepository;
import com.secureops.scan.ScanNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/decision/DecisionService.java
 * PURPOSE: Generate deterministic security decisions for scans.
 * WHY IT EXISTS: Combines findings, risk scoring, and policy evaluation into PASS/BLOCK verdict.
 * DEPENDENCIES: Uses ScanRepository, FindingRepository, RiskEngine, PolicyRepository, SecurityDecisionRepository.
 * 
 * DECISION FLOW:
 * 1. Load scan (includes project and environment)
 * 2. Determine project and environment
 * 3. Load project policy for that environment
 * 4. Count findings by severity
 * 5. Calculate risk score (Phase 7)
 * 6. Evaluate policy (Phase 8)
 * 7. Generate PASS or BLOCK with reason
 * 8. Store SecurityDecision
 * 9. Return decision
 * 
 * DETERMINISTIC:
 * - Same findings always produce same decision
 * - No AI, no randomness
 * - Based purely on policy thresholds
 * - Reproducible across systems
 * 
 * DECISION LOGIC:
 * Decision = PASS if ALL conditions met:
 *   criticalCount ≤ policy.maxCritical AND
 *   highCount ≤ policy.maxHigh AND
 *   riskScore ≤ policy.maxRiskScore
 * Otherwise: BLOCK
 * 
 * EXAMPLE PASS:
 * Scan has: 0 CRITICAL, 1 HIGH, riskScore=18
 * Policy: maxCritical=0, maxHigh=3, maxRiskScore=70
 * Evaluation: 0≤0 AND 1≤3 AND 18≤70 → PASS
 * Reason: "Security policy satisfied"
 * 
 * EXAMPLE BLOCK:
 * Scan has: 1 CRITICAL, 2 HIGH, riskScore=85
 * Policy: maxCritical=0, maxHigh=3, maxRiskScore=70
 * Evaluation: 1≤0 (FALSE) → BLOCK
 * Reason: "Critical findings exceed the configured production policy"
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DecisionService {

    private final SecurityDecisionRepository securityDecisionRepository;
    private final ScanRepository scanRepository;
    private final FindingRepository findingRepository;
    private final RiskEngine riskEngine;
    private final PolicyRepository policyRepository;

    /**
     * Generate security decision for a scan.
     * Combines findings, risk scoring, and policy evaluation.
     * If decision already exists, updates it instead of creating a new one.
     *
     * @param scanId UUID of the scan
     * @return SecurityDecisionResponse with PASS or BLOCK verdict
     * @throws ScanNotFoundException if scan doesn't exist
     * @throws PolicyNotFoundException if policy for scan environment doesn't exist
     */
    public SecurityDecisionResponse generateDecision(UUID scanId) {
        log.info("Generating security decision for scan: {}", scanId);

        // Step 1: Load scan
        Scan scan = scanRepository.findById(scanId)
            .orElseThrow(() -> {
                log.error("Scan not found: {}", scanId);
                return new ScanNotFoundException(scanId.toString());
            });
        log.debug("Scan found: {} environment: {}", scan.getId(), scan.getEnvironment());

        // Step 2: Determine project
        UUID projectId = scan.getPipeline().getProject().getId();
        log.debug("Project ID: {}", projectId);

        // Step 3: Determine environment and convert to PolicyEnvironment
        PolicyEnvironment policyEnvironment = PolicyEnvironment.valueOf(scan.getEnvironment().toString());
        log.debug("Policy environment: {}", policyEnvironment);

        // Step 4: Load project policy
        Policy policy = policyRepository.findByProjectIdAndEnvironment(projectId, policyEnvironment)
            .orElseThrow(() -> {
                log.error("Policy not found for project: {} environment: {}", projectId, policyEnvironment);
                return new PolicyNotFoundException("No policy defined for project: " + projectId + " environment: " + policyEnvironment);
            });
        log.debug("Policy found: {} thresholds - CRITICAL: {}, HIGH: {}, RiskScore: {}",
            policy.getId(), policy.getMaxCritical(), policy.getMaxHigh(), policy.getMaxRiskScore());

        // Step 5: Count findings by severity
        long criticalCount = findingRepository.findByScanId(scanId).stream()
            .filter(f -> f.getSeverity() == Severity.CRITICAL)
            .count();
        long highCount = findingRepository.findByScanId(scanId).stream()
            .filter(f -> f.getSeverity() == Severity.HIGH)
            .count();
        long mediumCount = findingRepository.findByScanId(scanId).stream()
            .filter(f -> f.getSeverity() == Severity.MEDIUM)
            .count();
        long lowCount = findingRepository.findByScanId(scanId).stream()
            .filter(f -> f.getSeverity() == Severity.LOW)
            .count();
        log.debug("Finding counts - CRITICAL: {}, HIGH: {}, MEDIUM: {}, LOW: {}",
            criticalCount, highCount, mediumCount, lowCount);

        // Step 6: Calculate risk score
        RiskScore riskScore = riskEngine.calculateRiskScore(scanId);
        log.debug("Risk score calculated: {}", riskScore.getRiskScore());

        // Step 7: Evaluate policy
        boolean criticalAccepted = criticalCount <= policy.getMaxCritical();
        boolean highAccepted = highCount <= policy.getMaxHigh();
        boolean riskAccepted = riskScore.getRiskScore() <= policy.getMaxRiskScore();

        // Step 8: Generate decision
        Decision decision;
        String reason;

        if (criticalAccepted && highAccepted && riskAccepted) {
            decision = Decision.PASS;
            reason = "Security policy satisfied";
            log.info("Decision: PASS - All thresholds met");
        } else {
            decision = Decision.BLOCK;

            // Build specific reason for block
            if (!criticalAccepted) {
                reason = String.format("Critical findings (%d) exceed the configured %s policy limit (%d)",
                    criticalCount, policyEnvironment, policy.getMaxCritical());
            } else if (!highAccepted) {
                reason = String.format("High severity findings (%d) exceed the configured %s policy limit (%d)",
                    highCount, policyEnvironment, policy.getMaxHigh());
            } else {
                reason = String.format("Risk score (%d) exceeds the configured %s policy limit (%d)",
                    riskScore.getRiskScore(), policyEnvironment, policy.getMaxRiskScore());
            }
            log.info("Decision: BLOCK - {}", reason);
        }

        // Step 9: Store or update decision
        SecurityDecision securityDecision = securityDecisionRepository.findByScanId(scanId)
            .orElse(new SecurityDecision(scan, null, null, null, null, null, null, null));
        
        // Update all fields (either for new or existing decision)
        securityDecision.setScan(scan);
        securityDecision.setDecision(decision);
        securityDecision.setRiskScore(riskScore.getRiskScore());
        securityDecision.setCriticalCount(criticalCount);
        securityDecision.setHighCount(highCount);
        securityDecision.setMediumCount(mediumCount);
        securityDecision.setLowCount(lowCount);
        securityDecision.setReason(reason);
        
        // Only set createdAt for new decisions
        if (securityDecision.getCreatedAt() == null) {
            securityDecision.setCreatedAt(LocalDateTime.now());
        }
        
        SecurityDecision saved = securityDecisionRepository.save(securityDecision);
        log.info("Security decision saved: {} decision: {} (update mode: {})", 
            saved.getId(), saved.getDecision(), securityDecision.getId() != null);

        // Step 10: Return response
        return SecurityDecisionResponse.fromEntity(saved);
    }

    /**
     * Get existing security decision for a scan.
     *
     * @param scanId UUID of the scan
     * @return SecurityDecisionResponse
     * @throws SecurityDecisionNotFoundException if decision doesn't exist
     */
    public SecurityDecisionResponse getDecision(UUID scanId) {
        log.info("Fetching security decision for scan: {}", scanId);

        SecurityDecision decision = securityDecisionRepository.findByScanId(scanId)
            .orElseThrow(() -> {
                log.error("Security decision not found for scan: {}", scanId);
                return new SecurityDecisionNotFoundException("No security decision found for scan: " + scanId);
            });

        return SecurityDecisionResponse.fromEntity(decision);
    }

}
