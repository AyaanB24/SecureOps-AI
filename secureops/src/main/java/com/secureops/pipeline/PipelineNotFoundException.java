package com.secureops.pipeline;

/**
 * FILE: src/main/java/com/secureops/pipeline/PipelineNotFoundException.java
 * PURPOSE: Custom exception thrown when a pipeline is not found.
 * WHY IT EXISTS: Allows PipelineService to signal not-found errors which GlobalExceptionHandler converts to HTTP 404.
 * DEPENDENCIES: Used by PipelineService and GlobalExceptionHandler.
 */
public class PipelineNotFoundException extends RuntimeException {

    public PipelineNotFoundException(String pipelineId) {
        super("Pipeline not found: " + pipelineId);
    }

    public PipelineNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
