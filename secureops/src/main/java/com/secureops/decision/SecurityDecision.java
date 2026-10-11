package com.secureops.decision;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.secureops.scan.Scan;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/decision/SecurityDecision.java
 * PURPOSE: JPA entity representing a security decision for a scan.
 * WHY IT EXISTS: Stores the final PASS/BLOCK verdict along with reasoning and metrics.
 * DEPENDENCIES: Has foreign key to Scan entity. Maps to 'security_decision' table in PostgreSQL.
 * RELATIONSHIP: One-to-One with Scan (one decision per scan)
 * 
 * DECISION DETERMINATION:
 * 1. Load scan (includes project and environment)
 * 2. Load project policy for that environment
 * 3. Count findings by severity
 * 4. Calculate risk score
 * 5. Compare against policy thresholds
 * 6. Generate PASS or BLOCK
 * 7. Store decision with all metrics and reason
 * 
 * DETERMINISTIC:
 * - Same findings always produce same decision
 * - No randomness or AI involved
 * - Based purely on policy thresholds and counts
 * - Reproducible across systems
 * 
 * EXAMPLE BLOCK:
 * Scan has 1 CRITICAL (policy max: 0) → BLOCK
 * Reason: "Critical findings exceed the configured production policy"
 * 
 * EXAMPLE PASS:
 * Scan has 0 CRITICAL, 1 HIGH (policy max: 0, 3) → PASS
 * Reason: "Security policy satisfied"
 */
@Entity
@Table(name = "security_decision", indexes = {
    @Index(name = "idx_decision_scan_id", columnList = "scan_id"),
    @Index(name = "idx_decision_decision", columnList = "decision"),
    @Index(name = "idx_decision_created_at", columnList = "created_at")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_decision_scan", columnNames = {"scan_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SecurityDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "scan_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_decision_scan"))
    private Scan scan;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 10)
    private Decision decision;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;

    @Column(name = "critical_count", nullable = false)
    private Long criticalCount;

    @Column(name = "high_count", nullable = false)
    private Long highCount;

    @Column(name = "medium_count", nullable = false)
    private Long mediumCount;

    @Column(name = "low_count", nullable = false)
    private Long lowCount;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    /**
     * Constructor for creating security decisions.
     */
    public SecurityDecision(Scan scan, Decision decision, Integer riskScore,
                           Long criticalCount, Long highCount, Long mediumCount, Long lowCount, String reason) {
        this.scan = scan;
        this.decision = decision;
        this.riskScore = riskScore;
        this.criticalCount = criticalCount;
        this.highCount = highCount;
        this.mediumCount = mediumCount;
        this.lowCount = lowCount;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
    }

}
