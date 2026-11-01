package com.secureops.ai.dto;

import com.secureops.ai.AiAnalysisSummary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/ai/dto/AiAnalysisSummaryResponse.java
 * PURPOSE: DTO for returning AI analysis summary to frontend.
 * WHY IT EXISTS: Provides structured JSON response for scan-level AI analysis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAnalysisSummaryResponse {
    private UUID id;
    private UUID scanId;
    private String overallAssessment;
    private Long criticalCount;
    private Long highCount;
    private Long mediumCount;
    private Long lowCount;
    private Long totalFindings;
    private String topThreeRisks;
    private String keyConcerns;
    private String recommendedActions;
    private Boolean hasSignificantConcerns;
    private String generationStatus;
    private String errorMessage;
    private String aiModel;
    private LocalDateTime createdAt;
    private LocalDateTime regeneratedAt;

    public static AiAnalysisSummaryResponse fromEntity(AiAnalysisSummary entity) {
        if (entity == null) {
            return null;
        }
        return AiAnalysisSummaryResponse.builder()
            .id(entity.getId())
            .scanId(entity.getScan().getId())
            .overallAssessment(entity.getOverallAssessment())
            .criticalCount(entity.getCriticalCount())
            .highCount(entity.getHighCount())
            .mediumCount(entity.getMediumCount())
            .lowCount(entity.getLowCount())
            .totalFindings(entity.getTotalFindings())
            .topThreeRisks(entity.getTopThreeRisks())
            .keyConcerns(entity.getKeyConcerns())
            .recommendedActions(entity.getRecommendedActions())
            .hasSignificantConcerns(entity.getHasSignificantConcerns())
            .generationStatus(entity.getGenerationStatus())
            .errorMessage(entity.getErrorMessage())
            .aiModel(entity.getAiModel())
            .createdAt(entity.getCreatedAt())
            .regeneratedAt(entity.getRegeneratedAt())
            .build();
    }
}
