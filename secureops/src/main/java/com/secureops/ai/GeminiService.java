package com.secureops.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * FILE: src/main/java/com/secureops/ai/GeminiService.java
 * PURPOSE: Integration with Google Gemini API for AI analysis.
 * WHY IT EXISTS: Centralizes Gemini API calls for security analysis generation.
 * SECURITY: API key stored in environment variables, never exposed to frontend.
 * CONFIGURATION: Model and version easily configurable via environment.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.api.model:gemini-1.5-flash}")
    private String geminiModel;

    @Value("${gemini.api.base-url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String geminiBaseUrl;

    /**
     * Generate AI security summary for a scan based on its findings.
     * 
     * @param scanId Unique scan identifier
     * @param projectName Name of the project
     * @param buildNumber Build/scan number (as string)
     * @param findingsSummary JSON string describing findings (counts, severities, etc.)
     * @return AiSummaryResult with generated analysis or error details
     */
    public AiSummaryResult generateScanSummary(String scanId, String projectName, String buildNumber, String findingsSummary) {
        log.debug("Generating AI summary for scan: {}", scanId);

        if (!isConfigured()) {
            return AiSummaryResult.builder()
                .scanId(scanId)
                .status("FAILED")
                .error("Gemini API key not configured. Set GEMINI_API_KEY environment variable.")
                .build();
        }

        try {
            String prompt = buildScanSummaryPrompt(projectName, buildNumber, findingsSummary);
            String responseText = callGeminiApi(prompt);
            AiSummaryResult result = parseGeminiResponse(responseText, scanId);
            return result;
        } catch (Exception e) {
            log.error("Error generating scan summary: {}", e.getMessage(), e);
            return AiSummaryResult.builder()
                .scanId(scanId)
                .status("FAILED")
                .error("Failed to generate summary: " + e.getMessage())
                .build();
        }
    }

    /**
     * Generate AI explanation and remediation for a finding.
     * 
     * @param findingId Unique finding identifier
     * @param findingDetails JSON string with finding info (title, severity, package, CVE, etc.)
     * @return AiRemediationResult with explanation and suggestions or error details
     */
    public AiRemediationResult generateFindingRemediation(String findingId, String findingDetails) {
        log.debug("Generating remediation for finding: {}", findingId);

        if (!isConfigured()) {
            return AiRemediationResult.builder()
                .findingId(findingId)
                .status("FAILED")
                .error("Gemini API key not configured. Set GEMINI_API_KEY environment variable.")
                .build();
        }

        try {
            String prompt = buildFindingRemediationPrompt(findingDetails);
            String responseText = callGeminiApi(prompt);
            AiRemediationResult result = parseRemediationResponse(responseText, findingId);
            return result;
        } catch (Exception e) {
            log.error("Error generating finding remediation: {}", e.getMessage(), e);
            return AiRemediationResult.builder()
                .findingId(findingId)
                .status("FAILED")
                .error("Failed to generate remediation: " + e.getMessage())
                .build();
        }
    }

    /**
     * Call Gemini API with the given prompt.
     * 
     * @param prompt The natural language prompt
     * @return API response text content
     */
    private String callGeminiApi(String prompt) throws Exception {
        String url = String.format("%s/%s:generateContent?key=%s", geminiBaseUrl, geminiModel, geminiApiKey);
        
        // Build request body
        Map<String, Object> requestBody = new HashMap<>();
        Map<String, String> part = new LinkedHashMap<>();
        part.put("text", prompt);
        
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("parts", new Object[] { part });
        requestBody.put("contents", new Object[] { content });

        // Configure request headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

        log.debug("Calling Gemini API with prompt length: {}", prompt.length());
        
        String response = restTemplate.postForObject(url, request, String.class);
        return response;
    }

    /**
     * Parse Gemini API response for scan summary.
     * 
     * Expected response JSON structure:
     * {
     *   "candidates": [{
     *     "content": {
     *       "parts": [{"text": "..."}]
     *     }
     *   }]
     * }
     */
    private AiSummaryResult parseGeminiResponse(String responseText, String scanId) throws Exception {
        JsonNode root = objectMapper.readTree(responseText);
        JsonNode candidates = root.get("candidates");
        
        if (candidates == null || candidates.size() == 0) {
            throw new IllegalArgumentException("No candidates in Gemini response");
        }

        JsonNode content = candidates.get(0).get("content");
        JsonNode parts = content.get("parts");
        String text = parts.get(0).get("text").asText();

        // Parse JSON response from AI
        JsonNode analysisJson = objectMapper.readTree(text);

        return AiSummaryResult.builder()
            .scanId(scanId)
            .status("SUCCESS")
            .overallAssessment(analysisJson.get("overall_assessment").asText(""))
            .topThreeRisks(analysisJson.get("top_three_risks").asText(""))
            .keyConcerns(analysisJson.get("key_concerns").asText(""))
            .recommendedActions(analysisJson.get("recommended_actions").asText(""))
            .hasSignificantConcerns(analysisJson.get("has_significant_concerns").asBoolean(false))
            .model(geminiModel)
            .build();
    }

    /**
     * Parse Gemini API response for finding remediation.
     */
    private AiRemediationResult parseRemediationResponse(String responseText, String findingId) throws Exception {
        JsonNode root = objectMapper.readTree(responseText);
        JsonNode candidates = root.get("candidates");
        
        if (candidates == null || candidates.size() == 0) {
            throw new IllegalArgumentException("No candidates in Gemini response");
        }

        JsonNode content = candidates.get(0).get("content");
        JsonNode parts = content.get("parts");
        String text = parts.get(0).get("text").asText();

        // Parse JSON response from AI
        JsonNode remediationJson = objectMapper.readTree(text);

        return AiRemediationResult.builder()
            .findingId(findingId)
            .status("SUCCESS")
            .whatItMeans(remediationJson.get("what_it_means").asText(""))
            .whyDangerous(remediationJson.get("why_dangerous").asText(""))
            .impactDescription(remediationJson.get("impact_description").asText(""))
            .remediationSuggestion(remediationJson.get("remediation_suggestion").asText(""))
            .versionSuggestion(remediationJson.get("version_suggestion").asText(""))
            .configurationSuggestion(remediationJson.get("configuration_suggestion").asText(""))
            .model(geminiModel)
            .build();
    }

    /**
     * Build prompt for scan-level AI analysis.
     */
    private String buildScanSummaryPrompt(String projectName, String buildNumber, String findingsSummary) {
        return String.format("""
You are a security expert analyzing vulnerability scan results. Analyze the following security scan and provide a structured JSON response.

Project: %s
Build: #%s

Findings Summary:
%s

Provide your analysis as valid JSON (no markdown, just JSON) with these exact fields:
{
  "overall_assessment": "Brief overall security assessment (1-2 sentences)",
  "top_three_risks": "Numbered list of the 3 most important risks to address (or fewer if <3 findings)",
  "key_concerns": "Explanation of why these are the top concerns",
  "recommended_actions": "Specific next steps to improve security",
  "has_significant_concerns": true/false (true if any CRITICAL or HIGH findings)
}

Only respond with valid JSON, no additional text.
""", projectName, buildNumber, findingsSummary);
    }

    /**
     * Build prompt for finding-level AI remediation.
     */
    private String buildFindingRemediationPrompt(String findingDetails) {
        return String.format("""
You are a security expert. Provide clear, practical guidance for this vulnerability. Respond with valid JSON (no markdown) with these exact fields:

{
  "what_it_means": "Plain English explanation of what this vulnerability is (1-2 sentences)",
  "why_dangerous": "Why this vulnerability matters and what risks it poses (2-3 sentences)",
  "impact_description": "Potential business/technical impact if exploited (2-3 sentences)",
  "remediation_suggestion": "Specific, actionable steps to fix this vulnerability",
  "version_suggestion": "Recommended package/component version (if applicable, otherwise empty string)",
  "configuration_suggestion": "Recommended config changes or code patterns (if applicable, otherwise empty string)"
}

Vulnerability Details:
%s

Only respond with valid JSON, no additional text or explanation.
""", findingDetails);
    }

    /**
     * Check if Gemini API is configured.
     */
    public boolean isConfigured() {
        return geminiApiKey != null && !geminiApiKey.isEmpty() && !geminiApiKey.equals("");
    }

    /**
     * Get configuration status for frontend display.
     */
    public String getConfigurationStatus() {
        if (isConfigured()) {
            return "Configured";
        }
        return "Not configured - Set GEMINI_API_KEY environment variable";
    }
}
