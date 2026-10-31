package com.secureops.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/project/ProjectRepository.java
 * PURPOSE: Spring Data JPA repository for Project entity.
 * WHY IT EXISTS: Provides CRUD operations and custom queries for projects without writing boilerplate SQL.
 * DEPENDENCIES: Extends JpaRepository; used by ProjectService to access database.
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    // Inherits: save(), findById(), findAll(), delete(), etc.

    /**
     * Find a project by its name.
     * Used to check for duplicate projects before creation.
     *
     * @param name Name of the project
     * @return Optional containing the project if found
     */
    Optional<Project> findByName(String name);

    /**
     * Find all projects accessible by a user.
     * Users can access: own projects + system projects (owner_id IS NULL).
     * System projects (created by Jenkins or pre-existing) are accessible to all users.
     *
     * @param userId User ID for ownership check
     * @return List of accessible projects owned by the user or system projects
     */
    @Query("SELECT p FROM Project p WHERE p.ownerId = :userId OR p.ownerId IS NULL ORDER BY p.createdAt DESC")
    List<Project> findAccessibleProjects(@Param("userId") UUID userId);

}

