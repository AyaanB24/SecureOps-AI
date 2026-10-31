package com.secureops.auth.exception;

/**
 * FILE: src/main/java/com/secureops/auth/exception/InvalidCredentialsException.java
 * PURPOSE: Exception thrown when login credentials are invalid or user not found.
 * WHY IT EXISTS: Provides specific exception for authentication failure; handled by GlobalExceptionHandler.
 * DEPENDENCIES: Caught by GlobalExceptionHandler; converted to HTTP 401 Unauthorized.
 *
 * SECURITY NOTE: Does not reveal whether email exists or password is wrong (prevents user enumeration).
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }

    public InvalidCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}
