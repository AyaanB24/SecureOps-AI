package com.secureops.decision;

/**
 * FILE: src/main/java/com/secureops/decision/Decision.java
 * PURPOSE: Enum for security decision outcomes.
 * WHY IT EXISTS: Represents the final verdict on whether a scan passes security policy.
 * DEPENDENCIES: Used by SecurityDecision entity.
 * 
 * DECISION VALUES:
 * - PASS: Scan meets all policy requirements
 * - BLOCK: Scan violates one or more policy requirements
 */
public enum Decision {
    PASS,
    BLOCK
}
