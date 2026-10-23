package com.secureops.project;

import com.secureops.project.dto.CreateProjectRequest;
import com.secureops.project.dto.ProjectResponse;
import lombok.RequiredArgsConstructor;
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
public class ProjectService {

    private final ProjectRepository projectRepository;

    /**
     * Create a new project.
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
        return ProjectResponse.fromEntity(saved);
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

}
