package com.secureops.ai;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.secureops.scan.Scan;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/ai/AiAnalysisSummary.java
 * PURPOSE: JPA entity representing AI-generated security analysis for a scan/build.
 * WHY IT EXISTS: Stores the AI-generated build-specific security assessment for each scan.
 * DEPENDENCIES: Has foreign key to Scan entity. Maps to 'ai_analysis_summary' table in PostgreSQL.
 * RELATIONSHIP: One-to-One with Scan (one summary per scan, though multiple can be regenerated)
 *
 * CACHING STRATEGY:
 * - Cache valid as long as the scan's findings have not changed
 * - findingHash tracks the state of findings when summary was generated
 * - If findings change, summary is marked stale
 * - Can be regenerated explicitly via POST with force=true
 *
 * PERSISTENCE:
 * - Store generated analysis in database to avoid unnecessary Gemini API calls
 * - Reuse cached summary if findings haven't changed
 * - Track generation timestamp and AI model version for audit
 */
@Entity
@Table(name = "ai_analysis_summary", indexes = {
    @Index(name = "idx_summary_scan_id", columnList = "scan_id"),
    @Index(name = "idx_summary_created_at", columnList = "created_at")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_summary_scan", columnNames = {"scan_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiAnalysisSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "scan_id", nullable = false, foreignKey = @ForeignKey(name = "fk_summary_scan"))
    private Scan scan;

    @Column(name = "overall_assessment", columnDefinition = "TEXT", nullable = false)
    private String overallAssessment;

    @Column(name = "critical_count", nullable = false)
    private Long criticalCount = 0L;

    @Column(name = "high_count", nullable = false)
    private Long highCount = 0L;

    @Column(name = "medium_count", nullable = false)
    private Long mediumCount = 0L;

    @Column(name = "low_count", nullable = false)
    private Long lowCount = 0L;

    @Column(name = "total_findings", nullable = false)
    private Long totalFindings = 0L;

    @Column(name = "top_three_risks", columnDefinition = "TEXT", nullable = false)
    private String topThreeRisks;

    @Column(name = "key_concerns", columnDefinition = "TEXT")
    private String keyConcerns;

    @Column(name = "recommended_actions", columnDefinition = "TEXT")
    private String recommendedActions;

    @Column(name = "has_significant_concerns", nullable = false)
    private Boolean hasSignificantConcerns = false;

    @Column(name = "ai_provider", nullable = false, length = 50)
    private String aiProvider = "GEMINI";

    @Column(name = "ai_model", length = 100)
    private String aiModel;

    @Column(name = "generation_status", nullable = false, length = 50)
    private String generationStatus = "SUCCESS"; // SUCCESS, FAILED, PENDING

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "regenerated_at")
    private LocalDateTime regeneratedAt;

    @Column(name = "finding_hash", length = 64)
    private String findingHash; // SHA-256 hash of findings at generation time

    public AiAnalysisSummary(Scan scan) {
        this.scan = scan;
        this.createdAt = LocalDateTime.now();
    }
}
