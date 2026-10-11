package com.secureops.decision;

/**
 * FILE: src/main/java/com/secureops/decision/SecurityDecisionNotFoundException.java
 * PURPOSE: Exception thrown when a requested security decision is not found.
 * WHY IT EXISTS: Enables proper error handling and HTTP 404 responses.
 * DEPENDENCIES: Thrown by DecisionService and DecisionController.
 */
public class SecurityDecisionNotFoundException extends RuntimeException {

    public SecurityDecisionNotFoundException(String message) {
        super(message);
    }

    public SecurityDecisionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
