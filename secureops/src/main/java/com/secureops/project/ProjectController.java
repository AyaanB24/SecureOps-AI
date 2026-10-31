package com.secureops.project;

import com.secureops.project.dto.CreateProjectRequest;
import com.secureops.project.dto.ProjectResponse;
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
 * FILE: src/main/java/com/secureops/project/ProjectController.java
 * PURPOSE: REST controller for project management with authorization.
 * WHY IT EXISTS: Exposes project CRUD operations via HTTP with ownership verification.
 * DEPENDENCIES: Uses ProjectService for business logic; extracts user from JWT.
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {

    private final ProjectService projectService;

    /**
     * Extract current user ID from JWT token or API key authentication.
     * For JWT: principal is the user ID (set by JwtAuthenticationFilter)
     * For API key: principal is the user ID (set by ApiKeyAuthenticationFilter to account.getCreatedBy())
     * Called by all endpoints requiring authorization.
     */
    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("User not authenticated");
        }
        // Principal is set by JWT filter to user ID or by API key filter to service account ID
        try {
            return UUID.fromString(auth.getPrincipal().toString());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid principal format: " + auth.getPrincipal());
        }
    }

    /**
     * Extract current user role from authentication (JWT token or API key).
     * For JWT: role comes from granted authorities (ROLE_ADMIN or ROLE_USER)
     * For API key: role is SERVICE_ACCOUNT (extracted from ROLE_SERVICE_ACCOUNT authority)
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
     * Create a new project with ownership assignment.
     * POST /api/projects
     *
     * @param request CreateProjectRequest containing project details
     * @return ResponseEntity with ProjectResponse and HTTP 201 Created
     */
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody CreateProjectRequest request) {
        UUID userId = getCurrentUserId();
        UserRole role = getCurrentUserRole();
        log.info("Creating new project: {} by user: {} with role: {}", request.getName(), userId, role);
        ProjectResponse response = projectService.createProject(request, userId, role);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all projects or find by name (with authorization).
     * GET /api/projects - returns accessible projects (user-scoped or all if admin)
     * GET /api/projects?name={projectName} - returns specific project by name (Jenkins integration)
     *
     * @param name Optional name parameter. If provided, searches for project by name.
     * @return ResponseEntity with ProjectResponse (single) or List of ProjectResponse (all)
     */
    @GetMapping
    public ResponseEntity<?> getProjects(@RequestParam(value = "name", required = false) String name) {
        UUID userId = getCurrentUserId();
        UserRole role = getCurrentUserRole();
        
        if (name != null && !name.isEmpty()) {
            // Search by name (for Jenkins integration - returns system projects)
            log.info("Fetching project by name: {} for user: {}", name, userId);
            ProjectResponse response = projectService.getProjectByName(name);
            return ResponseEntity.ok(response);
        } else {
            // Return accessible projects (user-scoped or all if admin)
            log.info("Fetching all projects for user: {} with role: {}", userId, role);
            List<ProjectResponse> projects = projectService.getAllProjects(userId, role);
            return ResponseEntity.ok(projects);
        }
    }

    /**
     * Get a specific project by ID with ownership verification (prevents IDOR).
     * GET /api/projects/{projectId}
     *
     * @param projectId UUID of the project
     * @return ResponseEntity with ProjectResponse and HTTP 200 OK
     * @throws ProjectNotFoundException if project does not exist
     * @throws AccessDeniedException if user lacks permission
     */
    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable UUID projectId) {
        UUID userId = getCurrentUserId();
        UserRole role = getCurrentUserRole();
        
        log.info("Fetching project: {} for user: {} with role: {}", projectId, userId, role);
        ProjectResponse response = projectService.getProjectById(projectId, userId, role);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a project with ownership verification (owner or admin only).
     * DELETE /api/projects/{projectId}
     *
     * @param projectId UUID of the project to delete
     * @return ResponseEntity with HTTP 204 No Content
     * @throws ProjectNotFoundException if project does not exist
     * @throws AccessDeniedException if user is not owner or admin
     */
    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable UUID projectId) {
        UUID userId = getCurrentUserId();
        UserRole role = getCurrentUserRole();
        
        log.info("Deleting project: {} requested by user: {} with role: {}", projectId, userId, role);
        projectService.deleteProject(projectId, userId, role);
        log.info("Project deleted successfully: {} by user: {}", projectId, userId);
        return ResponseEntity.noContent().build();
    }

}
