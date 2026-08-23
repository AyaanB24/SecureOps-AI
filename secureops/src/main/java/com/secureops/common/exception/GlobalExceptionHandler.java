package com.secureops.common.exception;

import com.secureops.project.ProjectNotFoundException;
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
