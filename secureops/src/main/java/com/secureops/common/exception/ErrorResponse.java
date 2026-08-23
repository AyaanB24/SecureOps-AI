package com.secureops.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * FILE: src/main/java/com/secureops/common/exception/ErrorResponse.java
 * PURPOSE: Standardized error response DTO for all API errors.
 * WHY IT EXISTS: Ensures consistent error format across all endpoints; improves client error handling.
 * DEPENDENCIES: Used by GlobalExceptionHandler to serialize errors to JSON.
 */
@Getter
@AllArgsConstructor
public class ErrorResponse {

    private String code;

    private String message;

}
