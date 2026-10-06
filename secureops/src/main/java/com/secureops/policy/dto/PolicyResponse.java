package com.secureops.policy.dto;

import com.secureops.policy.Policy;
import com.secureops.policy.PolicyEnvironment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/policy/dto/PolicyResponse.java
 * PURPOSE: DTO for returning policy data via REST API.
 * WHY IT EXISTS: Provides structured response for policy endpoints.
 * DEPENDENCIES: Converts Policy entity to JSON response.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PolicyResponse {

    private UUID id;
    private UUID projectId;
    private PolicyEnvironment environment;
    private Integer maxCritical;
    private Integer maxHigh;
    private Integer maxRiskScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Convert Policy entity to PolicyResponse DTO.
     *
     * @param policy Policy entity
     * @return PolicyResponse DTO
     */
    public static PolicyResponse fromEntity(Policy policy) {
        PolicyResponse response = new PolicyResponse();
        response.setId(policy.getId());
        response.setProjectId(policy.getProject().getId());
        response.setEnvironment(policy.getEnvironment());
        response.setMaxCritical(policy.getMaxCritical());
        response.setMaxHigh(policy.getMaxHigh());
        response.setMaxRiskScore(policy.getMaxRiskScore());
        response.setCreatedAt(policy.getCreatedAt());
        response.setUpdatedAt(policy.getUpdatedAt());
        return response;
    }

}
