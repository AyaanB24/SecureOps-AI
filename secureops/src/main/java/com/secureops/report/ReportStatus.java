package com.secureops.report;

/**
 * FILE: src/main/java/com/secureops/report/ReportStatus.java
 * PURPOSE: Enum representing report processing status.
 * WHY IT EXISTS: Tracks report lifecycle from ingestion to processing completion.
 * DEPENDENCIES: Used by Report entity and ReportService.
 */
public enum ReportStatus {
    RECEIVED,      // Report file uploaded, awaiting processing
    PROCESSING,    // Report being parsed and findings extracted
    PROCESSED,     // Report successfully parsed and findings stored
    FAILED         // Report processing failed
}
