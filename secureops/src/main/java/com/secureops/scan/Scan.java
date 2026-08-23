package com.secureops.scan;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.secureops.project.Project;
import com.secureops.pipeline.Pipeline;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/scan/Scan.java
 * PURPOSE: JPA entity representing a security scan execution.
 * WHY IT EXISTS: Scans are child entities of both Project and Pipeline; each scan captures security findings at a point in time.
 * DEPENDENCIES: Has foreign keys to Project and Pipeline entities. Maps to 'scan' table in PostgreSQL.
 * RELATIONSHIP: Many-to-One with Project (Scan.projectId → Project.id)
 *              Many-to-One with Pipeline (Scan.pipelineId → Pipeline.id)
 * VALIDATION: Pipeline must belong to the specified Project (enforced in service layer)
 */
@Entity
@Table(name = "scan", indexes = {
    @Index(name = "idx_scan_project_id", columnList = "project_id"),
    @Index(name = "idx_scan_pipeline_id", columnList = "pipeline_id"),
    @Index(name = "idx_scan_status", columnList = "status")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_scan_project_pipeline_env", columnNames = {"project_id", "pipeline_id", "environment"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Scan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "fk_scan_project"))
    private Project project;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "pipeline_id", nullable = false, foreignKey = @ForeignKey(name = "fk_scan_pipeline"))
    private Pipeline pipeline;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment", nullable = false, length = 50)
    private Environment environment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ScanStatus status;

    @Column(name = "started_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Constructor for creating new scans (ID will be auto-generated).
     */
    public Scan(Project project, Pipeline pipeline, Environment environment) {
        this.project = project;
        this.pipeline = pipeline;
        this.environment = environment;
        this.status = ScanStatus.CREATED;
        this.startedAt = LocalDateTime.now();
    }

}
