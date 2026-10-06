package com.secureops.policy;

import com.secureops.policy.dto.CreatePolicyRequest;
import com.secureops.policy.dto.PolicyResponse;
import com.secureops.project.Project;
import com.secureops.project.ProjectRepository;
import com.secureops.project.ProjectNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * FILE: src/main/java/com/secureops/policy/PolicyService.java
 * PURPOSE: Business logic for policy management.
 * WHY IT EXISTS: Encapsulates CRUD operations, validation, and policy enforcement.
 * DEPENDENCIES: Uses PolicyRepository and ProjectRepository for data access.
 * 
 * POLICY VALIDATION:
 * - Project must exist
 * - Environment must be valid enum
 * - Thresholds must be non-negative
 * - One policy per environment per project (enforced by unique constraint)
 * 
 * PROJECT-LEVEL ISOLATION:
 * Each policy is scoped to a project.
 * Different projects can have different thresholds.
 * Example:
 *   Payment API (Production): maxCritical=0
 *   Job Portal (Production): maxCritical=1
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final ProjectRepository projectRepository;

    /**
     * Create a new policy for a project and environment.
     *
     * @param projectId UUID of the project
     * @param request CreatePolicyRequest with threshold values
     * @return PolicyResponse with created policy
     * @throws ProjectNotFoundException if project doesn't exist
     * @throws IllegalArgumentException if validation fails
     */
    public PolicyResponse createPolicy(UUID projectId, CreatePolicyRequest request) {
        log.info("Creating policy for project: {} environment: {}", projectId, request.getEnvironment());

        // Validate project exists
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> {
                log.error("Project not found: {}", projectId);
                return new ProjectNotFoundException(projectId.toString());
            });
        log.debug("Project found: {}", project.getId());

        // Validate thresholds are non-negative
        if (request.getMaxCritical() < 0) {
            log.error("maxCritical is negative: {}", request.getMaxCritical());
            throw new IllegalArgumentException("maxCritical must be non-negative");
        }
        if (request.getMaxHigh() < 0) {
            log.error("maxHigh is negative: {}", request.getMaxHigh());
            throw new IllegalArgumentException("maxHigh must be non-negative");
        }
        if (request.getMaxRiskScore() < 0) {
            log.error("maxRiskScore is negative: {}", request.getMaxRiskScore());
            throw new IllegalArgumentException("maxRiskScore must be non-negative");
        }
        log.debug("Threshold validation passed");

        // Create and save policy
        Policy policy = new Policy(project, request.getEnvironment(), 
            request.getMaxCritical(), request.getMaxHigh(), request.getMaxRiskScore());
        Policy saved = policyRepository.save(policy);
        log.info("Policy created: {} for project: {} environment: {}", saved.getId(), projectId, request.getEnvironment());

        return PolicyResponse.fromEntity(saved);
    }

    /**
     * Get all policies for a project.
     *
     * @param projectId UUID of the project
     * @return List of PolicyResponse objects
     * @throws ProjectNotFoundException if project doesn't exist
     */
    public List<PolicyResponse> getPoliciesByProject(UUID projectId) {
        log.info("Fetching policies for project: {}", projectId);

        // Validate project exists
        if (!projectRepository.existsById(projectId)) {
            log.error("Project not found: {}", projectId);
            throw new ProjectNotFoundException(projectId.toString());
        }

        List<Policy> policies = policyRepository.findByProjectId(projectId);
        log.debug("Found {} policies for project: {}", policies.size(), projectId);

        return policies.stream()
            .map(PolicyResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Get policy for a specific project and environment.
     *
     * @param projectId UUID of the project
     * @param environment PolicyEnvironment enum
     * @return PolicyResponse
     * @throws PolicyNotFoundException if policy doesn't exist
     */
    public PolicyResponse getPolicyByProjectAndEnvironment(UUID projectId, PolicyEnvironment environment) {
        log.info("Fetching policy for project: {} environment: {}", projectId, environment);

        Policy policy = policyRepository.findByProjectIdAndEnvironment(projectId, environment)
            .orElseThrow(() -> {
                log.error("Policy not found for project: {} environment: {}", projectId, environment);
                return new PolicyNotFoundException("Policy not found for project: " + projectId + " environment: " + environment);
            });
        log.debug("Policy found: {}", policy.getId());

        return PolicyResponse.fromEntity(policy);
    }

    /**
     * Update a policy for a project and environment.
     *
     * @param projectId UUID of the project
     * @param environment PolicyEnvironment enum
     * @param request UpdatePolicyRequest with new threshold values
     * @return PolicyResponse with updated policy
     * @throws PolicyNotFoundException if policy doesn't exist
     * @throws IllegalArgumentException if validation fails
     */
    public PolicyResponse updatePolicy(UUID projectId, PolicyEnvironment environment, CreatePolicyRequest request) {
        log.info("Updating policy for project: {} environment: {}", projectId, environment);

        // Validate thresholds are non-negative
        if (request.getMaxCritical() < 0 || request.getMaxHigh() < 0 || request.getMaxRiskScore() < 0) {
            log.error("Invalid threshold values in update request");
            throw new IllegalArgumentException("All thresholds must be non-negative");
        }

        // Get existing policy
        Policy policy = policyRepository.findByProjectIdAndEnvironment(projectId, environment)
            .orElseThrow(() -> {
                log.error("Policy not found for project: {} environment: {}", projectId, environment);
                return new PolicyNotFoundException("Policy not found");
            });

        // Update fields
        policy.setMaxCritical(request.getMaxCritical());
        policy.setMaxHigh(request.getMaxHigh());
        policy.setMaxRiskScore(request.getMaxRiskScore());
        policy.setUpdatedAt(LocalDateTime.now());

        // Save and return
        Policy updated = policyRepository.save(policy);
        log.info("Policy updated: {} for project: {} environment: {}", updated.getId(), projectId, environment);

        return PolicyResponse.fromEntity(updated);
    }

}
