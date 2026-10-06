package com.secureops.report.parser.trivy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

/**
 * FILE: src/main/java/com/secureops/report/parser/trivy/TrivyReport.java
 * PURPOSE: Jackson POJO for deserializing Trivy JSON report.
 * WHY IT EXISTS: Maps entire Trivy report structure including all results.
 * DEPENDENCIES: Used by TrivyParser as root object for report deserialization.
 * 
 * TRIVY REPORT STRUCTURE:
 * {
 *   "SchemaVersion": 2,
 *   "ArtifactName": "payment-api:latest",
 *   "ArtifactType": "container_image",
 *   "Metadata": { ... },
 *   "Results": [ ... ]
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrivyReport {

    @JsonProperty("SchemaVersion")
    private Integer schemaVersion;

    @JsonProperty("ArtifactName")
    private String artifactName;

    @JsonProperty("ArtifactType")
    private String artifactType;

    @JsonProperty("Metadata")
    private Object metadata;

    @JsonProperty("Results")
    private List<TrivyResult> results;

}
