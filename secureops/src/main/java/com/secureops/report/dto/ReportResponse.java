package com.secureops.report.dto;

import com.secureops.report.Report;
import com.secureops.report.ReportTool;
import com.secureops.report.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/report/dto/ReportResponse.java
 * PURPOSE: DTO for returning report data via REST API.
 * WHY IT EXISTS: Decouples API response structure from JPA entity; includes only relevant fields for clients.
 * DEPENDENCIES: Used by ReportController and ReportService; converts from Report entity via static factory method.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {

    private UUID id;
    private UUID scanId;
    private ReportTool tool;
    private String fileName;
    private ReportStatus status;
    private LocalDateTime receivedAt;

    /**
     * Factory method to convert Report entity to ReportResponse DTO.
     *
     * @param report Report entity
     * @return ReportResponse DTO
     */
    public static ReportResponse fromEntity(Report report) {
        return new ReportResponse(
            report.getId(),
            report.getScan().getId(),
            report.getTool(),
            report.getFileName(),
            report.getStatus(),
            report.getReceivedAt()
        );
    }

}
