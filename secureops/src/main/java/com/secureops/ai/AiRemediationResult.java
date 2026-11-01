package com.secureops.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FILE: src/main/java/com/secureops/ai/AiRemediationResult.java
 * PURPOSE: DTO for Gemini API response (finding remediation).
 * WHY IT EXISTS: Structured representation of AI-generated finding explanation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRemediationResult {
    private String findingId;
    private String status; // SUCCESS or FAILED
    private String whatItMeans;
    private String whyDangerous;
    private String impactDescription;
    private String remediationSuggestion;
    private String versionSuggestion;
    private String configurationSuggestion;
    private String model;
    private String error;
}
