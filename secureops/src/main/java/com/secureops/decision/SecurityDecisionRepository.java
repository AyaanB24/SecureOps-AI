package com.secureops.decision;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/decision/SecurityDecisionRepository.java
 * PURPOSE: Spring Data JPA repository for SecurityDecision CRUD operations.
 * WHY IT EXISTS: Provides data access layer for security decision entities.
 * DEPENDENCIES: Extends JpaRepository for automatic CRUD implementation.
 * 
 * CUSTOM QUERIES:
 * - findByScanId: Get decision for a specific scan
 */
@Repository
public interface SecurityDecisionRepository extends JpaRepository<SecurityDecision, UUID> {

    /**
     * Find security decision by scan ID.
     *
     * @param scanId UUID of the scan
     * @return Optional containing decision if found
     */
    Optional<SecurityDecision> findByScanId(UUID scanId);

}
