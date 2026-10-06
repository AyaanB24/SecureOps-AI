package com.secureops.policy;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/policy/PolicyEvaluation.java
 * PURPOSE: DTO for returning policy evaluation results.
 * WHY IT EXISTS: Provides structured response for policy evaluation endpoint.
 * DEPENDENCIES: None (pure data class).
 * 
 * EVALUATION RESULT:
 * Contains scan evaluation against project's policy.
 * Shows which thresholds were passed/failed.
 * 
 * EXAMPLE:
 * {
 *   "scanId": "...",
 *   "projectId": "...",
 *   "environment": "PRODUCTION",
 *   "policyId": "...",
 *   "accepted": false,
 *   "criticalCount": 2,
 *   "maxCritical": 0,
 *   "criticalAccepted": false,
 *   "highCount": 5,
 *   "maxHigh": 3,
 *   "highAccepted": false,
 *   "riskScore": 85,
 *   "maxRiskScore": 70,
 *   "riskAccepted": false
 * }
 * 
 * INTERPRETATION:
 * "accepted": false → Scan REJECTED (violated multiple thresholds)
 * "criticalAccepted": false → 2 critical findings exceed limit of 0
 * "highAccepted": false → 5 high findings exceed limit of 3
 * "riskAccepted": false → Risk score 85 exceeds limit of 70
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PolicyEvaluation {

    private UUID scanId;
    private UUID projectId;
    private PolicyEnvironment environment;
    private UUID policyId;
    private Boolean accepted;
    
    // CRITICAL findings evaluation
    private Long criticalCount;
    private Integer maxCritical;
    private Boolean criticalAccepted;
    
    // HIGH findings evaluation
    private Long highCount;
    private Integer maxHigh;
    private Boolean highAccepted;
    
    // Risk score evaluation
    private Integer riskScore;
    private Integer maxRiskScore;
    private Boolean riskAccepted;

}
