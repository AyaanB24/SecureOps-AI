package com.secureops.report;

/**
 * FILE: src/main/java/com/secureops/report/ReportTool.java
 * PURPOSE: Enum representing supported security analysis tools.
 * WHY IT EXISTS: Normalizes tool names and ensures type-safety for report sources.
 * DEPENDENCIES: Used by Report entity and ReportService.
 * FUTURE: Will support Semgrep and OWASP_DEPENDENCY_CHECK parsing in later phases.
 */
public enum ReportTool {
    TRIVY,
    SEMGREP,
    OWASP_DEPENDENCY_CHECK
}
