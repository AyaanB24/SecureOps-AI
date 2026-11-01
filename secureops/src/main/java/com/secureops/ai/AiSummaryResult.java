package com.secureops.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FILE: src/main/java/com/secureops/ai/AiSummaryResult.java
 * PURPOSE: DTO for Gemini API response (scan summary).
 * WHY IT EXISTS: Structured representation of AI-generated scan analysis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSummaryResult {
    private String scanId;
    private String status; // SUCCESS or FAILED
    private String overallAssessment;
    private String topThreeRisks;
    private String keyConcerns;
    private String recommendedActions;
    private Boolean hasSignificantConcerns;
    private String model;
    private String error;
}
