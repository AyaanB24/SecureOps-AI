package com.secureops.risk;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/risk/RiskScore.java
 * PURPOSE: DTO for returning risk score calculation results.
 * WHY IT EXISTS: Provides structured response for risk engine API.
 * DEPENDENCIES: None (pure data class).
 * 
 * RISK SCORE CALCULATION (Deterministic):
 * - CRITICAL: 10 points each
 * - HIGH: 7 points each
 * - MEDIUM: 4 points each
 * - LOW: 1 point each
 * 
 * Total Risk Score = (criticalCount × 10) + (highCount × 7) + (mediumCount × 4) + (lowCount × 1)
 * 
 * IMPORTANT: This is a simple weighted scoring mechanism, NOT an industry-standard security score.
 * It is deterministic: same findings always produce same score.
 * 
 * EXAMPLE:
 * 1 CRITICAL + 2 HIGH + 2 MEDIUM + 0 LOW
 * = (1 × 10) + (2 × 7) + (2 × 4) + (0 × 1)
 * = 10 + 14 + 8 + 0
 * = 32 points
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RiskScore {

    private UUID scanId;
    private Integer riskScore;
    private Integer criticalCount;
    private Integer highCount;
    private Integer mediumCount;
    private Integer lowCount;

}
