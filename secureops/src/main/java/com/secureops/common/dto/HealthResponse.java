package com.secureops.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FILE: src/main/java/com/secureops/common/dto/HealthResponse.java
 * PURPOSE: Data Transfer Object for health check endpoint response.
 * WHY IT EXISTS: Provides a structured, serializable JSON response for the /api/health endpoint.
 * DEPENDENCIES: Used by HealthController to serialize response; Jackson handles JSON conversion automatically.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class HealthResponse {

    private String status;
    private String service;
    private String database;

}
