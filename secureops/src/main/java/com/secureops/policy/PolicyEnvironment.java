package com.secureops.policy;

/**
 * FILE: src/main/java/com/secureops/policy/PolicyEnvironment.java
 * PURPOSE: Enum for policy environment types.
 * WHY IT EXISTS: Enables different policies per environment (e.g., Production has stricter rules than Development).
 * DEPENDENCIES: Used by Policy entity.
 * 
 * ENVIRONMENTS:
 * - DEVELOPMENT: Relaxed thresholds, fast iteration
 * - STAGING: Medium thresholds, pre-production validation
 * - PRODUCTION: Strict thresholds, zero-tolerance for critical issues
 * 
 * EXAMPLES:
 * Production policy: maxCritical=0 (zero tolerance for critical vulns in production)
 * Development policy: maxCritical=10 (allows more issues during development)
 */
public enum PolicyEnvironment {
    DEVELOPMENT,
    STAGING,
    PRODUCTION
}
