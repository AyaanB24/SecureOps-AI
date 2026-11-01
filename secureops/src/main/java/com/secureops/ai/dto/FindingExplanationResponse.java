package com.secureops.ai.dto;

import com.secureops.ai.FindingExplanation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/ai/dto/FindingExplanationResponse.java
 * PURPOSE: DTO for returning finding explanation and remediation to frontend.
 * WHY IT EXISTS: Provides structured JSON response for finding-level AI analysis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindingExplanationResponse {
    private UUID id;
    private UUID findingId;
    private String whatItMeans;
    private String whyDangerous;
    private String impactDescription;
    private String remediationSuggestion;
    private String versionSuggestion;
    private String configurationSuggestion;
    private String generationStatus;
    private String errorMessage;
    private String aiModel;
    private LocalDateTime createdAt;
    private LocalDateTime regeneratedAt;

    public static FindingExplanationResponse fromEntity(FindingExplanation entity) {
        if (entity == null) {
            return null;
        }
        return FindingExplanationResponse.builder()
            .id(entity.getId())
            .findingId(entity.getFinding().getId())
            .whatItMeans(entity.getWhatItMeans())
            .whyDangerous(entity.getWhyDangerous())
            .impactDescription(entity.getImpactDescription())
            .remediationSuggestion(entity.getRemediationSuggestion())
            .versionSuggestion(entity.getVersionSuggestion())
            .configurationSuggestion(entity.getConfigurationSuggestion())
            .generationStatus(entity.getGenerationStatus())
            .errorMessage(entity.getErrorMessage())
            .aiModel(entity.getAiModel())
            .createdAt(entity.getCreatedAt())
            .regeneratedAt(entity.getRegeneratedAt())
            .build();
    }
}
