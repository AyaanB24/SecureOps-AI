package com.secureops.policy;

import com.secureops.policy.dto.CreatePolicyRequest;
import com.secureops.policy.dto.PolicyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/policy/PolicyController.java
 * PURPOSE: REST API endpoints for policy management.
 * WHY IT EXISTS: Exposes policy CRUD operations through HTTP API.
 * DEPENDENCIES: Uses PolicyService for business logic.
 * 
 * API ENDPOINTS:
 * POST   /api/projects/{projectId}/policies                      - Create policy
 * GET    /api/projects/{projectId}/policies                      - List all policies for project
 * GET    /api/projects/{projectId}/policies/{environment}        - Get specific policy
 * PUT    /api/projects/{projectId}/policies/{environment}        - Update policy
 * 
 * PROJECT-LEVEL ISOLATION:
 * All policies are scoped to a specific project.
 * Each project can have different thresholds for same environment.
 * Example:
 *   POST /api/projects/payment-api/policies (Production, maxCritical=0)
 *   POST /api/projects/job-portal/policies (Production, maxCritical=1)
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class PolicyController {

    private final PolicyService policyService;

    /**
     * Create a new security policy for a project environment.
     * POST /api/projects/{projectId}/policies
     *
     * @param projectId UUID of the project
     * @param request CreatePolicyRequest with environment and thresholds
     * @return ResponseEntity with PolicyResponse and HTTP 201 Created
     * @throws ProjectNotFoundException if project doesn't exist
     * @throws IllegalArgumentException if validation fails
     */
    @PostMapping("/projects/{projectId}/policies")
    public ResponseEntity<PolicyResponse> createPolicy(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreatePolicyRequest request) {
        log.info("POST /api/projects/{}/policies - Creating policy for environment: {}", projectId, request.getEnvironment());

        PolicyResponse response = policyService.createPolicy(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all policies for a project.
     * GET /api/projects/{projectId}/policies
     *
     * @param projectId UUID of the project
     * @return ResponseEntity with List of PolicyResponse and HTTP 200 OK
     * @throws ProjectNotFoundException if project doesn't exist
     */
    @GetMapping("/projects/{projectId}/policies")
    public ResponseEntity<List<PolicyResponse>> getPoliciesByProject(@PathVariable UUID projectId) {
        log.info("GET /api/projects/{}/policies - Fetching all policies", projectId);

        List<PolicyResponse> policies = policyService.getPoliciesByProject(projectId);
        return ResponseEntity.ok(policies);
    }

    /**
     * Get a specific policy for a project and environment.
     * GET /api/projects/{projectId}/policies/{environment}
     *
     * @param projectId UUID of the project
     * @param environment PolicyEnvironment enum value (DEVELOPMENT, STAGING, PRODUCTION)
     * @return ResponseEntity with PolicyResponse and HTTP 200 OK
     * @throws PolicyNotFoundException if policy doesn't exist
     */
    @GetMapping("/projects/{projectId}/policies/{environment}")
    public ResponseEntity<PolicyResponse> getPolicyByEnvironment(
            @PathVariable UUID projectId,
            @PathVariable String environment) {
        log.info("GET /api/projects/{}/policies/{} - Fetching policy", projectId, environment);

        try {
            PolicyEnvironment env = PolicyEnvironment.valueOf(environment.toUpperCase());
            PolicyResponse response = policyService.getPolicyByProjectAndEnvironment(projectId, env);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid environment: {}", environment);
            throw new IllegalArgumentException("Invalid environment: " + environment + ". Valid values: DEVELOPMENT, STAGING, PRODUCTION");
        }
    }

    /**
     * Update a security policy for a project environment.
     * PUT /api/projects/{projectId}/policies/{environment}
     *
     * @param projectId UUID of the project
     * @param environment PolicyEnvironment enum value
     * @param request CreatePolicyRequest with new threshold values
     * @return ResponseEntity with PolicyResponse and HTTP 200 OK
     * @throws PolicyNotFoundException if policy doesn't exist
     * @throws IllegalArgumentException if validation fails
     */
    @PutMapping("/projects/{projectId}/policies/{environment}")
    public ResponseEntity<PolicyResponse> updatePolicy(
            @PathVariable UUID projectId,
            @PathVariable String environment,
            @Valid @RequestBody CreatePolicyRequest request) {
        log.info("PUT /api/projects/{}/policies/{} - Updating policy", projectId, environment);

        try {
            PolicyEnvironment env = PolicyEnvironment.valueOf(environment.toUpperCase());
            PolicyResponse response = policyService.updatePolicy(projectId, env, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid environment: {}", environment);
            throw new IllegalArgumentException("Invalid environment: " + environment + ". Valid values: DEVELOPMENT, STAGING, PRODUCTION");
        }
    }

}
