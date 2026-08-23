package com.secureops.scan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/scan/ScanRepository.java
 * PURPOSE: Spring Data JPA repository for Scan entity CRUD operations.
 * WHY IT EXISTS: Provides database access layer; handles SQL queries automatically via Spring Data.
 * DEPENDENCIES: Extends JpaRepository for standard CRUD methods. Used by ScanService.
 */
@Repository
public interface ScanRepository extends JpaRepository<Scan, UUID> {

    /**
     * Find all scans belonging to a specific project.
     *
     * @param projectId UUID of the project
     * @return List of scans for that project
     */
    List<Scan> findByProjectId(UUID projectId);

    /**
     * Find a scan by project ID, pipeline ID, and environment (prevents duplicate scans per environment).
     *
     * @param projectId UUID of the project
     * @param pipelineId UUID of the pipeline
     * @param environment Deployment environment
     * @return Optional containing the scan if found
     */
    Optional<Scan> findByProjectIdAndPipelineIdAndEnvironment(UUID projectId, UUID pipelineId, Environment environment);

}
