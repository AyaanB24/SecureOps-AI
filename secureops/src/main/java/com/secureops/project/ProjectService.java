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
     */
    public ProjectResponse createProject(CreateProjectRequest request) {
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

}
