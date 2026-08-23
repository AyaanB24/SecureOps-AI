package com.secureops.pipeline;

import com.secureops.pipeline.dto.CreatePipelineRequest;
import com.secureops.pipeline.dto.PipelineResponse;
import com.secureops.project.Project;
import com.secureops.project.ProjectNotFoundException;
import com.secureops.project.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * FILE: src/main/java/com/secureops/pipeline/PipelineService.java
 * PURPOSE: Business logic for pipeline management.
 * WHY IT EXISTS: Encapsulates CRUD operations, validation, and multi-project isolation logic.
 * DEPENDENCIES: Uses PipelineRepository and ProjectRepository for data access.
 * ISOLATION: Ensures pipelines can only be created for valid projects and cannot be moved between projects.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PipelineService {

    private final PipelineRepository pipelineRepository;
    private final ProjectRepository projectRepository;

    /**
     * Create a new pipeline for a specific project.
     * Validates that the project exists and that the pipeline doesn't already exist.
     *
     * @param projectId UUID of the project
     * @param request CreatePipelineRequest containing pipeline details
     * @return PipelineResponse with created pipeline
     * @throws ProjectNotFoundException if project does not exist
     * @throws DuplicatePipelineException if a pipeline with the same job name and build number already exists for this project
     */
    public PipelineResponse createPipeline(UUID projectId, CreatePipelineRequest request) {
        log.info("Creating pipeline for project: {} with job: {}", projectId, request.getJobName());

        // Validate that project exists
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> {
                log.error("Project not found: {}", projectId);
                return new ProjectNotFoundException(projectId.toString());
            });

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
        log.info("Pipeline created with ID: {}", saved.getId());

        return PipelineResponse.fromEntity(saved);
    }

    /**
     * Get all pipelines for a specific project.
     *
     * @param projectId UUID of the project
     * @return List of PipelineResponse objects
     * @throws ProjectNotFoundException if project does not exist
     */
    public List<PipelineResponse> getPipelinesByProject(UUID projectId) {
        log.info("Fetching pipelines for project: {}", projectId);

        // Validate that project exists
        if (!projectRepository.existsById(projectId)) {
            log.error("Project not found: {}", projectId);
            throw new ProjectNotFoundException(projectId.toString());
        }

        List<Pipeline> pipelines = pipelineRepository.findByProjectId(projectId);
        return pipelines.stream()
            .map(PipelineResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Get a specific pipeline by ID.
     *
     * @param pipelineId UUID of the pipeline
     * @return PipelineResponse
     * @throws PipelineNotFoundException if pipeline does not exist
     */
    public PipelineResponse getPipelineById(UUID pipelineId) {
        log.info("Fetching pipeline: {}", pipelineId);

        Pipeline pipeline = pipelineRepository.findById(pipelineId)
            .orElseThrow(() -> {
                log.error("Pipeline not found: {}", pipelineId);
                return new PipelineNotFoundException(pipelineId.toString());
            });

        return PipelineResponse.fromEntity(pipeline);
    }

}
