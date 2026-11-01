package com.secureops.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secureops.ai.dto.AiAnalysisSummaryResponse;
import com.secureops.ai.dto.FindingExplanationResponse;
import com.secureops.finding.Finding;
import com.secureops.finding.FindingRepository;
import com.secureops.finding.Severity;
import com.secureops.scan.Scan;
import com.secureops.scan.ScanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDateTime;

/**
 * FILE: src/main/java/com/secureops/ai/AiService.java
 * PURPOSE: Business logic for AI analysis generation and caching.
 * WHY IT EXISTS: Orchestrates Gemini calls, caching, and persistence for AI analysis.
 * DEPENDENCIES: Uses GeminiService for API calls, repositories for persistence.
 *
 * CACHING LOGIC:
 * - For scans: Use findingHash to detect if findings changed
 * - For findings: Check if finding data (severity, package, etc.) changed
 * - If cached result is still valid, return it
 * - If findings changed or force=true, regenerate via Gemini
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private final GeminiService geminiService;
    private final AiAnalysisSummaryRepository summaryRepository;
    private final FindingExplanationRepository explanationRepository;
    private final ScanRepository scanRepository;
    private final FindingRepository findingRepository;
    private final ObjectMapper objectMapper;

    /**
     * Generate or retrieve AI summary for a scan.
     * 
     * @param scanId UUID of the scan
     * @param force If true, always regenerate (ignore cache)
     * @return AiAnalysisSummaryResponse with analysis
     * @throws ScanNotFoundException if scan doesn't exist
     */
    public AiAnalysisSummaryResponse generateOrGetScanSummary(UUID scanId, boolean force) {
        log.info("Generating/retrieving AI summary for scan: {} (force={})", scanId, force);

        // Verify scan exists
        Scan scan = scanRepository.findById(scanId)
            .orElseThrow(() -> new ScanNotFoundException(scanId.toString()));

        // Check if cached summary exists and is still valid
        if (!force) {
            var existing = summaryRepository.findByScanId(scanId);
            if (existing.isPresent()) {
                AiAnalysisSummary summary = existing.get();
                if ("SUCCESS".equals(summary.getGenerationStatus())) {
                    log.debug("Returning cached summary for scan: {}", scanId);
                    return AiAnalysisSummaryResponse.fromEntity(summary);
                }
            }
        }

        // Generate new summary
        List<Finding> findings = findingRepository.findByScanId(scanId);
        String findingsSummary = buildFindingsSummary(findings);
        String findingHash = hashFindings(findings);

        AiSummaryResult result = geminiService.generateScanSummary(
            scanId.toString(),
            scan.getProject().getName(),
            scan.getPipeline().getBuildNumber().toString(),
            findingsSummary
        );

        // Count findings by severity
        long criticalCount = findings.stream().filter(f -> f.getSeverity() == Severity.CRITICAL).count();
        long highCount = findings.stream().filter(f -> f.getSeverity() == Severity.HIGH).count();
        long mediumCount = findings.stream().filter(f -> f.getSeverity() == Severity.MEDIUM).count();
        long lowCount = findings.stream().filter(f -> f.getSeverity() == Severity.LOW).count();

        // Create or update summary
        AiAnalysisSummary summary = summaryRepository.findByScanId(scanId)
            .orElse(new AiAnalysisSummary(scan));

        summary.setOverallAssessment(result.getOverallAssessment());
        summary.setTopThreeRisks(result.getTopThreeRisks());
        summary.setKeyConcerns(result.getKeyConcerns());
        summary.setRecommendedActions(result.getRecommendedActions());
        summary.setHasSignificantConcerns(result.getHasSignificantConcerns());
        summary.setCriticalCount(criticalCount);
        summary.setHighCount(highCount);
        summary.setMediumCount(mediumCount);
        summary.setLowCount(lowCount);
        summary.setTotalFindings((long) findings.size());
        summary.setFindingHash(findingHash);
        summary.setGenerationStatus(result.getStatus());
        summary.setErrorMessage(result.getError());
        summary.setAiModel(result.getModel());
        summary.setRegeneratedAt(LocalDateTime.now());

        AiAnalysisSummary saved = summaryRepository.save(summary);
        log.info("AI summary saved for scan: {}", scanId);

        return AiAnalysisSummaryResponse.fromEntity(saved);
    }

    /**
     * Generate or retrieve AI remediation for a finding.
     * 
     * @param findingId UUID of the finding
     * @param force If true, always regenerate (ignore cache)
     * @return FindingExplanationResponse with explanation and remediation
     * @throws FindingNotFoundException if finding doesn't exist
     */
    public FindingExplanationResponse generateOrGetFindingRemediation(UUID findingId, boolean force) {
        log.info("Generating/retrieving remediation for finding: {} (force={})", findingId, force);

        // Verify finding exists
        Finding finding = findingRepository.findById(findingId)
            .orElseThrow(() -> new FindingNotFoundException(findingId.toString()));

        // Check if cached explanation exists
        if (!force) {
            var existing = explanationRepository.findByFindingId(findingId);
            if (existing.isPresent()) {
                FindingExplanation explanation = existing.get();
                if ("SUCCESS".equals(explanation.getGenerationStatus())) {
                    log.debug("Returning cached remediation for finding: {}", findingId);
                    return FindingExplanationResponse.fromEntity(explanation);
                }
            }
        }

        // Generate new remediation
        String findingDetails = buildFindingDetails(finding);
        AiRemediationResult result = geminiService.generateFindingRemediation(
            findingId.toString(),
            findingDetails
        );

        // Create or update explanation
        FindingExplanation explanation = explanationRepository.findByFindingId(findingId)
            .orElse(new FindingExplanation(finding));

        explanation.setWhatItMeans(result.getWhatItMeans());
        explanation.setWhyDangerous(result.getWhyDangerous());
        explanation.setImpactDescription(result.getImpactDescription());
        explanation.setRemediationSuggestion(result.getRemediationSuggestion());
        explanation.setVersionSuggestion(result.getVersionSuggestion());
        explanation.setConfigurationSuggestion(result.getConfigurationSuggestion());
        explanation.setGenerationStatus(result.getStatus());
        explanation.setErrorMessage(result.getError());
        explanation.setAiModel(result.getModel());
        explanation.setRegeneratedAt(LocalDateTime.now());

        FindingExplanation saved = explanationRepository.save(explanation);
        log.info("Remediation saved for finding: {}", findingId);

        return FindingExplanationResponse.fromEntity(saved);
    }

    /**
     * Build findings summary for AI prompt.
     */
    private String buildFindingsSummary(List<Finding> findings) {
        long critical = findings.stream().filter(f -> f.getSeverity() == Severity.CRITICAL).count();
        long high = findings.stream().filter(f -> f.getSeverity() == Severity.HIGH).count();
        long medium = findings.stream().filter(f -> f.getSeverity() == Severity.MEDIUM).count();
        long low = findings.stream().filter(f -> f.getSeverity() == Severity.LOW).count();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Total findings: %d\n", findings.size()));
        sb.append(String.format("Critical: %d\n", critical));
        sb.append(String.format("High: %d\n", high));
        sb.append(String.format("Medium: %d\n", medium));
        sb.append(String.format("Low: %d\n", low));

        if (findings.size() <= 10) {
            sb.append("\nFindings:\n");
            for (Finding f : findings) {
                sb.append(String.format("- [%s] %s (%s)", f.getSeverity(), f.getTitle(), f.getTool()));
                if (f.getPackageName() != null) {
                    sb.append(String.format(" - %s@%s", f.getPackageName(), f.getPackageVersion()));
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Build finding details for AI prompt.
     */
    private String buildFindingDetails(Finding finding) {
        Map<String, String> details = new HashMap<>();
        details.put("title", finding.getTitle());
        details.put("severity", finding.getSeverity().name());
        details.put("scanner", finding.getTool());
        details.put("ruleId", finding.getRuleId());
        details.put("description", finding.getDescription());

        if (finding.getPackageName() != null) {
            details.put("package", finding.getPackageName());
            details.put("installedVersion", finding.getPackageVersion());
        }

        if (finding.getFilePath() != null) {
            details.put("filePath", finding.getFilePath());
        }

        if (finding.getCvssScore() != null) {
            details.put("cvssScore", finding.getCvssScore().toString());
        }

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(details);
        } catch (Exception e) {
            log.warn("Failed to serialize finding details: {}", e.getMessage());
            return details.toString();
        }
    }

    /**
     * Generate SHA-256 hash of findings for cache validation.
     */
    private String hashFindings(List<Finding> findings) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder sb = new StringBuilder();
            for (Finding f : findings) {
                sb.append(f.getId()).append("|")
                  .append(f.getSeverity()).append("|")
                  .append(f.getRuleId()).append("|");
            }
            byte[] hash = digest.digest(sb.toString().getBytes());
            return toHexString(hash);
        } catch (Exception e) {
            log.warn("Failed to hash findings: {}", e.getMessage());
            return "";
        }
    }

    private String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
