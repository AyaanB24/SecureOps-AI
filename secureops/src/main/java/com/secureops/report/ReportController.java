package com.secureops.report;

import com.secureops.report.dto.ReportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/report/ReportController.java
 * PURPOSE: REST controller for report management endpoints.
 * WHY IT EXISTS: Exposes report upload and retrieval via HTTP; handles multipart file uploads.
 * DEPENDENCIES: Uses ReportService for business logic; returns ReportResponse DTOs.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;

    /**
     * Upload a security report for a specific scan.
     * POST /api/scans/{scanId}/reports
     *
     * @param scanId UUID of the scan
     * @param tool ReportTool enum value (TRIVY, SEMGREP, OWASP_DEPENDENCY_CHECK)
     * @param file MultipartFile containing the report
     * @return ResponseEntity with ReportResponse and HTTP 201 Created
     */
    @PostMapping("/scans/{scanId}/reports")
    public ResponseEntity<ReportResponse> uploadReport(
        @PathVariable UUID scanId,
        @RequestParam("tool") String toolString,
        @RequestParam("file") MultipartFile file) {
        
        log.info("POST /api/scans/{}/reports - Uploading report with tool: {}", scanId, toolString);
        
        try {
            // Convert string to enum
            ReportTool tool = ReportTool.valueOf(toolString.toUpperCase());
            ReportResponse response = reportService.uploadReport(scanId, tool, file);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid tool: {}", toolString);
            throw new IllegalArgumentException("Invalid tool: " + toolString + ". Supported tools: TRIVY, SEMGREP, OWASP_DEPENDENCY_CHECK");
        } catch (IOException e) {
            log.error("File storage error: {}", e.getMessage());
            throw new RuntimeException("Failed to store report file: " + e.getMessage());
        }
    }

    /**
     * Get all reports for a specific scan.
     * GET /api/scans/{scanId}/reports
     *
     * @param scanId UUID of the scan
     * @return ResponseEntity with List of ReportResponse and HTTP 200 OK
     */
    @GetMapping("/scans/{scanId}/reports")
    public ResponseEntity<List<ReportResponse>> getReportsByScan(@PathVariable UUID scanId) {
        log.info("GET /api/scans/{}/reports - Fetching all reports", scanId);
        List<ReportResponse> reports = reportService.getReportsByScan(scanId);
        return ResponseEntity.ok(reports);
    }

    /**
     * Get a specific report by ID.
     * GET /api/reports/{reportId}
     *
     * @param reportId UUID of the report
     * @return ResponseEntity with ReportResponse and HTTP 200 OK
     * @throws ReportNotFoundException if report does not exist (converted to HTTP 404)
     */
    @GetMapping("/reports/{reportId}")
    public ResponseEntity<ReportResponse> getReportById(@PathVariable UUID reportId) {
        log.info("GET /api/reports/{} - Fetching report", reportId);
        ReportResponse response = reportService.getReportById(reportId);
        return ResponseEntity.ok(response);
    }

}
