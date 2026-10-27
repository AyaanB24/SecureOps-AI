package com.secureops.project;

import com.secureops.project.dto.CreateProjectRequest;
import com.secureops.project.dto.ProjectResponse;
import com.secureops.policy.PolicyEnvironment;
import com.secureops.policy.PolicyService;
import com.secureops.policy.dto.CreatePolicyRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * FILE: src/main/java/com/secureops/project/ProjectService.java
 * PURPOSE: Business logic for project management.
 * WHY IT EXISTS: Encapsulates CRUD operations and validation; keeps controller thin and testable.
 * DEPENDENCIES: Uses ProjectRepository for database access; converts between entities and DTOs.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final PolicyService policyService;

    /**
     * Create a new project.
     * Auto-creates default security policies for all environments (DEV, STAGING, PROD).
     *
     * @param request CreateProjectRequest with name and optional repositoryUrl
     * @return ProjectResponse containing created project data
     * @throws DuplicateProjectException if a project with the same name already exists
     */
    public ProjectResponse createProject(CreateProjectRequest request) {
        // Check if project with this name already exists
        if (projectRepository.findByName(request.getName()).isPresent()) {
            throw new DuplicateProjectException(request.getName());
        }

        Project project = new Project(request.getName(), request.getRepositoryUrl());
        Project saved = projectRepository.save(project);
        log.info("Project created: {} with name: {}", saved.getId(), saved.getName());

        // Auto-create default policies for all environments
        createDefaultPolicies(saved.getId());

        return ProjectResponse.fromEntity(saved);
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
            // This is a best-effort operation
        }
    }

    /**
     * Get all projects.
     *
     * @return List of ProjectResponse DTOs
     */
    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findAll()
            .stream()
            .map(ProjectResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Get a specific project by ID.
     *
     * @param projectId UUID of the project
     * @return ProjectResponse containing project data
     * @throws ProjectNotFoundException if project does not exist
     */
    public ProjectResponse getProjectById(UUID projectId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ProjectNotFoundException(projectId.toString()));
        return ProjectResponse.fromEntity(project);
    }

    /**
     * Find a project by name.
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
     * Delete a project by ID.
     * This will cascade delete all associated pipelines, scans, reports, findings, and decisions.
     *
     * @param projectId UUID of the project to delete
     * @throws ProjectNotFoundException if project does not exist
     */
    public void deleteProject(UUID projectId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ProjectNotFoundException(projectId.toString()));
        projectRepository.delete(project);
    }

}
