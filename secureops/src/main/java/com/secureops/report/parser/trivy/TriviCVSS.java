package com.secureops.report.parser.trivy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Map;

/**
 * FILE: src/main/java/com/secureops/report/parser/trivy/TriviCVSS.java
 * PURPOSE: Jackson POJO for deserializing Trivy CVSS score data (object format).
 * WHY IT EXISTS: Modern Trivy reports include CVSS as an object with nested vendor/metrics data.
 * DEPENDENCIES: Used by TrivyVulnerability when parsing CVSS field.
 * 
 * TRIVY CVSS STRUCTURE:
 * Can be either:
 * 1. Simple numeric: "CVSS": 7.5
 * 2. Complex object with vendor data:
 *    "CVSS": {
 *      "nvd": {
 *        "V3Score": 7.5,
 *        "V3Vector": "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:N",
 *        "V2Score": 5.0,
 *        "V2Vector": "AV:N/AC:L/Au:N/C:P/I:N/A:N"
 *      },
 *      "redhat": {
 *        "V3Score": 8.8,
 *        "V3Vector": "CVSS:3.1/AV:N/AC:L/PR:L/UI:N/S:U/C:H/I:H/A:H"
 *      }
 *    }
 * 3. Null/missing: No CVSS data available
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TriviCVSS {

    @JsonProperty("nvd")
    private CVSSScore nvd;

    @JsonProperty("redhat")
    private CVSSScore redhat;

    @JsonProperty("ghsa")
    private CVSSScore ghsa;

    /**
     * Get the best available CVSS v3 score from any vendor.
     * Priority: NVD > RedHat > GHSA
     * 
     * @return Double score (0-10) or null if no scores available
     */
    public Double getBestV3Score() {
        if (nvd != null && nvd.getV3Score() != null) {
            return nvd.getV3Score();
        }
        if (redhat != null && redhat.getV3Score() != null) {
            return redhat.getV3Score();
        }
        if (ghsa != null && ghsa.getV3Score() != null) {
            return ghsa.getV3Score();
        }
        return null;
    }

    /**
     * Get the best available CVSS v2 score from any vendor.
     * Priority: NVD > RedHat > GHSA
     * 
     * @return Double score (0-10) or null if no scores available
     */
    public Double getBestV2Score() {
        if (nvd != null && nvd.getV2Score() != null) {
            return nvd.getV2Score();
        }
        if (redhat != null && redhat.getV2Score() != null) {
            return redhat.getV2Score();
        }
        if (ghsa != null && ghsa.getV2Score() != null) {
            return ghsa.getV2Score();
        }
        return null;
    }

    /**
     * Nested CVSS Score object for individual vendor data.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CVSSScore {
        @JsonProperty("V3Score")
        private Double v3Score;

        @JsonProperty("V3Vector")
        private String v3Vector;

        @JsonProperty("V2Score")
        private Double v2Score;

        @JsonProperty("V2Vector")
        private String v2Vector;
    }

}
