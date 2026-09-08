package com.secureops.finding;

/**
 * FILE: src/main/java/com/secureops/finding/FindingNotFoundException.java
 * PURPOSE: Exception thrown when a finding is not found by ID.
 * WHY IT EXISTS: Provides specific exception for finding lookup failures.
 */
public class FindingNotFoundException extends RuntimeException {

    public FindingNotFoundException(String message) {
        super(message);
    }

    public FindingNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
