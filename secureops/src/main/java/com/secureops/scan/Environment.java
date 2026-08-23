package com.secureops.scan;

/**
 * FILE: src/main/java/com/secureops/scan/Environment.java
 * PURPOSE: Enum representing deployment environments where scans execute.
 * WHY IT EXISTS: Normalizes environment names and ensures type-safety for scan environments.
 * DEPENDENCIES: Used by Scan entity and ScanService.
 */
public enum Environment {
    DEVELOPMENT,
    STAGING,
    PRODUCTION
}
