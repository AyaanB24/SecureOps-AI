package com.secureops.decision.dto;

import com.secureops.decision.Decision;
import com.secureops.decision.SecurityDecision;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/decision/dto/SecurityDecisionResponse.java
 * PURPOSE: DTO for returning security decision data via REST API.
 * WHY IT EXISTS: Provides structured response for decision endpoints.
 * DEPENDENCIES: Converts SecurityDecision entity to JSON response.
 * 
 * RESPONSE FORMAT:
 * {
 *   "scanId": "scan-uuid",
 *   "decision": "PASS" or "BLOCK",
 *   "riskScore": 38,
 *   "criticalCount": 1,
 *   "highCount": 2,
 *   "mediumCount": 2,
 *   "lowCount": 0,
 *   "reason": "Security policy satisfied",
 *   "createdAt": "2026-10-07T10:30:00"
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SecurityDecisionResponse {

    private UUID scanId;
    private Decision decision;
    private Integer riskScore;
    private Long criticalCount;
    private Long highCount;
    private Long mediumCount;
    private Long lowCount;
    private String reason;
    private LocalDateTime createdAt;

    /**
     * Convert SecurityDecision entity to SecurityDecisionResponse DTO.
     *
     * @param decision SecurityDecision entity
     * @return SecurityDecisionResponse DTO
     */
    public static SecurityDecisionResponse fromEntity(SecurityDecision decision) {
        SecurityDecisionResponse response = new SecurityDecisionResponse();
        response.setScanId(decision.getScan().getId());
        response.setDecision(decision.getDecision());
        response.setRiskScore(decision.getRiskScore());
        response.setCriticalCount(decision.getCriticalCount());
        response.setHighCount(decision.getHighCount());
        response.setMediumCount(decision.getMediumCount());
        response.setLowCount(decision.getLowCount());
        response.setReason(decision.getReason());
        response.setCreatedAt(decision.getCreatedAt());
        return response;
    }

}
