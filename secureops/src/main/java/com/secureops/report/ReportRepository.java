package com.secureops.report;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/report/ReportRepository.java
 * PURPOSE: Spring Data JPA repository for Report entity CRUD operations.
 * WHY IT EXISTS: Provides database access layer; handles SQL queries automatically via Spring Data.
 * DEPENDENCIES: Extends JpaRepository for standard CRUD methods. Used by ReportService.
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

    /**
     * Find all reports for a specific scan.
     *
     * @param scanId UUID of the scan
     * @return List of reports for that scan
     */
    List<Report> findByScanId(UUID scanId);

    /**
     * Find a report by scan ID and tool (prevents duplicate reports from same tool per scan).
     *
     * @param scanId UUID of the scan
     * @param tool ReportTool enum value
     * @return Optional containing the report if found
     */
    Optional<Report> findByScanIdAndTool(UUID scanId, ReportTool tool);

}
