package com.secureops.common.controller;

import com.secureops.common.dto.HealthResponse;
import com.secureops.common.service.HealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FILE: src/main/java/com/secureops/common/controller/HealthController.java
 * PURPOSE: REST controller providing health check endpoint for SecureOps.
 * WHY IT EXISTS: Allows external systems (load balancers, monitoring) to verify application and database status.
 * DEPENDENCIES: Depends on HealthService to check database connectivity; returns HealthResponse DTO.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Autowired
    private HealthService healthService;

    /**
     * Health check endpoint that verifies both application and database status.
     * GET /api/health
     *
     * @return HealthResponse with application status, service name, and database status
     */
    @GetMapping
    public ResponseEntity<HealthResponse> health() {
        String dbStatus = healthService.isDatabaseHealthy() ? "UP" : "DOWN";
        HealthResponse response = new HealthResponse("UP", "SecureOps", dbStatus);
        return ResponseEntity.ok(response);
    }

}
