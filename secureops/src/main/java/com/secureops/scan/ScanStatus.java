package com.secureops.scan;

/**
 * FILE: src/main/java/com/secureops/scan/ScanStatus.java
 * PURPOSE: Enum representing scan execution status.
 * WHY IT EXISTS: Normalizes scan status values and enables type-safe status transitions.
 * DEPENDENCIES: Used by Scan entity and ScanService.
 */
public enum ScanStatus {
    CREATED,
    PROCESSING,
    COMPLETED,
    FAILED
}
