package com.secureops.project;

import com.secureops.project.dto.CreateProjectRequest;
import com.secureops.project.dto.ProjectResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/project/ProjectController.java
 * PURPOSE: REST controller for project management endpoints.
 * WHY IT EXISTS: Exposes project CRUD operations via HTTP; handles request routing and response serialization.
 * DEPENDENCIES: Uses ProjectService for business logic; returns ProjectResponse DTOs.
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {

    private final ProjectService projectService;

    /**
     * Create a new project.
     * POST /api/projects
     *
     * @param request CreateProjectRequest containing project details
     * @return ResponseEntity with ProjectResponse and HTTP 201 Created
     */
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody CreateProjectRequest request) {
        log.info("Creating new project: {}", request.getName());
        ProjectResponse response = projectService.createProject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all projects.
     * GET /api/projects
     *
     * @return ResponseEntity with List of ProjectResponse and HTTP 200 OK
     */
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        log.info("Fetching all projects");
        List<ProjectResponse> projects = projectService.getAllProjects();
        return ResponseEntity.ok(projects);
    }

    /**
     * Get a specific project by ID.
     * GET /api/projects/{projectId}
     *
     * @param projectId UUID of the project
     * @return ResponseEntity with ProjectResponse and HTTP 200 OK
     * @throws ProjectNotFoundException if project does not exist (converted to HTTP 404)
     */
    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable UUID projectId) {
        log.info("Fetching project: {}", projectId);
        ProjectResponse response = projectService.getProjectById(projectId);
        return ResponseEntity.ok(response);
    }

}
