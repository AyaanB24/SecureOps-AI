package com.secureops.pipeline;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/pipeline/PipelineRepository.java
 * PURPOSE: Spring Data JPA repository for Pipeline entity CRUD operations.
 * WHY IT EXISTS: Provides database access layer; handles SQL queries automatically via Spring Data.
 * DEPENDENCIES: Extends JpaRepository for standard CRUD methods. Used by PipelineService.
 */
@Repository
public interface PipelineRepository extends JpaRepository<Pipeline, UUID> {

    /**
     * Find all pipelines belonging to a specific project.
     *
     * @param projectId UUID of the project
     * @return List of pipelines for that project
     */
    List<Pipeline> findByProjectId(UUID projectId);

    /**
     * Find a specific pipeline by project, job name, and build number.
     * Used to check for duplicate pipelines before creation.
     *
     * @param projectId UUID of the project
     * @param jobName Name of the job
     * @param buildNumber Build number
     * @return Optional containing the pipeline if found
     */
    Optional<Pipeline> findByProjectIdAndJobNameAndBuildNumber(UUID projectId, String jobName, Long buildNumber);

}
