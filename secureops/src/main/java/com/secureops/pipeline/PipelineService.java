package com.secureops.pipeline;

import com.secureops.pipeline.dto.CreatePipelineRequest;
import com.secureops.pipeline.dto.PipelineResponse;
import com.secureops.project.Project;
import com.secureops.project.ProjectNotFoundException;
import com.secureops.project.ProjectRepository;
import com.secureops.user.UserRole;
import com.secureops.common.exception.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * FILE: src/main/java/com/secureops/pipeline/PipelineService.java
 * PURPOSE: Business logic for pipeline management with transitive project access checks.
 * WHY IT EXISTS: Encapsulates CRUD operations and enforces ownership isolation via transitive checks.
 * DEPENDENCIES: Uses PipelineRepository and ProjectRepository for data access.
 * ISOLATION: Ensures users can only access pipelines if they have project access.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PipelineService {

    private final PipelineRepository pipelineRepository;
    private final ProjectRepository projectRepository;

    /**
     * Create a new pipeline for a specific project (with project access verification).
     *
     * @param projectId UUID of the project
     * @param request CreatePipelineRequest containing pipeline details
     * @param userId Current user ID (from JWT)
     * @param role Current user role (from JWT)
     * @return PipelineResponse with created pipeline
     * @throws ProjectNotFoundException if project does not exist
     * @throws AccessDeniedException if user lacks project access
     * @throws DuplicatePipelineException if duplicate pipeline exists
     */
    public PipelineResponse createPipeline(UUID projectId, CreatePipelineRequest request, UUID userId, UserRole role) {
        log.info("Creating pipeline for project: {} by user: {}", projectId, userId);

        // Verify project access (transitive security)
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ProjectNotFoundException(projectId.toString()));
        
        if (!canAccessProject(project, userId, role)) {
            throw new AccessDeniedException("You do not have permission to access this project");
        }

        // Check if pipeline with same job name and build number already exists for this project
        if (pipelineRepository.findByProjectIdAndJobNameAndBuildNumber(projectId, request.getJobName(), request.getBuildNumber()).isPresent()) {
            log.error("Duplicate pipeline for project {}: job={}, buildNumber={}", projectId, request.getJobName(), request.getBuildNumber());
            throw new DuplicatePipelineException(request.getJobName(), request.getBuildNumber());
        }

        // Create pipeline with explicit project reference
        Pipeline pipeline = new Pipeline(
            project,
            request.getProvider(),
            request.getJobName(),
            request.getBuildNumber(),
            request.getBranch(),
            request.getCommitSha()
        );

        Pipeline saved = pipelineRepository.save(pipeline);
        log.info("Pipeline created with ID: {} in project: {}", saved.getId(), projectId);

        return PipelineResponse.fromEntity(saved);
    }

    /**
     * Get all pipelines for a specific project (with transitive project access check).
     *
     * @param projectId UUID of the project
     * @param userId Current user ID (from JWT)
     * @param role Current user role (from JWT)
     * @return List of PipelineResponse objects
     * @throws ProjectNotFoundException if project does not exist
     * @throws AccessDeniedException if user lacks project access
     */
    public List<PipelineResponse> getPipelinesByProject(UUID projectId, UUID userId, UserRole role) {
        log.info("Fetching pipelines for project: {} by user: {}", projectId, userId);

        // Verify project access first (transitive security)
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ProjectNotFoundException(projectId.toString()));
        
        if (!canAccessProject(project, userId, role)) {
            throw new AccessDeniedException("You do not have permission to access this project");
        }

        List<Pipeline> pipelines = pipelineRepository.findByProjectId(projectId);
        return pipelines.stream()
            .map(PipelineResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Get a specific pipeline by ID (with transitive project access check).
     *
     * @param pipelineId UUID of the pipeline
     * @param userId Current user ID (from JWT)
     * @param role Current user role (from JWT)
     * @return PipelineResponse
     * @throws PipelineNotFoundException if pipeline does not exist
     * @throws AccessDeniedException if user lacks project access
     */
    public PipelineResponse getPipelineById(UUID pipelineId, UUID userId, UserRole role) {
        log.info("Fetching pipeline: {} by user: {}", pipelineId, userId);

        Pipeline pipeline = pipelineRepository.findById(pipelineId)
            .orElseThrow(() -> new PipelineNotFoundException(pipelineId.toString()));

        // Verify project access transitively
        Project project = pipeline.getProject();
        if (!canAccessProject(project, userId, role)) {
            throw new AccessDeniedException("You do not have permission to access this pipeline");
        }

        return PipelineResponse.fromEntity(pipeline);
    }

    /**
     * Helper: Can user access this project?
     * - ADMIN: Always yes
     * - USER: Yes if owner
     * - SERVICE_ACCOUNT: Yes if system project OR if project was created by same user
     *   (Service accounts are scoped to a specific user and can access that user's projects)
     */
    private boolean canAccessProject(Project project, UUID userId, UserRole role) {
        if (role == UserRole.ADMIN) {
            return true;
        }
        
        // System projects accessible to all
        if (project.getOwnerId() == null) {
            return true;
        }
        
        // Owner can always access their own projects
        if (project.getOwnerId().equals(userId)) {
            return true;
        }
        
        // SERVICE_ACCOUNT can access projects if owned by the same user
        // (service account ID maps to user ID for credential scoping)
        if (role == UserRole.SERVICE_ACCOUNT) {
            return project.getOwnerId().equals(userId);
        }
        
        return false;
    }

}
