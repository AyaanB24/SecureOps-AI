package com.secureops.finding.dto;

import com.secureops.finding.Severity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * FILE: src/main/java/com/secureops/finding/dto/CreateFindingRequest.java
 * PURPOSE: DTO for receiving finding creation requests from REST API clients.
 * WHY IT EXISTS: Provides structured input validation for finding creation endpoint.
 * DEPENDENCIES: Used by FindingController to parse and validate request body.
 * 
 * REQUIRED FIELDS:
 * - tool: Security tool identifier (TRIVY, SEMGREP, OWASP_DEPENDENCY_CHECK)
 * - ruleId: Rule or CVE identifier from tool
 * - title: Human-readable finding title
 * - description: Detailed description of the finding
 * - severity: Finding severity (CRITICAL, HIGH, MEDIUM, LOW)
 * 
 * OPTIONAL FIELDS (null-safe):
 * - filePath: Source file path (for SAST findings)
 * - lineNumber: Source line number (for SAST findings)
 * - packageName: Package/library name (for dependency findings)
 * - packageVersion: Package version (for dependency findings)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateFindingRequest {

    @NotBlank(message = "Tool is required")
    private String tool;

    @NotBlank(message = "Rule ID is required")
    private String ruleId;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Severity is required")
    private Severity severity;

    private String filePath;
    private Integer lineNumber;
    private String packageName;
    private String packageVersion;

}
