package com.secureops.pipeline;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.secureops.project.Project;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/pipeline/Pipeline.java
 * PURPOSE: JPA entity representing a CI/CD pipeline within a project.
 * WHY IT EXISTS: Pipelines are child entities of projects; each project can have multiple pipelines.
 * DEPENDENCIES: Has a foreign key to Project entity. Maps to 'pipeline' table in PostgreSQL.
 * RELATIONSHIP: Many-to-One with Project (Pipeline.projectId → Project.id)
 */
@Entity
@Table(name = "pipeline", indexes = {
    @Index(name = "idx_pipeline_project_id", columnList = "project_id"),
    @Index(name = "idx_pipeline_job_name", columnList = "job_name")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_pipeline_project_job_build", columnNames = {"project_id", "job_name", "build_number"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Pipeline {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pipeline_project"))
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 50)
    private PipelineProvider provider;

    @Column(name = "job_name", nullable = false, length = 255)
    private String jobName;

    @Column(name = "build_number", nullable = false)
    private Long buildNumber;

    @Column(name = "branch", nullable = false, length = 255)
    private String branch;

    @Column(name = "commit_sha", length = 255)
    private String commitSha;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    /**
     * Constructor for creating new pipelines (ID will be auto-generated).
     */
    public Pipeline(Project project, PipelineProvider provider, String jobName, Long buildNumber, String branch, String commitSha) {
        this.project = project;
        this.provider = provider;
        this.jobName = jobName;
        this.buildNumber = buildNumber;
        this.branch = branch;
        this.commitSha = commitSha;
        this.createdAt = LocalDateTime.now();
    }

}
