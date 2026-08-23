package com.secureops.pipeline;

/**
 * FILE: src/main/java/com/secureops/pipeline/DuplicatePipelineException.java
 * PURPOSE: Custom exception thrown when attempting to create a pipeline with duplicate job name + build number for a project.
 * WHY IT EXISTS: Allows PipelineService to signal duplicate pipeline errors which GlobalExceptionHandler converts to HTTP 409 Conflict.
 * DEPENDENCIES: Used by PipelineService and GlobalExceptionHandler.
 */
public class DuplicatePipelineException extends RuntimeException {

    public DuplicatePipelineException(String jobName, Long buildNumber) {
        super("Pipeline with job name '" + jobName + "' and build number " + buildNumber + " already exists for this project");
    }

    public DuplicatePipelineException(String message, Throwable cause) {
        super(message, cause);
    }

}
