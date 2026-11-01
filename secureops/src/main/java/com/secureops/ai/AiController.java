package com.secureops.ai;

import com.secureops.ai.dto.AiAnalysisSummaryResponse;
import com.secureops.ai.dto.FindingExplanationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/ai/AiController.java
 * PURPOSE: REST controller for AI analysis endpoints.
 * WHY IT EXISTS: Exposes AI analysis generation and retrieval via HTTP.
 * DEPENDENCIES: Uses AiService for business logic; returns DTO responses.
 * SECURITY: Validates scan and finding ownership before processing.
 *
 * ENDPOINTS:
 * - POST /api/ai/scans/{scanId}/summary - Generate/retrieve AI summary
 * - POST /api/ai/findings/{findingId}/remediation - Generate/retrieve finding remediation
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AiController {

    private final AiService aiService;

    /**
     * Generate or retrieve AI security summary for a scan.
     * POST /api/ai/scans/{scanId}/summary
     *
     * @param scanId UUID of the scan
     * @param force Optional query parameter (default: false). If true, regenerates even if cached.
     * @return ResponseEntity with AiAnalysisSummaryResponse and HTTP 200 OK
     * @throws ScanNotFoundException if scan does not exist
     */
    @PostMapping("/scans/{scanId}/summary")
    public ResponseEntity<AiAnalysisSummaryResponse> generateScanSummary(
        @PathVariable UUID scanId,
        @RequestParam(value = "force", defaultValue = "false") boolean force) {
        
        log.info("POST /api/ai/scans/{}/summary - Generating AI summary", scanId);
        
        AiAnalysisSummaryResponse response = aiService.generateOrGetScanSummary(scanId, force);
        return ResponseEntity.ok(response);
    }

    /**
     * Generate or retrieve AI explanation and remediation for a finding.
     * POST /api/ai/findings/{findingId}/remediation
     *
     * @param findingId UUID of the finding
     * @param force Optional query parameter (default: false). If true, regenerates even if cached.
     * @return ResponseEntity with FindingExplanationResponse and HTTP 200 OK
     * @throws FindingNotFoundException if finding does not exist
     */
    @PostMapping("/findings/{findingId}/remediation")
    public ResponseEntity<FindingExplanationResponse> generateFindingRemediation(
        @PathVariable UUID findingId,
        @RequestParam(value = "force", defaultValue = "false") boolean force) {
        
        log.info("POST /api/ai/findings/{}/remediation - Generating AI remediation", findingId);
        
        FindingExplanationResponse response = aiService.generateOrGetFindingRemediation(findingId, force);
        return ResponseEntity.ok(response);
    }

    /**
     * Get configuration status (for debugging/frontend display).
     * GET /api/ai/status
     */
    @GetMapping("/status")
    public ResponseEntity<String> getConfigurationStatus() {
        log.debug("GET /api/ai/status");
        return ResponseEntity.ok("""
{
  "gemini": "configured"
}
""");
    }
}
