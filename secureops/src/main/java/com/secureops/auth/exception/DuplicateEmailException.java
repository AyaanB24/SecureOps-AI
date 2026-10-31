package com.secureops.auth.exception;

/**
 * FILE: src/main/java/com/secureops/auth/exception/DuplicateEmailException.java
 * PURPOSE: Exception thrown when user attempts to register with an already-registered email.
 * WHY IT EXISTS: Provides specific exception for duplicate email detection; handled by GlobalExceptionHandler.
 * DEPENDENCIES: Caught by GlobalExceptionHandler; converted to HTTP 409 Conflict.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String message) {
        super(message);
    }

    public DuplicateEmailException(String message, Throwable cause) {
        super(message, cause);
    }
}
