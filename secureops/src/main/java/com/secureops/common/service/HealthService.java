package com.secureops.common.service;

import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FILE: src/main/java/com/secureops/common/service/HealthService.java
 * PURPOSE: Service to check application and database health status.
 * WHY IT EXISTS: Encapsulates database connectivity check logic; separates health checks from the controller.
 * DEPENDENCIES: Uses JdbcTemplate from Spring Data JDBC to execute a simple SQL query against PostgreSQL.
 */
@Service
public class HealthService {

    private static final Logger logger = LoggerFactory.getLogger(HealthService.class);

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    /**
     * Check if the application and database are operational.
     * For Phase 0: Always returns true (database connectivity check deferred to Phase 1).
     *
     * @return true if database is reachable; false otherwise
     */
    public boolean isDatabaseHealthy() {
        // Phase 0: Defer actual database connectivity check
        // This allows the application to start without PostgreSQL running
        // In Phase 1, implement proper database health checking
        logger.debug("Database health check: Deferred for Phase 0");
        return true;
    }

}
