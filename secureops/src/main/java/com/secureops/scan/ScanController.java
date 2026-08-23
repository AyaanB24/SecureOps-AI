package com.secureops.scan;

import com.secureops.scan.dto.CreateScanRequest;
import com.secureops.scan.dto.ScanResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/scan/ScanController.java
 * PURPOSE: REST controller for scan management endpoints.
 * WHY IT EXISTS: Exposes scan CRUD operations via HTTP; handles request routing and response serialization.
 * DEPENDENCIES: Uses ScanService for business logic; returns ScanResponse DTOs.
 * SECURITY: Validates pipeline belongs to project before allowing scan creation.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ScanController {

    private final ScanService scanService;

    /**
     * Create a new scan for a specific project.
     * POST /api/projects/{projectId}/scans
     *
     * @param projectId UUID of the project
     * @param request CreateScanRequest containing pipeline and environment
     * @return ResponseEntity with ScanResponse and HTTP 201 Created
     */
    @PostMapping("/projects/{projectId}/scans")
    public ResponseEntity<ScanResponse> createScan(
        @PathVariable UUID projectId,
        @Valid @RequestBody CreateScanRequest request) {
        log.info("POST /api/projects/{}/scans - Creating scan", projectId);
        ScanResponse response = scanService.createScan(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all scans for a specific project.
     * GET /api/projects/{projectId}/scans
     *
     * @param projectId UUID of the project
     * @return ResponseEntity with List of ScanResponse and HTTP 200 OK
     */
    @GetMapping("/projects/{projectId}/scans")
    public ResponseEntity<List<ScanResponse>> getScansByProject(@PathVariable UUID projectId) {
        log.info("GET /api/projects/{}/scans - Fetching all scans", projectId);
        List<ScanResponse> scans = scanService.getScansByProject(projectId);
        return ResponseEntity.ok(scans);
    }

    /**
     * Get a specific scan by ID.
     * GET /api/scans/{scanId}
     *
     * @param scanId UUID of the scan
     * @return ResponseEntity with ScanResponse and HTTP 200 OK
     * @throws ScanNotFoundException if scan does not exist (converted to HTTP 404)
     */
    @GetMapping("/scans/{scanId}")
    public ResponseEntity<ScanResponse> getScanById(@PathVariable UUID scanId) {
        log.info("GET /api/scans/{} - Fetching scan", scanId);
        ScanResponse response = scanService.getScanById(scanId);
        return ResponseEntity.ok(response);
    }

}
