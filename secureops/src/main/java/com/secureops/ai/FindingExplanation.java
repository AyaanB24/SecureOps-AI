package com.secureops.ai;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.secureops.finding.Finding;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/ai/FindingExplanation.java
 * PURPOSE: JPA entity representing AI-generated explanation and remediation for a finding.
 * WHY IT EXISTS: Stores AI-generated explanation, impact analysis, and remediation suggestions for each finding.
 * DEPENDENCIES: Has foreign key to Finding entity. Maps to 'finding_explanation' table in PostgreSQL.
 * RELATIONSHIP: One-to-One with Finding (one explanation per finding, though multiple can be regenerated)
 *
 * CACHING STRATEGY:
 * - Cache valid as long as the finding's data has not changed
 * - Change detection: compare scanner output, package version, etc.
 * - Can be regenerated explicitly via POST with force=true
 *
 * CONTENT:
 * - what_it_means: Plain English explanation of the vulnerability
 * - why_dangerous: Potential risks and business impact
 * - impact_description: What could happen if exploited
 * - remediation_suggestion: Recommended fix or mitigation
 * - version_suggestion: Recommended package version (if applicable)
 * - configuration_suggestion: Config changes or code patterns (if supported by finding evidence)
 */
@Entity
@Table(name = "finding_explanation", indexes = {
    @Index(name = "idx_explanation_finding_id", columnList = "finding_id"),
    @Index(name = "idx_explanation_created_at", columnList = "created_at")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_explanation_finding", columnNames = {"finding_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FindingExplanation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "finding_id", nullable = false, foreignKey = @ForeignKey(name = "fk_explanation_finding"))
    private Finding finding;

    @Column(name = "what_it_means", columnDefinition = "TEXT", nullable = false)
    private String whatItMeans;

    @Column(name = "why_dangerous", columnDefinition = "TEXT", nullable = false)
    private String whyDangerous;

    @Column(name = "impact_description", columnDefinition = "TEXT", nullable = false)
    private String impactDescription;

    @Column(name = "remediation_suggestion", columnDefinition = "TEXT", nullable = false)
    private String remediationSuggestion;

    @Column(name = "version_suggestion", length = 255)
    private String versionSuggestion;

    @Column(name = "configuration_suggestion", columnDefinition = "TEXT")
    private String configurationSuggestion;

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

    public FindingExplanation(Finding finding) {
        this.finding = finding;
        this.createdAt = LocalDateTime.now();
    }
}
