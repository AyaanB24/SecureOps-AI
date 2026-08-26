package com.secureops.report;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.secureops.scan.Scan;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/report/Report.java
 * PURPOSE: JPA entity representing a security report from a tool.
 * WHY IT EXISTS: Reports are child entities of scans; each scan aggregates reports from multiple tools.
 * DEPENDENCIES: Has foreign key to Scan entity. Maps to 'report' table in PostgreSQL.
 * RELATIONSHIP: Many-to-One with Scan (Report.scanId → Scan.id)
 * 
 * REPORT ASSOCIATION WITH PROJECT:
 * Project → Pipeline → Scan → Report
 * The report is associated with a project through its parent Scan,
 * which is associated with a Pipeline, which belongs to a Project.
 */
@Entity
@Table(name = "report", indexes = {
    @Index(name = "idx_report_scan_id", columnList = "scan_id"),
    @Index(name = "idx_report_tool", columnList = "tool"),
    @Index(name = "idx_report_status", columnList = "status")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_report_scan_tool", columnNames = {"scan_id", "tool"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "scan_id", nullable = false, foreignKey = @ForeignKey(name = "fk_report_scan"))
    private Scan scan;

    @Enumerated(EnumType.STRING)
    @Column(name = "tool", nullable = false, length = 50)
    private ReportTool tool;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 512)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ReportStatus status;

    @Column(name = "received_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime receivedAt;

    /**
     * Constructor for creating new reports (ID will be auto-generated).
     */
    public Report(Scan scan, ReportTool tool, String fileName, String filePath) {
        this.scan = scan;
        this.tool = tool;
        this.fileName = fileName;
        this.filePath = filePath;
        this.status = ReportStatus.RECEIVED;
        this.receivedAt = LocalDateTime.now();
    }

}
