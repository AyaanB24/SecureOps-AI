package com.secureops.finding;

/**
 * FILE: src/main/java/com/secureops/finding/Severity.java
 * PURPOSE: Enum representing vulnerability severity levels (tool-independent).
 * WHY IT EXISTS: Normalizes severity from different security tools into consistent values.
 * DEPENDENCIES: Used by Finding entity and FingerprintService.
 */
public enum Severity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW
}
