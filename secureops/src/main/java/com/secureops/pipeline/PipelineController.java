package com.secureops.pipeline;

import com.secureops.pipeline.dto.CreatePipelineRequest;
import com.secureops.pipeline.dto.PipelineResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/pipeline/PipelineController.java
 * PURPOSE: REST controller for pipeline management endpoints.
 * WHY IT EXISTS: Exposes pipeline CRUD operations via HTTP; handles request routing and response serialization.
 * DEPENDENCIES: Uses PipelineService for business logic; returns PipelineResponse DTOs.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class PipelineController {

    private final PipelineService pipelineService;

    /**
     * Create a new pipeline for a specific project.
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
        log.info("POST /api/projects/{}/pipelines - Creating pipeline", projectId);
        PipelineResponse response = pipelineService.createPipeline(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all pipelines for a specific project.
     * GET /api/projects/{projectId}/pipelines
     *
     * @param projectId UUID of the project
     * @return ResponseEntity with List of PipelineResponse and HTTP 200 OK
     */
    @GetMapping("/projects/{projectId}/pipelines")
    public ResponseEntity<List<PipelineResponse>> getPipelinesByProject(@PathVariable UUID projectId) {
        log.info("GET /api/projects/{}/pipelines - Fetching all pipelines", projectId);
        List<PipelineResponse> pipelines = pipelineService.getPipelinesByProject(projectId);
        return ResponseEntity.ok(pipelines);
    }

    /**
     * Get a specific pipeline by ID.
     * GET /api/pipelines/{pipelineId}
     *
     * @param pipelineId UUID of the pipeline
     * @return ResponseEntity with PipelineResponse and HTTP 200 OK
     * @throws PipelineNotFoundException if pipeline does not exist (converted to HTTP 404)
     */
    @GetMapping("/pipelines/{pipelineId}")
    public ResponseEntity<PipelineResponse> getPipelineById(@PathVariable UUID pipelineId) {
        log.info("GET /api/pipelines/{} - Fetching pipeline", pipelineId);
        PipelineResponse response = pipelineService.getPipelineById(pipelineId);
        return ResponseEntity.ok(response);
    }

}
