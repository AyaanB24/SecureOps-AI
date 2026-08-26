package com.secureops.report;

/**
 * FILE: src/main/java/com/secureops/report/ReportNotFoundException.java
 * PURPOSE: Custom exception thrown when a report is not found.
 * WHY IT EXISTS: Allows ReportService to signal not-found errors which GlobalExceptionHandler converts to HTTP 404.
 * DEPENDENCIES: Used by ReportService and GlobalExceptionHandler.
 */
public class ReportNotFoundException extends RuntimeException {

    public ReportNotFoundException(String reportId) {
        super("Report not found: " + reportId);
    }

    public ReportNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
