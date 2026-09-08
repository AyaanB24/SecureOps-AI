package com.secureops.finding;

import com.secureops.finding.dto.FindingResponse;
import com.secureops.finding.dto.CreateFindingRequest;
import com.secureops.scan.Scan;
import com.secureops.scan.ScanRepository;
import com.secureops.scan.ScanNotFoundException;
import com.secureops.report.Report;
import com.secureops.report.ReportRepository;
import com.secureops.report.ReportNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * FILE: src/main/java/com/secureops/finding/FindingController.java
 * PURPOSE: REST API endpoints for finding management.
 * WHY IT EXISTS: Exposes finding CRUD operations through HTTP API.
 * DEPENDENCIES: Uses FindingService for business logic; ScanRepository and ReportRepository for entity access.
 * 
 * API ENDPOINTS:
 * GET  /api/scans/{scanId}/findings          - List all findings for a scan
 * GET  /api/findings/{findingId}             - Get specific finding by ID
 * POST /api/scans/{scanId}/findings          - Create finding (development/testing only)
 * 
 * VALIDATION PERFORMED:
 * - Scan exists before retrieving findings
 * - Report exists and belongs to scan before creating finding
 * - Report.scanId matches requested scanId (cross-project prevention)
 */
@RestController
@RequestMapping("/api")
@Slf4j
public class FindingController {

    private final FindingService findingService;
    private final ScanRepository scanRepository;
    private final ReportRepository reportRepository;

    public FindingController(FindingService findingService, ScanRepository scanRepository, ReportRepository reportRepository) {
        this.findingService = findingService;
        this.scanRepository = scanRepository;
        this.reportRepository = reportRepository;
    }

    /**
     * Get all findings for a specific scan.
     * Validates that scan exists before retrieving findings.
     * 
     * @param scanId UUID of the scan
     * @return List of findings with HTTP 200
     */
    @GetMapping("/scans/{scanId}/findings")
    public ResponseEntity<List<FindingResponse>> getFindings(@PathVariable UUID scanId) {
        log.info("GET /api/scans/{}/findings", scanId);

        // Validate scan exists
        Scan scan = scanRepository.findById(scanId)
            .orElseThrow(() -> new ScanNotFoundException(scanId.toString()));
        log.debug("Scan found: {}", scan.getId());

        // Retrieve findings
        List<Finding> findings = findingService.getFindingsByScan(scanId);
        log.info("Retrieved {} findings for scan: {}", findings.size(), scanId);

        // Convert to response DTOs
        List<FindingResponse> responses = findings.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Get a specific finding by ID.
     * 
     * @param findingId UUID of the finding
     * @return Finding with HTTP 200
     * @throws FindingNotFoundException if finding not found
     */
    @GetMapping("/findings/{findingId}")
    public ResponseEntity<FindingResponse> getFinding(@PathVariable UUID findingId) {
        log.info("GET /api/findings/{}", findingId);

        Finding finding = findingService.getFindingById(findingId);
        log.debug("Finding found: {}", finding.getId());

        return ResponseEntity.ok(mapToResponse(finding));
    }

    /**
     * Create a new finding (development/testing endpoint).
     * This endpoint allows manual finding creation for testing purposes.
     * 
     * In production, findings would be created automatically by parsing security tool reports.
     * 
     * @param scanId UUID of the scan
     * @param reportId UUID of the report
     * @param request Finding creation request with tool, ruleId, title, etc.
     * @return Created finding with HTTP 201
     * @throws IllegalArgumentException if report doesn't belong to scan
     */
    @PostMapping("/scans/{scanId}/findings")
    public ResponseEntity<FindingResponse> createFinding(
            @PathVariable UUID scanId,
            @RequestParam UUID reportId,
            @RequestBody CreateFindingRequest request) {
        log.info("POST /api/scans/{}/findings for report: {}", scanId, reportId);

        // Validate scan exists
        Scan scan = scanRepository.findById(scanId)
            .orElseThrow(() -> new ScanNotFoundException(scanId.toString()));
        log.debug("Scan found: {}", scan.getId());

        // Validate report exists and belongs to scan
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new ReportNotFoundException(reportId.toString()));
        log.debug("Report found: {}", report.getId());

        if (!report.getScan().getId().equals(scanId)) {
            log.error("Security validation failed: report {} doesn't belong to scan {}", reportId, scanId);
            throw new IllegalArgumentException(
                "Report does not belong to the specified scan");
        }

        // Create finding
        Finding finding = findingService.createFinding(
            scan, report, request.getTool(), request.getRuleId(),
            request.getTitle(), request.getDescription(), request.getSeverity(),
            request.getFilePath(), request.getLineNumber(),
            request.getPackageName(), request.getPackageVersion());

        log.info("Finding created: {}", finding.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(finding));
    }

    /**
     * Map Finding entity to FindingResponse DTO.
     * 
     * @param finding Finding entity
     * @return FindingResponse DTO
     */
    private FindingResponse mapToResponse(Finding finding) {
        FindingResponse response = new FindingResponse();
        response.setId(finding.getId());
        response.setScanId(finding.getScan().getId());
        response.setReportId(finding.getReport().getId());
        response.setTool(finding.getTool());
        response.setRuleId(finding.getRuleId());
        response.setTitle(finding.getTitle());
        response.setDescription(finding.getDescription());
        response.setSeverity(finding.getSeverity());
        response.setFilePath(finding.getFilePath());
        response.setLineNumber(finding.getLineNumber());
        response.setPackageName(finding.getPackageName());
        response.setPackageVersion(finding.getPackageVersion());
        response.setFingerprint(finding.getFingerprint());
        response.setStatus(finding.getStatus());
        response.setCreatedAt(finding.getCreatedAt());
        return response;
    }

}
