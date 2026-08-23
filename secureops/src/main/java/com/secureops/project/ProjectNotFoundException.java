package com.secureops.project;

/**
 * FILE: src/main/java/com/secureops/project/ProjectNotFoundException.java
 * PURPOSE: Exception thrown when a requested project does not exist.
 * WHY IT EXISTS: Provides clean error handling for missing projects; converts to HTTP 404.
 * DEPENDENCIES: Thrown by ProjectService; caught by @ExceptionHandler in controller.
 */
public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(String projectId) {
        super("Project not found: " + projectId);
    }

    public ProjectNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
