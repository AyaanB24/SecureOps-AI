package com.secureops.finding;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.secureops.scan.Scan;
import com.secureops.report.Report;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/finding/Finding.java
 * PURPOSE: JPA entity representing a normalized security finding (tool-independent).
 * WHY IT EXISTS: Provides unified finding model across different security tools (Trivy, Semgrep, OWASP).
 * DEPENDENCIES: Has foreign keys to Scan and Report entities. Maps to 'finding' table in PostgreSQL.
 * RELATIONSHIP: Many-to-One with Scan (Finding.scanId → Scan.id)
 *              Many-to-One with Report (Finding.reportId → Report.id)
 * 
 * FINDING NORMALIZATION:
 * Different tools (Trivy, Semgrep, OWASP) produce different formats.
 * This entity normalizes them into a common model:
 * - Trivy CVE → Finding with ruleId=CVE-xxx, severity, packageName, packageVersion
 * - Semgrep rule → Finding with ruleId=rule-id, severity, filePath, lineNumber
 * - OWASP → Finding with ruleId=owasp-rule, severity, packageName, packageVersion
 * 
 * FINGERPRINT STRATEGY:
 * Fingerprint = hash(tool, ruleId, filePath, lineNumber, packageName)
 * Used for deduplication across multiple scans (same vulnerability won't be duplicated)
 */
@Entity
@Table(name = "finding", indexes = {
    @Index(name = "idx_finding_scan_id", columnList = "scan_id"),
    @Index(name = "idx_finding_report_id", columnList = "report_id"),
    @Index(name = "idx_finding_severity", columnList = "severity"),
    @Index(name = "idx_finding_status", columnList = "status"),
    @Index(name = "idx_finding_fingerprint", columnList = "fingerprint")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Finding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "scan_id", nullable = false, foreignKey = @ForeignKey(name = "fk_finding_scan"))
    private Scan scan;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "report_id", nullable = false, foreignKey = @ForeignKey(name = "fk_finding_report"))
    private Report report;

    @Column(name = "tool", nullable = false, length = 50)
    private String tool;

    @Column(name = "rule_id", nullable = false, length = 255)
    private String ruleId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 50)
    private Severity severity;

    @Column(name = "file_path", length = 512)
    private String filePath;

    @Column(name = "line_number")
    private Integer lineNumber;

    @Column(name = "package_name", length = 255)
    private String packageName;

    @Column(name = "package_version", length = 255)
    private String packageVersion;

    @Column(name = "fingerprint", nullable = false, length = 64)
    private String fingerprint;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private FindingStatus status;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    /**
     * Constructor for creating new findings (ID will be auto-generated).
     */
    public Finding(Scan scan, Report report, String tool, String ruleId, String title,
                   String description, Severity severity, String filePath, Integer lineNumber,
                   String packageName, String packageVersion, String fingerprint) {
        this.scan = scan;
        this.report = report;
        this.tool = tool;
        this.ruleId = ruleId;
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.filePath = filePath;
        this.lineNumber = lineNumber;
        this.packageName = packageName;
        this.packageVersion = packageVersion;
        this.fingerprint = fingerprint;
        this.status = FindingStatus.OPEN;
        this.createdAt = LocalDateTime.now();
    }

}
