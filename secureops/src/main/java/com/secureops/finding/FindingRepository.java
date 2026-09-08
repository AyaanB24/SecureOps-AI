package com.secureops.finding;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/finding/FindingRepository.java
 * PURPOSE: Spring Data JPA repository for Finding entity CRUD operations.
 * WHY IT EXISTS: Provides database access layer; handles SQL queries automatically via Spring Data.
 * DEPENDENCIES: Extends JpaRepository for standard CRUD methods. Used by FindingService.
 */
@Repository
public interface FindingRepository extends JpaRepository<Finding, UUID> {

    /**
     * Find all findings for a specific scan.
     *
     * @param scanId UUID of the scan
     * @return List of findings for that scan
     */
    List<Finding> findByScanId(UUID scanId);

    /**
     * Find a finding by its fingerprint (for deduplication).
     *
     * @param fingerprint Fingerprint hash
     * @return Optional containing the finding if found
     */
    Optional<Finding> findByFingerprint(String fingerprint);

    /**
     * Find findings by report and severity (for filtering/querying).
     *
     * @param reportId UUID of the report
     * @param severity Severity level
     * @return List of findings matching criteria
     */
    List<Finding> findByReportIdAndSeverity(UUID reportId, Severity severity);

}
