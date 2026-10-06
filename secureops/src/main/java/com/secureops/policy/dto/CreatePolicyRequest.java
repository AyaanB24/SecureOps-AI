package com.secureops.policy.dto;

import com.secureops.policy.PolicyEnvironment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

/**
 * FILE: src/main/java/com/secureops/policy/dto/CreatePolicyRequest.java
 * PURPOSE: DTO for receiving policy creation requests from REST API clients.
 * WHY IT EXISTS: Provides structured input validation for policy creation endpoint.
 * DEPENDENCIES: Used by PolicyController to parse and validate request body.
 * 
 * REQUIRED FIELDS:
 * - environment: PolicyEnvironment enum (DEVELOPMENT, STAGING, PRODUCTION)
 * - maxCritical: Maximum allowed CRITICAL findings (≥ 0)
 * - maxHigh: Maximum allowed HIGH findings (≥ 0)
 * - maxRiskScore: Maximum allowed risk score from Phase 7 (≥ 0)
 * 
 * VALIDATION:
 * - All fields required
 * - All thresholds must be non-negative
 * - No null values allowed
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePolicyRequest {

    @NotNull(message = "Environment is required")
    private PolicyEnvironment environment;

    @NotNull(message = "Max critical is required")
    @Min(value = 0, message = "Max critical must be non-negative")
    private Integer maxCritical;

    @NotNull(message = "Max high is required")
    @Min(value = 0, message = "Max high must be non-negative")
    private Integer maxHigh;

    @NotNull(message = "Max risk score is required")
    @Min(value = 0, message = "Max risk score must be non-negative")
    private Integer maxRiskScore;

}
