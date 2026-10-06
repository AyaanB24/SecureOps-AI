package com.secureops.report.parser.trivy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

/**
 * FILE: src/main/java/com/secureops/report/parser/trivy/TrivyResult.java
 * PURPOSE: Jackson POJO for deserializing Trivy result (scan target) JSON.
 * WHY IT EXISTS: Maps one scan target's results including vulnerabilities.
 * DEPENDENCIES: Used by TrivyParser to read result data.
 * 
 * TRIVY RESULT STRUCTURE:
 * {
 *   "Target": "python:3.9-slim (ubuntu 20.04)",
 *   "Class": "os-pkgs",
 *   "Type": "ubuntu",
 *   "Vulnerabilities": [ ... ]
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrivyResult {

    @JsonProperty("Target")
    private String target;
    
    @JsonProperty("Class")
    private String resultClass;

    @JsonProperty("Type")
    private String type;
    
    @JsonProperty("Vulnerabilities")
    private List<TrivyVulnerability> vulnerabilities;

}
