package com.secureops.finding.dto;

import com.secureops.finding.Severity;
import com.secureops.finding.FindingStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/finding/dto/FindingResponse.java
 * PURPOSE: DTO for sending finding data to REST API clients.
 * WHY IT EXISTS: Prevents exposing internal entity structure; provides clean API contract.
 * DEPENDENCIES: Maps to Finding entity fields. Used by FindingController.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FindingResponse {

    private UUID id;
    private UUID scanId;
    private UUID reportId;
    private String tool;
    private String ruleId;
    private String title;
    private String description;
    private Severity severity;
    private String filePath;
    private Integer lineNumber;
    private String packageName;
    private String packageVersion;
    private String fingerprint;
    private FindingStatus status;
    private LocalDateTime createdAt;

}
