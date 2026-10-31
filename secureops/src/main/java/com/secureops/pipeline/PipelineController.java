package com.secureops.pipeline;

import com.secureops.pipeline.dto.CreatePipelineRequest;
import com.secureops.pipeline.dto.PipelineResponse;
import com.secureops.user.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/pipeline/PipelineController.java
 * PURPOSE: REST controller for pipeline management with transitive access checks.
 * WHY IT EXISTS: Exposes pipeline CRUD operations via HTTP with ownership isolation.
 * DEPENDENCIES: Uses PipelineService for business logic; extracts user from JWT.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class PipelineController {

    private final PipelineService pipelineService;

    /**
     * Extract current user ID from JWT token or API key authentication.
     * For JWT: principal is the user ID (set by JwtAuthenticationFilter)
     * For API key: principal is the user ID (set by ApiKeyAuthenticationFilter to account.getCreatedBy())
     */
    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("User not authenticated");
        }
        try {
            return UUID.fromString(auth.getPrincipal().toString());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid principal format: " + auth.getPrincipal());
        }
    }

    /**
     * Extract current user role from JWT token or API key authentication.
     * For JWT: role comes from granted authorities
     * For API key: defaults to SERVICE_ACCOUNT
     */
    private UserRole getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("User not authenticated");
        }
        
        // Try to extract role from authorities
        UserRole role = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(a -> a.startsWith("ROLE_"))
            .map(a -> a.replace("ROLE_", ""))
            .findFirst()
            .map(roleStr -> {
                try {
                    return UserRole.valueOf(roleStr);
                } catch (IllegalArgumentException e) {
                    // If role doesn't match enum, it might be SERVICE_ACCOUNT
                    if (roleStr.equals("SERVICE_ACCOUNT")) {
                        return UserRole.SERVICE_ACCOUNT;
                    }
                    return UserRole.USER; // Default to USER
                }
            })
            .orElse(UserRole.USER);
        
        return role;
    }

    /**
     * Create a new pipeline for a specific project (with transitive project access check).
     * POST /api/projects/{projectId}/pipelines
     *
     * @param projectId UUID of the project
     * @param request CreatePipelineRequest containing pipeline details
     * @return ResponseEntity with PipelineResponse and HTTP 201 Created
     */
    @PostMapping("/projects/{projectId}/pipelines")
    public ResponseEntity<PipelineResponse> createPipeline(
        @PathVariable UUID projectId,
        @Valid @RequestBody CreatePipelineRequest request) {
        UUID userId = getCurrentUserId();
        UserRole role = getCurrentUserRole();
        
        log.info("Creating pipeline in project: {} by user: {}", projectId, userId);
        PipelineResponse response = pipelineService.createPipeline(projectId, request, userId, role);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all pipelines for a specific project (with transitive project access check).
     * GET /api/projects/{projectId}/pipelines
     *
     * @param projectId UUID of the project
     * @return ResponseEntity with List of PipelineResponse and HTTP 200 OK
     */
    @GetMapping("/projects/{projectId}/pipelines")
    public ResponseEntity<List<PipelineResponse>> getPipelinesByProject(@PathVariable UUID projectId) {
        UUID userId = getCurrentUserId();
        UserRole role = getCurrentUserRole();
        
        log.info("Fetching pipelines for project: {} by user: {}", projectId, userId);
        List<PipelineResponse> pipelines = pipelineService.getPipelinesByProject(projectId, userId, role);
        return ResponseEntity.ok(pipelines);
    }

    /**
     * Get a specific pipeline by ID (with transitive project access check).
     * GET /api/pipelines/{pipelineId}
     *
     * @param pipelineId UUID of the pipeline
     * @return ResponseEntity with PipelineResponse and HTTP 200 OK
     */
    @GetMapping("/pipelines/{pipelineId}")
    public ResponseEntity<PipelineResponse> getPipelineById(@PathVariable UUID pipelineId) {
        UUID userId = getCurrentUserId();
        UserRole role = getCurrentUserRole();
        
        log.info("Fetching pipeline: {} by user: {}", pipelineId, userId);
        PipelineResponse response = pipelineService.getPipelineById(pipelineId, userId, role);
        return ResponseEntity.ok(response);
    }

}
