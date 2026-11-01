package com.secureops.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/ai/AiAnalysisSummaryRepository.java
 * PURPOSE: Spring Data JPA repository for AiAnalysisSummary persistence.
 * WHY IT EXISTS: Provides database access for AI analysis summaries.
 */
@Repository
public interface AiAnalysisSummaryRepository extends JpaRepository<AiAnalysisSummary, UUID> {
    Optional<AiAnalysisSummary> findByScanId(UUID scanId);
}
