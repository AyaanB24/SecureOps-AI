package com.secureops.policy;

/**
 * FILE: src/main/java/com/secureops/policy/PolicyNotFoundException.java
 * PURPOSE: Exception thrown when a requested policy is not found.
 * WHY IT EXISTS: Enables proper error handling and HTTP 404 responses.
 * DEPENDENCIES: Thrown by PolicyService and PolicyController.
 */
public class PolicyNotFoundException extends RuntimeException {

    public PolicyNotFoundException(String message) {
        super(message);
    }

    public PolicyNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
