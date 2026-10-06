package com.secureops.report.parser;

import com.secureops.finding.Finding;
import java.io.IOException;
import java.util.List;

/**
 * FILE: src/main/java/com/secureops/report/parser/SecurityReportParser.java
 * PURPOSE: Interface for parsing security tool reports into normalized findings.
 * WHY IT EXISTS: Enables pluggable report parsers for different tools (Trivy, Semgrep, OWASP).
 * DEPENDENCIES: Takes report file content and Scan/Report entities, returns Finding objects.
 * 
 * PARSER RESPONSIBILITY:
 * - Read security tool's report format (JSON, XML, etc.)
 * - Normalize tool-specific fields to Finding model
 * - Handle missing/optional fields gracefully
 * - Generate deterministic fingerprints
 * - Never crash application due to malformed data
 * - Track parsing errors/warnings for debugging
 */
public interface SecurityReportParser {

    /**
     * Parse security report and generate findings.
     * 
     * @param reportContent Raw report file content (JSON, XML, etc.)
     * @param scanId ID of the scan this report belongs to
     * @param reportId ID of the report entity
     * @return List of Finding objects (empty if no vulnerabilities found)
     * @throws IOException if file reading fails
     * @throws com.secureops.report.parser.ReportParsingException if critical parsing error
     */
    List<Finding> parse(String reportContent, java.util.UUID scanId, java.util.UUID reportId) throws IOException;

    /**
     * Check if parser can handle report.
     * Used to route reports to correct parser implementation.
     * 
     * @param reportContent Raw report content
     * @return true if parser can handle this report format
     */
    boolean canParse(String reportContent);

}
