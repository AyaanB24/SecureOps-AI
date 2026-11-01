package com.secureops.ai;

/**
 * FILE: src/main/java/com/secureops/ai/FindingNotFoundException.java
 * PURPOSE: Exception thrown when finding is not found.
 * WHY IT EXISTS: Provides meaningful error messages for missing findings.
 */
public class FindingNotFoundException extends RuntimeException {
    public FindingNotFoundException(String findingId) {
        super("Finding not found: " + findingId);
    }
}
