package com.secureops.policy;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.secureops.project.Project;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/policy/Policy.java
 * PURPOSE: JPA entity representing a configurable security policy for a project environment.
 * WHY IT EXISTS: Policies define acceptance thresholds for scans, enabling environment-specific security standards.
 * DEPENDENCIES: Has foreign key to Project entity. Maps to 'policy' table in PostgreSQL.
 * RELATIONSHIP: Many-to-One with Project (Policy.projectId → Project.id)
 * 
 * POLICY CONCEPT:
 * Each project can define different security policies for different environments.
 * Example:
 *   Payment API (Production): maxCritical=0, maxHigh=3, maxRiskScore=70
 *   Payment API (Development): maxCritical=5, maxHigh=10, maxRiskScore=200
 *   Job Portal (Production): maxCritical=1, maxHigh=5, maxRiskScore=100
 *   Job Portal (Development): maxCritical=10, maxHigh=20, maxRiskScore=300
 * 
 * PROJECT-LEVEL ISOLATION:
 * Each policy is scoped to a specific project + environment combination.
 * Unique constraint (project_id, environment) ensures one policy per environment per project.
 * Different projects can have different threshold values for same environment.
 * 
 * THRESHOLDS:
 * - maxCritical: Maximum allowed CRITICAL findings (0 = zero tolerance)
 * - maxHigh: Maximum allowed HIGH findings
 * - maxRiskScore: Maximum allowed total risk score (from Phase 7)
 * 
 * Used by PolicyEngine (Phase 8) to evaluate if a scan meets project's security standards.
 */
@Entity
@Table(name = "policy", indexes = {
    @Index(name = "idx_policy_project_id", columnList = "project_id"),
    @Index(name = "idx_policy_environment", columnList = "environment")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_policy_project_environment", columnNames = {"project_id", "environment"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "fk_policy_project"))
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment", nullable = false, length = 50)
    private PolicyEnvironment environment;

    @Column(name = "max_critical", nullable = false)
    private Integer maxCritical;

    @Column(name = "max_high", nullable = false)
    private Integer maxHigh;

    @Column(name = "max_risk_score", nullable = false)
    private Integer maxRiskScore;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Constructor for creating new policies (ID will be auto-generated).
     */
    public Policy(Project project, PolicyEnvironment environment, Integer maxCritical, Integer maxHigh, Integer maxRiskScore) {
        this.project = project;
        this.environment = environment;
        this.maxCritical = maxCritical;
        this.maxHigh = maxHigh;
        this.maxRiskScore = maxRiskScore;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

}
