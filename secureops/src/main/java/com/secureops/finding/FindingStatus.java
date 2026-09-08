package com.secureops.finding;

/**
 * FILE: src/main/java/com/secureops/finding/FindingStatus.java
 * PURPOSE: Enum representing finding remediation status.
 * WHY IT EXISTS: Tracks whether a vulnerability has been addressed or is still open.
 * DEPENDENCIES: Used by Finding entity.
 */
public enum FindingStatus {
    OPEN,      // Vulnerability is active and unresolved
    RESOLVED   // Vulnerability has been fixed or remediated
}
