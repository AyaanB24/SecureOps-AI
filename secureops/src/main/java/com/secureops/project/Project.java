package com.secureops.project;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.secureops.pipeline.Pipeline;
import com.secureops.scan.Scan;
import com.secureops.policy.Policy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_project_name", columnNames = "name")
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
     * One-to-Many relationship: One project has many pipelines.
     * Cascade delete: When project is deleted, all pipelines are deleted.
     * Note: Scans are NOT cascaded here; they are deleted through Pipeline → Scan cascade
     * to avoid dual-deletion conflicts since Scan has a NOT NULL foreign key to Pipeline.
     */
    @OneToMany(mappedBy = "project", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Pipeline> pipelines = new ArrayList<>();

    /**
     * One-to-Many relationship: One project has many scans.
     * NOTE: Do NOT use cascade here. Scans are deleted through Pipeline cascade
     * to prevent constraint violations on the pipeline_id NOT NULL constraint.
     */
    @OneToMany(mappedBy = "project")
    private List<Scan> scans = new ArrayList<>();

    /**
     * One-to-Many relationship: One project has many policies.
     * Cascade delete: When project is deleted, all policies are deleted.
     */
    @OneToMany(mappedBy = "project", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Policy> policies = new ArrayList<>();

    /**
     * Constructor for creating new projects (ID will be auto-generated).
     */
    public Project(String name, String repositoryUrl) {
        this.name = name;
        this.repositoryUrl = repositoryUrl;
        this.createdAt = LocalDateTime.now();
        this.pipelines = new ArrayList<>();
        this.scans = new ArrayList<>();
        this.policies = new ArrayList<>();
    }

}

