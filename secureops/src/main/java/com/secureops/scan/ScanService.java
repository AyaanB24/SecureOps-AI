package com.secureops.scan;

import com.secureops.scan.dto.CreateScanRequest;
import com.secureops.scan.dto.ScanResponse;
import com.secureops.project.Project;
import com.secureops.project.ProjectNotFoundException;
import com.secureops.project.ProjectRepository;
import com.secureops.pipeline.Pipeline;
import com.secureops.pipeline.PipelineNotFoundException;
import com.secureops.pipeline.PipelineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * FILE: src/main/java/com/secureops/scan/ScanService.java
 * PURPOSE: Business logic for scan management.
 * WHY IT EXISTS: Encapsulates CRUD operations, validation, and multi-level isolation logic.
 * DEPENDENCIES: Uses ScanRepository, PipelineRepository, and ProjectRepository for data access.
 * ISOLATION: Ensures scans can only be created for valid pipelines that belong to the specified project.
 * SECURITY: Critical validation prevents accidental cross-project scan attachment.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScanService {

    private final ScanRepository scanRepository;
    private final PipelineRepository pipelineRepository;
    private final ProjectRepository projectRepository;

    /**
     * Create a new scan for a specific project and pipeline.
     * Validates that:
     * 1. The project exists
     * 2. The pipeline exists
     * 3. The pipeline belongs to the specified project (CRITICAL for isolation)
     *
     * @param projectId UUID of the project
     * @param request CreateScanRequest containing pipeline and environment
     * @return ScanResponse with created scan
     * @throws ProjectNotFoundException if project does not exist
     * @throws PipelineNotFoundException if pipeline does not exist
     * @throws IllegalArgumentException if pipeline does not belong to the project
     */
    public ScanResponse createScan(UUID projectId, CreateScanRequest request) {
        log.info("Creating scan for project: {} with pipeline: {}", projectId, request.getPipelineId());

        // Step 1: Validate project exists
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> {
                log.error("Project not found: {}", projectId);
                return new ProjectNotFoundException(projectId.toString());
            });

        // Step 2: Validate pipeline exists
        Pipeline pipeline = pipelineRepository.findById(request.getPipelineId())
            .orElseThrow(() -> {
                log.error("Pipeline not found: {}", request.getPipelineId());
                return new PipelineNotFoundException(request.getPipelineId().toString());
            });

        // Step 3: CRITICAL VALIDATION - Ensure pipeline belongs to the specified project
        if (!pipeline.getProject().getId().equals(projectId)) {
            log.error("Security violation: Attempted to attach pipeline {} from different project to project {}", 
                request.getPipelineId(), projectId);
            throw new IllegalArgumentException(
                "Pipeline " + request.getPipelineId() + " does not belong to project " + projectId
            );
        }

        // Step 4: Check for duplicate scan (one scan per pipeline-project-environment combination)
        if (scanRepository.findByProjectIdAndPipelineIdAndEnvironment(projectId, request.getPipelineId(), request.getEnvironment()).isPresent()) {
            log.warn("Duplicate scan: project={}, pipeline={}, environment={}", projectId, request.getPipelineId(), request.getEnvironment());
            throw new IllegalArgumentException(
                "A scan already exists for this pipeline in " + request.getEnvironment() + " environment"
            );
        }

        // Step 5: Create and save scan
        Scan scan = new Scan(project, pipeline, request.getEnvironment());
        Scan saved = scanRepository.save(scan);
        log.info("Scan created with ID: {} for project: {}", saved.getId(), projectId);

        return ScanResponse.fromEntity(saved);
    }

    /**
     * Get all scans for a specific project.
     *
     * @param projectId UUID of the project
     * @return List of ScanResponse objects
     * @throws ProjectNotFoundException if project does not exist
     */
    public List<ScanResponse> getScansByProject(UUID projectId) {
        log.info("Fetching scans for project: {}", projectId);

        // Validate that project exists
        if (!projectRepository.existsById(projectId)) {
            log.error("Project not found: {}", projectId);
            throw new ProjectNotFoundException(projectId.toString());
        }

        List<Scan> scans = scanRepository.findByProjectId(projectId);
        return scans.stream()
            .map(ScanResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Get a specific scan by ID.
     *
     * @param scanId UUID of the scan
     * @return ScanResponse
     * @throws ScanNotFoundException if scan does not exist
     */
    public ScanResponse getScanById(UUID scanId) {
        log.info("Fetching scan: {}", scanId);

        Scan scan = scanRepository.findById(scanId)
            .orElseThrow(() -> {
                log.error("Scan not found: {}", scanId);
                return new ScanNotFoundException(scanId.toString());
            });

        return ScanResponse.fromEntity(scan);
    }

}
