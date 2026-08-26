package com.secureops.report;

import com.secureops.report.dto.ReportResponse;
import com.secureops.scan.Scan;
import com.secureops.scan.ScanNotFoundException;
import com.secureops.scan.ScanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * FILE: src/main/java/com/secureops/report/ReportService.java
 * PURPOSE: Business logic for report management and file handling.
 * WHY IT EXISTS: Encapsulates CRUD operations, file storage, validation, and duplicate prevention.
 * DEPENDENCIES: Uses ReportRepository and ScanRepository for data access.
 * STORAGE: Uses local filesystem storage for MVP. Each report stored in dedicated directory.
 * 
 * FILESYSTEM STORAGE RATIONALE:
 * - MVP simplicity: No cloud provider setup required
 * - Development-friendly: Easy to inspect reports locally
 * - Cost-effective: No AWS S3 charges during development
 * - Future-proof: Can migrate to S3/cloud storage in Phase N without code changes
 *   (just swap storage layer implementation)
 * - Reports contain sensitive data: Keeping locally during MVP is acceptable
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;
    private final ScanRepository scanRepository;

    @Value("${secureops.reports.storage-path:./reports}")
    private String reportsStoragePath;

    /**
     * Upload and store a security report for a scan.
     *
     * @param scanId UUID of the scan
     * @param tool ReportTool enum representing the security tool
     * @param file MultipartFile containing the report
     * @return ReportResponse with created report
     * @throws ScanNotFoundException if scan does not exist
     * @throws IllegalArgumentException if validation fails
     * @throws IOException if file storage fails
     */
    public ReportResponse uploadReport(UUID scanId, ReportTool tool, MultipartFile file) throws IOException {
        log.info("Uploading report for scan: {} with tool: {}", scanId, tool);

        // Step 1: Validate file is present
        if (file == null || file.isEmpty()) {
            log.error("File is missing or empty");
            throw new IllegalArgumentException("Report file is required and cannot be empty");
        }

        // Step 2: Validate scan exists
        Scan scan = scanRepository.findById(scanId)
            .orElseThrow(() -> {
                log.error("Scan not found: {}", scanId);
                return new ScanNotFoundException(scanId.toString());
            });

        // Step 3: Validate tool is not null
        if (tool == null) {
            log.error("Tool is required");
            throw new IllegalArgumentException("Tool is required");
        }

        // Step 4: Check for duplicate report (one report per tool per scan)
        if (reportRepository.findByScanIdAndTool(scanId, tool).isPresent()) {
            log.warn("Duplicate report: scan={}, tool={}", scanId, tool);
            throw new IllegalArgumentException(
                "A report from " + tool + " already exists for this scan"
            );
        }

        // Step 5: Validate file extension and content type
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || !isValidReportFile(originalFileName, file.getContentType())) {
            log.error("Invalid file: {}, contentType: {}", originalFileName, file.getContentType());
            throw new IllegalArgumentException("Invalid report file. Supported formats: JSON");
        }

        // Step 6: Store file on filesystem
        String storedFilePath = storeReportFile(scanId, tool, file);

        // Step 7: Create and save report entity
        Report report = new Report(scan, tool, originalFileName, storedFilePath);
        Report saved = reportRepository.save(report);
        log.info("Report uploaded with ID: {} for scan: {}", saved.getId(), scanId);

        return ReportResponse.fromEntity(saved);
    }

    /**
     * Store report file on local filesystem.
     * Directory structure: ./reports/{scanId}/{tool}/
     *
     * @param scanId UUID of the scan
     * @param tool ReportTool enum
     * @param file MultipartFile to store
     * @return File path where report was stored
     * @throws IOException if storage fails
     */
    private String storeReportFile(UUID scanId, ReportTool tool, MultipartFile file) throws IOException {
        // Create directory structure: ./reports/{scanId}/{tool}/
        Path reportDir = Paths.get(reportsStoragePath, scanId.toString(), tool.toString());
        Files.createDirectories(reportDir);

        // Generate unique filename: {timestamp}_{originalFileName}
        String uniqueFileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = reportDir.resolve(uniqueFileName);

        // Store file
        Files.copy(file.getInputStream(), filePath);
        log.info("Report file stored at: {}", filePath.toAbsolutePath());

        return filePath.toAbsolutePath().toString();
    }

    /**
     * Validate report file format.
     * Currently supports JSON files (for all tools).
     *
     * @param fileName Name of the file
     * @param contentType MIME type of the file
     * @return true if file is valid, false otherwise
     */
    private boolean isValidReportFile(String fileName, String contentType) {
        // Accept JSON files
        if (fileName.endsWith(".json") || "application/json".equals(contentType)) {
            return true;
        }

        // Accept .txt files with JSON content
        if (fileName.endsWith(".txt") && (contentType == null || contentType.contains("text"))) {
            return true;
        }

        return false;
    }

    /**
     * Get all reports for a specific scan.
     *
     * @param scanId UUID of the scan
     * @return List of ReportResponse objects
     * @throws ScanNotFoundException if scan does not exist
     */
    public List<ReportResponse> getReportsByScan(UUID scanId) {
        log.info("Fetching reports for scan: {}", scanId);

        // Validate that scan exists
        if (!scanRepository.existsById(scanId)) {
            log.error("Scan not found: {}", scanId);
            throw new ScanNotFoundException(scanId.toString());
        }

        List<Report> reports = reportRepository.findByScanId(scanId);
        return reports.stream()
            .map(ReportResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Get a specific report by ID.
     *
     * @param reportId UUID of the report
     * @return ReportResponse
     * @throws ReportNotFoundException if report does not exist
     */
    public ReportResponse getReportById(UUID reportId) {
        log.info("Fetching report: {}", reportId);

        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> {
                log.error("Report not found: {}", reportId);
                return new ReportNotFoundException(reportId.toString());
            });

        return ReportResponse.fromEntity(report);
    }

}
