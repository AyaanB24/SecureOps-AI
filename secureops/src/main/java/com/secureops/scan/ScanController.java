package com.secureops.scan;

import com.secureops.scan.dto.CreateScanRequest;
import com.secureops.scan.dto.ScanResponse;
import com.secureops.common.exception.AccessDeniedException;
import com.secureops.project.Project;
import com.secureops.project.ProjectRepository;
import com.secureops.serviceaccount.ServiceAccount;
import com.secureops.serviceaccount.ServiceAccountRepository;
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
 * FILE: src/main/java/com/secureops/scan/ScanController.java
 * PURPOSE: REST controller for scan management endpoints.
 * WHY IT EXISTS: Exposes scan CRUD operations via HTTP; handles request routing and response serialization.
 * DEPENDENCIES: Uses ScanService for business logic; returns ScanResponse DTOs.
 * SECURITY: 
 * - Validates authentication (JWT or API Key required)
 * - Validates authorization (API key owner must match project owner)
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ScanController {

    private final ScanService scanService;
    private final ProjectRepository projectRepository;
    private final ServiceAccountRepository serviceAccountRepository;

    /**
     * Extract current authenticated entity type from Security Context.
     * Returns either "jwt" (for web users) or "api_key" (for Jenkins/CI/CD)
     */
    private String getAuthType() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return "none";
        }
        
        // Check if authenticated via API Key (has ROLE_SERVICE_ACCOUNT)
        boolean hasServiceAccountRole = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(a -> a.equals("ROLE_SERVICE_ACCOUNT"));
        
        return hasServiceAccountRole ? "api_key" : "jwt";
    }

    /**
     * Get current authenticated user ID (if JWT) or service account owner ID (if API Key)
     * 
     * NOTE: When API key authentication occurs in ApiKeyAuthenticationFilter,
     * the principal is already set to the user ID (account.getCreatedBy()).
     * So we can simply return the principal as-is for both JWT and API key auth.
     */
    private UUID getCurrentAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("User not authenticated");
        }

        String principal = auth.getPrincipal().toString();
        
        // For both JWT and API Key auth, principal is the user ID
        // - JWT: principal set by JwtAuthenticationFilter to user ID
        // - API Key: principal set by ApiKeyAuthenticationFilter to account.getCreatedBy() (user ID)
        return UUID.fromString(principal);
    }

    /**
     * Create a new scan for a specific project.
     * POST /api/projects/{projectId}/scans
     * 
     * Requires authentication (JWT or API Key).
     * If using API Key: service account owner must own the project.
     * If using JWT: user must own the project or be admin.
     *
     * @param projectId UUID of the project
     * @param request CreateScanRequest containing pipeline and environment
     * @return ResponseEntity with ScanResponse and HTTP 201 Created
     */
    @PostMapping("/projects/{projectId}/scans")
    public ResponseEntity<ScanResponse> createScan(
        @PathVariable UUID projectId,
        @Valid @RequestBody CreateScanRequest request) {
        
        log.info("POST /api/projects/{}/scans - Creating scan", projectId);
        
        // Get authenticated user (either JWT user or API key owner)
        UUID authenticatedUserId = getCurrentAuthenticatedUserId();
        String authType = getAuthType();
        
        // Verify project exists
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> {
                log.warn("Project not found: {}", projectId);
                return new com.secureops.project.ProjectNotFoundException(projectId.toString());
            });

        // Verify authorization: user must own the project (or be admin for JWT)
        // For API Key: service account owner must own project
        // For JWT: user must own project or be admin
        boolean hasAccess = false;
        
        if ("api_key".equals(authType)) {
            // API Key: service account owner must own the project
            hasAccess = project.getOwnerId() != null && project.getOwnerId().equals(authenticatedUserId);
            if (!hasAccess) {
                log.warn("Access denied: API key owner {} cannot access project {} (owner: {})", 
                    authenticatedUserId, projectId, project.getOwnerId());
                throw new AccessDeniedException("API key does not have access to this project");
            }
        } else {
            // JWT: user must own project or be admin
            UserRole role = getCurrentUserRole();
            if (role == UserRole.ADMIN) {
                hasAccess = true;
            } else {
                hasAccess = project.getOwnerId() != null && project.getOwnerId().equals(authenticatedUserId);
            }
            if (!hasAccess) {
                log.warn("Access denied: User {} cannot access project {}", authenticatedUserId, projectId);
                throw new AccessDeniedException("You do not have permission to access this project");
            }
        }

        // Create scan (ScanService will validate pipeline belongs to project)
        ScanResponse response = scanService.createScan(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Extract current user role from JWT token or API key authentication.
     * For JWT: role comes from granted authorities (ROLE_ADMIN or ROLE_USER)
     * For API key: role is SERVICE_ACCOUNT (extracted from ROLE_SERVICE_ACCOUNT authority)
     */
    private UserRole getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return UserRole.USER;
        }
        
        // Check for SERVICE_ACCOUNT role (set by ApiKeyAuthenticationFilter)
        boolean isServiceAccount = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(a -> a.equals("ROLE_SERVICE_ACCOUNT"));
        
        if (isServiceAccount) {
            return UserRole.SERVICE_ACCOUNT;
        }
        
        // Extract role from authorities for JWT authentication
        return auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(a -> a.startsWith("ROLE_"))
            .map(a -> a.replace("ROLE_", ""))
            .findFirst()
            .map(roleStr -> {
                try {
                    return UserRole.valueOf(roleStr);
                } catch (IllegalArgumentException e) {
                    return UserRole.USER;
                }
            })
            .orElse(UserRole.USER);
    }

    /**
     * Get all scans for a specific project.
     * GET /api/projects/{projectId}/scans
     *
     * @param projectId UUID of the project
     * @return ResponseEntity with List of ScanResponse and HTTP 200 OK
     */
    @GetMapping("/projects/{projectId}/scans")
    public ResponseEntity<List<ScanResponse>> getScansByProject(@PathVariable UUID projectId) {
        log.info("GET /api/projects/{}/scans - Fetching all scans", projectId);
        List<ScanResponse> scans = scanService.getScansByProject(projectId);
        return ResponseEntity.ok(scans);
    }

    /**
     * Get a specific scan by ID.
     * GET /api/scans/{scanId}
     *
     * @param scanId UUID of the scan
     * @return ResponseEntity with ScanResponse and HTTP 200 OK
     * @throws ScanNotFoundException if scan does not exist (converted to HTTP 404)
     */
    @GetMapping("/scans/{scanId}")
    public ResponseEntity<ScanResponse> getScanById(@PathVariable UUID scanId) {
        log.info("GET /api/scans/{} - Fetching scan", scanId);
        ScanResponse response = scanService.getScanById(scanId);
        return ResponseEntity.ok(response);
    }

}
