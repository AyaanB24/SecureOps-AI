package com.secureops.report;

import com.secureops.finding.Finding;
import com.secureops.finding.FindingRepository;
import com.secureops.report.parser.SecurityReportParser;
import com.secureops.report.parser.trivy.TrivyParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/report/ReportProcessingService.java
 * PURPOSE: Orchestrate report parsing and finding creation.
 * WHY IT EXISTS: Bridges gap between report upload and finding extraction.
 * DEPENDENCIES: Uses SecurityReportParser implementations, FindingRepository.
 * 
 * PROCESSING FLOW:
 * 1. Load report file from filesystem (stored by ReportService)
 * 2. Select appropriate parser (currently Trivy)
 * 3. Parse report into findings list
 * 4. Persist findings to database
 * 5. Update report status to PROCESSED
 * 6. Return processing result with finding count
 * 
 * ERROR HANDLING:
 * - If file not found: Throws ReportNotFoundException
 * - If parsing fails: Updates report status to FAILED, logs error
 * - If finding creation fails: Continues with other findings
 */
@Service
@Slf4j
public class ReportProcessingService {

    private final TrivyParser trivyParser;
    private final FindingRepository findingRepository;
    private final ReportRepository reportRepository;

    public ReportProcessingService(TrivyParser trivyParser, FindingRepository findingRepository, ReportRepository reportRepository) {
        this.trivyParser = trivyParser;
        this.findingRepository = findingRepository;
        this.reportRepository = reportRepository;
    }

    /**
     * Process security report and create findings.
     * 
     * @param reportId UUID of the report
     * @return ReportProcessingResult with findings created count and status
     * @throws ReportNotFoundException if report not found
     */
    public ReportProcessingResult processReport(UUID reportId) {
        log.info("Starting report processing for reportId: {}", reportId);

        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new ReportNotFoundException(reportId.toString()));

        try {
            // Update status to PROCESSING
            report.setStatus(ReportStatus.PROCESSING);
            reportRepository.save(report);
            log.debug("Report status updated to PROCESSING");

            // Load report file from filesystem
            String reportContent = loadReportFile(report.getFilePath());

            // Parse report using appropriate parser
            List<Finding> findings = trivyParser.parse(reportContent, report.getScan().getId(), reportId);
            log.info("Parser returned {} findings", findings.size());

            // Persist findings to database
            List<Finding> savedFindings = findingRepository.saveAll(findings);
            log.info("Persisted {} findings to database", savedFindings.size());

            // Update report status to PROCESSED
            report.setStatus(ReportStatus.PROCESSED);
            reportRepository.save(report);
            log.info("Report processing completed successfully");

            return new ReportProcessingResult(reportId, report.getTool(), savedFindings.size(), ReportStatus.PROCESSED);

        } catch (Exception e) {
            log.error("Report processing failed for reportId: {}", reportId, e);
            
            // Update report status to FAILED
            report.setStatus(ReportStatus.FAILED);
            reportRepository.save(report);
            
            throw new RuntimeException("Failed to process report: " + e.getMessage(), e);
        }
    }

    /**
     * Load report file content from filesystem.
     * 
     * @param filePath Absolute file path
     * @return File content as string
     * @throws IOException if file reading fails
     */
    private String loadReportFile(String filePath) throws IOException {
        log.debug("Loading report file from: {}", filePath);
        byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
        String content = new String(fileBytes, StandardCharsets.UTF_8);
        log.debug("Loaded {} bytes from report file", fileBytes.length);
        return content;
    }

}
