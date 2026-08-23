package com.secureops.project;

/**
 * FILE: src/main/java/com/secureops/project/DuplicateProjectException.java
 * PURPOSE: Custom exception thrown when attempting to create a project with a duplicate name.
 * WHY IT EXISTS: Allows ProjectService to signal duplicate name errors which GlobalExceptionHandler converts to HTTP 409 Conflict.
 * DEPENDENCIES: Used by ProjectService and GlobalExceptionHandler.
 */
public class DuplicateProjectException extends RuntimeException {

    public DuplicateProjectException(String projectName) {
        super("Project with name '" + projectName + "' already exists");
    }

    public DuplicateProjectException(String message, Throwable cause) {
        super(message, cause);
    }

}
