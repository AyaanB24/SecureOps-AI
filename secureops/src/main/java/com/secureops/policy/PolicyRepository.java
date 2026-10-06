package com.secureops.policy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/policy/PolicyRepository.java
 * PURPOSE: Spring Data JPA repository for Policy CRUD operations.
 * WHY IT EXISTS: Provides data access layer for policy entities.
 * DEPENDENCIES: Extends JpaRepository for automatic CRUD implementation.
 * 
 * CUSTOM QUERIES:
 * - findByProjectId: Get all policies for a project
 * - findByProjectIdAndEnvironment: Get specific policy for project+environment
 */
@Repository
public interface PolicyRepository extends JpaRepository<Policy, UUID> {

    /**
     * Find all policies for a specific project.
     *
     * @param projectId UUID of the project
     * @return List of policies for the project
     */
    List<Policy> findByProjectId(UUID projectId);

    /**
     * Find a specific policy by project and environment.
     *
     * @param projectId UUID of the project
     * @param environment PolicyEnvironment enum value
     * @return Optional containing policy if found
     */
    Optional<Policy> findByProjectIdAndEnvironment(UUID projectId, PolicyEnvironment environment);

}
