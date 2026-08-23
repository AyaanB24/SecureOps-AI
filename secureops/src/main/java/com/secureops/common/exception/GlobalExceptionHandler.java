package com.secureops.common.exception;

import com.secureops.project.ProjectNotFoundException;
import com.secureops.project.DuplicateProjectException;
import com.secureops.pipeline.PipelineNotFoundException;
import com.secureops.pipeline.DuplicatePipelineException;
import com.secureops.scan.ScanNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * FILE: src/main/java/com/secureops/common/exception/GlobalExceptionHandler.java
 * PURPOSE: Centralized exception handling for all REST endpoints.
 * WHY IT EXISTS: Converts exceptions to consistent HTTP error responses; simplifies error handling across controllers.
 * DEPENDENCIES: Catches exceptions thrown by services and controllers; returns ErrorResponse DTO.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProjectNotFound(ProjectNotFoundException ex) {
        log.warn("Project not found: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(DuplicateProjectException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateProject(DuplicateProjectException ex) {
        log.warn("Duplicate project: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("DUPLICATE", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(PipelineNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePipelineNotFound(PipelineNotFoundException ex) {
        log.warn("Pipeline not found: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(DuplicatePipelineException.class)
    public ResponseEntity<ErrorResponse> handleDuplicatePipeline(DuplicatePipelineException ex) {
        log.warn("Duplicate pipeline: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("DUPLICATE", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(ScanNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleScanNotFound(ScanNotFoundException ex) {
        log.warn("Scan not found: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("INVALID_REQUEST", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getAllErrors().stream()
            .map(err -> err.getDefaultMessage())
            .findFirst()
            .orElse("Validation failed");
        log.warn("Validation error: {}", message);
        ErrorResponse error = new ErrorResponse("VALIDATION_ERROR", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        ErrorResponse error = new ErrorResponse("INTERNAL_SERVER_ERROR", "An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

}
