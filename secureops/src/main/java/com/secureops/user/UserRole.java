package com.secureops.user;

/**
 * FILE: src/main/java/com/secureops/user/UserRole.java
 * PURPOSE: Enum representing user roles for role-based authorization.
 * WHY IT EXISTS: Defines available roles in the system for authorization and permission checks.
 * DEPENDENCIES: Used by User entity.
 */
public enum UserRole {
    ADMIN,              // Administrator with full access
    USER,               // Regular user with project-level access
    SERVICE_ACCOUNT     // CI/CD service account with API key auth (restricted scope)
}
