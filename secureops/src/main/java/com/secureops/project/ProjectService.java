package com.secureops.project;

import com.secureops.project.dto.CreateProjectRequest;
import com.secureops.project.dto.ProjectResponse;
import com.secureops.policy.PolicyEnvironment;
import com.secureops.policy.PolicyService;
import com.secureops.policy.dto.CreatePolicyRequest;
import com.secureops.common.exception.AccessDeniedException;
import com.secureops.user.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * FILE: src/main/java/com/secureops/project/ProjectService.java
 * PURPOSE: Business logic for project management with authorization checks.
 * WHY IT EXISTS: Encapsulates CRUD operations, validation, and ownership verification.
 * DEPENDENCIES: Uses ProjectRepository for database access; enforces RBAC via service layer.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final PolicyService policyService;

    /**
     * Get all projects accessible by user (with authorization check).
     * - ADMIN: All projects
     * - USER: Own projects + system projects (owner_id IS NULL)
     * - SERVICE_ACCOUNT: System projects only (for CI/CD tools via API key)
     *
     * @param userId Current user ID (from JWT or API key)
     * @param role Current user role (from JWT or API key)
     * @return List of accessible projects
     */
    public List<ProjectResponse> getAllProjects(UUID userId, UserRole role) {
        List<Project> projects;
        
        if (role == UserRole.ADMIN) {
            // Admins see all projects
            projects = projectRepository.findAll();
        } else if (role == UserRole.SERVICE_ACCOUNT) {
            // Service accounts (Jenkins/CI) see only system projects (owner_id IS NULL)
            // Query: SELECT p FROM Project p WHERE p.ownerId IS NULL
            projects = projectRepository.findAccessibleProjects(UUID.randomUUID())
                .stream()
                .filter(p -> p.getOwnerId() == null)
                .collect(Collectors.toList());
        } else {
            // Users see: own projects + system projects
            projects = projectRepository.findAccessibleProjects(userId);
        }
        
        return projects.stream()
            .map(ProjectResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Get single project with ownership verification (prevents IDOR).
     *
     * @param projectId Project UUID
     * @param userId Current user ID (from JWT)
     * @param role Current user role (from JWT)
     * @return ProjectResponse if authorized
     * @throws ProjectNotFoundException if not found
     * @throws AccessDeniedException if user lacks permission
     */
    public ProjectResponse getProjectById(UUID projectId, UUID userId, UserRole role) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ProjectNotFoundException(projectId.toString()));
        
        // Check access
        if (!canAccessProject(project, userId, role)) {
            throw new AccessDeniedException("You do not have permission to access this project");
        }
        
        return ProjectResponse.fromEntity(project);
    }

    /**
     * Create a new project with ownership assignment.
     * If created by SERVICE_ACCOUNT (via API key), project is a system project (owner_id IS NULL).
     * If created by regular USER, project is owned by that user.
     * Auto-creates default security policies for all environments (DEV, STAGING, PROD).
     *
     * @param request CreateProjectRequest with name and optional repositoryUrl
     * @param userId Current user/service account ID
     * @param role Current user role (USER or SERVICE_ACCOUNT)
     * @return ProjectResponse containing created project data
     * @throws DuplicateProjectException if a project with the same name already exists
     */
    public ProjectResponse createProject(CreateProjectRequest request, UUID userId, UserRole role) {
        // Check if project with this name already exists
        if (projectRepository.findByName(request.getName()).isPresent()) {
            throw new DuplicateProjectException(request.getName());
        }

        Project project = new Project(request.getName(), request.getRepositoryUrl());
        
        // If created via API key (SERVICE_ACCOUNT), make it a system project (owner_id = NULL)
        // Otherwise assign ownership to the user
        if (role == UserRole.SERVICE_ACCOUNT) {
            project.setOwnerId(null); // System project - accessible to all via API key
            log.info("System project created: {} (via API key) for service account: {}", project.getName(), userId);
        } else {
            project.setOwnerId(userId); // User-owned project
            log.info("Project created: {} with name: {} owned by user: {}", project.getId(), project.getName(), userId);
        }
        
        Project saved = projectRepository.save(project);

        // Auto-create default policies for all environments
        createDefaultPolicies(saved.getId());

        return ProjectResponse.fromEntity(saved);
    }
    
    /**
     * Legacy overload for backward compatibility (assumes USER role).
     */
    public ProjectResponse createProject(CreateProjectRequest request, UUID userId) {
        return createProject(request, userId, UserRole.USER);
    }

    /**
     * Delete project with ownership verification.
     * - Project owner can delete their own projects
     * - ADMIN can delete any project
     * - System projects (owner_id IS NULL) can be deleted by any authenticated user
     *
     * @param projectId Project UUID
     * @param userId Current user ID (from JWT)
     * @param role Current user role (from JWT)
     * @throws ProjectNotFoundException if not found
     * @throws AccessDeniedException if user is not owner and not admin and not system project
     */
    public void deleteProject(UUID projectId, UUID userId, UserRole role) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ProjectNotFoundException(projectId.toString()));
        
        // Allow deletion if:
        // 1. User is the project owner
        if (isProjectOwner(project, userId)) {
            projectRepository.delete(project);
            log.info("Project deleted: {} by owner: {}", projectId, userId);
            return;
        }
        
        // 2. User is an admin
        if (role == UserRole.ADMIN) {
            projectRepository.delete(project);
            log.info("Project deleted: {} by admin: {}", projectId, userId);
            return;
        }
        
        // 3. System project (owner_id IS NULL) - allow any authenticated user to delete
        if (project.getOwnerId() == null) {
            projectRepository.delete(project);
            log.info("System project deleted: {} by user: {}", projectId, userId);
            return;
        }
        
        // Otherwise deny access
        throw new AccessDeniedException("Only project owner or admin can delete this project");
    }

    /**
     * Find a project by name (for Jenkins integration).
     * Used by Jenkins to identify existing projects before creating new ones.
     *
     * @param name Name of the project to find
     * @return ProjectResponse containing project data
     * @throws ProjectNotFoundException if project does not exist
     */
    public ProjectResponse getProjectByName(String name) {
        Project project = projectRepository.findByName(name)
            .orElseThrow(() -> new ProjectNotFoundException("Project not found with name: " + name));
        return ProjectResponse.fromEntity(project);
    }

    /**
     * Helper: Can user access this project?
     * - ADMIN: Always yes
     * - USER: Yes if owner or system project (owner_id IS NULL)
     * - SERVICE_ACCOUNT: Only system projects (owner_id IS NULL) - for Jenkins/CI integration
     */
    private boolean canAccessProject(Project project, UUID userId, UserRole role) {
        // Admins can access all projects
        if (role == UserRole.ADMIN) {
            return true;
        }
        
        // Service accounts can only access system projects (for Jenkins/CI)
        if (role == UserRole.SERVICE_ACCOUNT) {
            return project.getOwnerId() == null;
        }
        
        // Users can access: own projects + system projects (owner_id IS NULL)
        if (isProjectOwner(project, userId)) {
            return true;
        }
        
        if (project.getOwnerId() == null) {
            return true; // System project accessible to all users
        }
        
        return false;
    }

    /**
     * Helper: Is this user the project owner?
     */
    private boolean isProjectOwner(Project project, UUID userId) {
        return project.getOwnerId() != null && project.getOwnerId().equals(userId);
    }

    /**
     * Auto-create default policies for all environments when a project is created.
     * This ensures that every project has policies for DEVELOPMENT, STAGING, and PRODUCTION environments.
     *
     * Default Thresholds:
     * - DEVELOPMENT: More lenient (allows higher vulnerability counts)
     * - STAGING: Medium thresholds (balance between security and development)
     * - PRODUCTION: Strictest (minimal vulnerabilities allowed)
     *
     * @param projectId UUID of the newly created project
     */
    private void createDefaultPolicies(UUID projectId) {
        log.info("Auto-creating default policies for project: {}", projectId);

        try {
            // DEVELOPMENT: Lenient policies (Development environment allows higher counts)
            CreatePolicyRequest devPolicy = new CreatePolicyRequest();
            devPolicy.setEnvironment(PolicyEnvironment.DEVELOPMENT);
            devPolicy.setMaxCritical(5);      // Up to 5 critical findings
            devPolicy.setMaxHigh(10);         // Up to 10 high findings
            devPolicy.setMaxRiskScore(100);   // Risk score up to 100
            policyService.createPolicy(projectId, devPolicy);
            log.debug("DEVELOPMENT policy created for project: {}", projectId);

            // STAGING: Medium policies (Staging should be more restrictive)
            CreatePolicyRequest stagingPolicy = new CreatePolicyRequest();
            stagingPolicy.setEnvironment(PolicyEnvironment.STAGING);
            stagingPolicy.setMaxCritical(1);  // Max 1 critical finding
            stagingPolicy.setMaxHigh(5);      // Max 5 high findings
            stagingPolicy.setMaxRiskScore(50); // Risk score up to 50
            policyService.createPolicy(projectId, stagingPolicy);
            log.debug("STAGING policy created for project: {}", projectId);

            // PRODUCTION: Strict policies (Production requires highest security)
            CreatePolicyRequest prodPolicy = new CreatePolicyRequest();
            prodPolicy.setEnvironment(PolicyEnvironment.PRODUCTION);
            prodPolicy.setMaxCritical(0);     // Zero critical findings allowed
            prodPolicy.setMaxHigh(1);         // Max 1 high finding
            prodPolicy.setMaxRiskScore(30);   // Risk score up to 30
            policyService.createPolicy(projectId, prodPolicy);
            log.debug("PRODUCTION policy created for project: {}", projectId);

            log.info("All default policies created successfully for project: {}", projectId);
        } catch (Exception e) {
            log.error("Error creating default policies for project: {}", projectId, e);
            // Don't fail project creation if policy creation fails, but log the error
        }
    }

}

