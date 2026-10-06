package com.secureops.report;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/report/ReportProcessingResult.java
 * PURPOSE: DTO for report processing API response.
 * WHY IT EXISTS: Provides structured response for report processing endpoint.
 * DEPENDENCIES: Returned by ReportProcessingService and ReportController.
 * 
 * RESPONSE STRUCTURE:
 * {
 *   "reportId": "550e8400-e29b-41d4-a716-446655440000",
 *   "tool": "TRIVY",
 *   "findingsCreated": 5,
 *   "status": "PROCESSED"
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportProcessingResult {

    private UUID reportId;
    private ReportTool tool;
    private Integer findingsCreated;
    private ReportStatus status;

}
