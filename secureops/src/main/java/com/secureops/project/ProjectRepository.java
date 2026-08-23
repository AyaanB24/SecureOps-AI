package com.secureops.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
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

}
