package com.secureops.scan;

/**
 * FILE: src/main/java/com/secureops/scan/ScanNotFoundException.java
 * PURPOSE: Custom exception thrown when a scan is not found.
 * WHY IT EXISTS: Allows ScanService to signal not-found errors which GlobalExceptionHandler converts to HTTP 404.
 * DEPENDENCIES: Used by ScanService and GlobalExceptionHandler.
 */
public class ScanNotFoundException extends RuntimeException {

    public ScanNotFoundException(String scanId) {
        super("Scan not found: " + scanId);
    }

    public ScanNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
