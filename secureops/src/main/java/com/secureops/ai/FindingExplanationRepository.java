package com.secureops.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/ai/FindingExplanationRepository.java
 * PURPOSE: Spring Data JPA repository for FindingExplanation persistence.
 * WHY IT EXISTS: Provides database access for finding explanations and remediations.
 */
@Repository
public interface FindingExplanationRepository extends JpaRepository<FindingExplanation, UUID> {
    Optional<FindingExplanation> findByFindingId(UUID findingId);
}
