package com.secureops.project;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/project/Project.java
 * PURPOSE: JPA entity representing a SecureOps project.
 * WHY IT EXISTS: Projects are the top-level isolation boundary in SecureOps. Each project has independent pipelines, scans, and findings.
 * DEPENDENCIES: Used by ProjectRepository and ProjectService. Maps to 'project' table in PostgreSQL.
 */
@Entity
@Table(name = "project", indexes = {
    @Index(name = "idx_project_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "repository_url", length = 512)
    private String repositoryUrl;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    /**
     * Constructor for creating new projects (ID will be auto-generated).
     */
    public Project(String name, String repositoryUrl) {
        this.name = name;
        this.repositoryUrl = repositoryUrl;
        this.createdAt = LocalDateTime.now();
    }

}
