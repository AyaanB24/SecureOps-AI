package com.secureops.ai;

/**
 * FILE: src/main/java/com/secureops/ai/ScanNotFoundException.java
 * PURPOSE: Exception thrown when scan is not found.
 * WHY IT EXISTS: Provides meaningful error messages for missing scans.
 */
public class ScanNotFoundException extends RuntimeException {
    public ScanNotFoundException(String scanId) {
        super("Scan not found: " + scanId);
    }
}
