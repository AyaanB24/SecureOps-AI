package com.secureops.common.exception;

/**
 * FILE: src/main/java/com/secureops/common/exception/AccessDeniedException.java
 * PURPOSE: Exception thrown when user lacks authorization for operation.
 * WHY IT EXISTS: Distinguishes authorization failures (user is authenticated but not permitted) from authentication failures (no token).
 * HTTP MAPPING: 403 Forbidden
 */
public class AccessDeniedException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "You do not have permission to access this resource";

    public AccessDeniedException(String message) {
        super(message);
    }

    public AccessDeniedException() {
        super(DEFAULT_MESSAGE);
    }
}
