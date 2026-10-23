package com.secureops.report.parser.trivy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secureops.finding.Finding;
import com.secureops.finding.FindingStatus;
import com.secureops.finding.Severity;
import com.secureops.finding.FingerprintService;
import com.secureops.report.Report;
import com.secureops.report.ReportRepository;
import com.secureops.report.ReportStatus;
import com.secureops.report.ReportTool;
import com.secureops.scan.Scan;
import com.secureops.scan.ScanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * FILE: src/test/java/com/secureops/report/parser/trivy/TrivyParserTest.java
 * PURPOSE: Unit tests for TrivyParser CVSS deserialization and finding extraction.
 * WHY IT EXISTS: Verifies that parser correctly handles both simple numeric CVSS (legacy)
 *                and complex object-valued CVSS (modern Trivy format) without crashes.
 * 
 * TEST SCENARIOS:
 * 1. Simple numeric CVSS (Integer) - legacy format compatibility
 * 2. Simple numeric CVSS (Double) - legacy format compatibility
 * 3. Complex CVSS object with NVD vendor data (V3Score)
 * 4. Complex CVSS object with RedHat vendor data
 * 5. Complex CVSS object with multiple vendors (priority testing)
 * 6. Missing CVSS field (null safety)
 * 7. Empty vulnerability results
 * 8. Real Trivy JSON with object-valued CVSS field
 */
@SpringBootTest
@DisplayName("Trivy Parser Tests - CVSS Deserialization and Finding Extraction")
public class TrivyParserTest {

    @Autowired
    private TrivyParser trivyParser;

    @MockBean
    private FingerprintService fingerprintService;

    @MockBean
    private ScanRepository scanRepository;

    @MockBean
    private ReportRepository reportRepository;

    private Scan testScan;
    private Report testReport;
    private UUID scanId;
    private UUID reportId;

    @BeforeEach
    public void setUp() {
        // Setup test data
        scanId = UUID.randomUUID();
        reportId = UUID.randomUUID();
        
        testScan = new Scan();
        testScan.setId(scanId);
        
        testReport = new Report();
        testReport.setId(reportId);
        testReport.setStatus(ReportStatus.RECEIVED);
        testReport.setTool(ReportTool.TRIVY);
        
        when(scanRepository.findById(scanId)).thenReturn(Optional.of(testScan));
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(testReport));
        when(fingerprintService.generateFingerprint(anyString(), anyString(), isNull(), isNull(), anyString()))
            .thenReturn("mock-fingerprint-hash");
    }

    @Test
    @DisplayName("Parse vulnerability with simple numeric CVSS (Integer)")
    public void testParseSimpleNumericCVSSInteger() throws Exception {
        String json = "{\n" +
            "  \"SchemaVersion\": 2,\n" +
            "  \"Results\": [\n" +
            "    {\n" +
            "      \"Target\": \"alpine:3.18\",\n" +
            "      \"Type\": \"os-pkgs\",\n" +
            "      \"Vulnerabilities\": [\n" +
            "        {\n" +
            "          \"VulnerabilityID\": \"CVE-2024-1086\",\n" +
            "          \"PkgName\": \"linux\",\n" +
            "          \"InstalledVersion\": \"5.4.0\",\n" +
            "          \"FixedVersion\": \"5.4.1\",\n" +
            "          \"Title\": \"Kernel Privilege Escalation\",\n" +
            "          \"Description\": \"A vulnerability in the Linux kernel\",\n" +
            "          \"Severity\": \"CRITICAL\",\n" +
            "          \"PrimaryURL\": \"https://nvd.nist.gov/vuln/detail/CVE-2024-1086\",\n" +
            "          \"CweID\": \"CWE-416\",\n" +
            "          \"CVSS\": 9\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";

        List<Finding> findings = trivyParser.parse(json, scanId, reportId);

        assertNotNull(findings);
        assertEquals(1, findings.size());
        
        Finding finding = findings.get(0);
        assertEquals("CVE-2024-1086", finding.getRuleId());
        assertEquals("linux", finding.getPackageName());
        assertEquals(Severity.CRITICAL, finding.getSeverity());
        assertEquals(9.0, finding.getCvssScore(), 0.01);
    }

    @Test
    @DisplayName("Parse vulnerability with simple numeric CVSS (Double)")
    public void testParseSimpleNumericCVSSDouble() throws Exception {
        String json = "{\n" +
            "  \"SchemaVersion\": 2,\n" +
            "  \"Results\": [\n" +
            "    {\n" +
            "      \"Target\": \"alpine:3.18\",\n" +
            "      \"Type\": \"os-pkgs\",\n" +
            "      \"Vulnerabilities\": [\n" +
            "        {\n" +
            "          \"VulnerabilityID\": \"CVE-2023-44487\",\n" +
            "          \"PkgName\": \"openssl\",\n" +
            "          \"InstalledVersion\": \"1.1.1f\",\n" +
            "          \"FixedVersion\": \"1.1.1g\",\n" +
            "          \"Title\": \"HTTP/2 Rapid Reset\",\n" +
            "          \"Description\": \"HTTP/2 attack vector\",\n" +
            "          \"Severity\": \"HIGH\",\n" +
            "          \"PrimaryURL\": \"https://nvd.nist.gov/vuln/detail/CVE-2023-44487\",\n" +
            "          \"CweID\": \"CWE-400\",\n" +
            "          \"CVSS\": 7.5\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";

        List<Finding> findings = trivyParser.parse(json, scanId, reportId);

        assertNotNull(findings);
        assertEquals(1, findings.size());
        
        Finding finding = findings.get(0);
        assertEquals("CVE-2023-44487", finding.getRuleId());
        assertEquals("openssl", finding.getPackageName());
        assertEquals(Severity.HIGH, finding.getSeverity());
        assertEquals(7.5, finding.getCvssScore(), 0.01);
    }

    @Test
    @DisplayName("Parse vulnerability with complex CVSS object (NVD vendor)")
    public void testParseComplexCVSSObjectNVD() throws Exception {
        String json = "{\n" +
            "  \"SchemaVersion\": 2,\n" +
            "  \"Results\": [\n" +
            "    {\n" +
            "      \"Target\": \"alpine:3.18\",\n" +
            "      \"Type\": \"os-pkgs\",\n" +
            "      \"Vulnerabilities\": [\n" +
            "        {\n" +
            "          \"VulnerabilityID\": \"CVE-2024-12345\",\n" +
            "          \"PkgName\": \"curl\",\n" +
            "          \"InstalledVersion\": \"7.68.0\",\n" +
            "          \"FixedVersion\": \"7.69.0\",\n" +
            "          \"Title\": \"Curl Vulnerability\",\n" +
            "          \"Description\": \"A vulnerability in curl\",\n" +
            "          \"Severity\": \"HIGH\",\n" +
            "          \"PrimaryURL\": \"https://nvd.nist.gov/vuln/detail/CVE-2024-12345\",\n" +
            "          \"CweID\": \"CWE-113\",\n" +
            "          \"CVSS\": {\n" +
            "            \"nvd\": {\n" +
            "              \"V3Score\": 8.2,\n" +
            "              \"V3Vector\": \"CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:L\",\n" +
            "              \"V2Score\": 5.0,\n" +
            "              \"V2Vector\": \"AV:N/AC:L/Au:N/C:P/I:N/A:N\"\n" +
            "            }\n" +
            "          }\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";

        List<Finding> findings = trivyParser.parse(json, scanId, reportId);

        assertNotNull(findings);
        assertEquals(1, findings.size());
        
        Finding finding = findings.get(0);
        assertEquals("CVE-2024-12345", finding.getRuleId());
        assertEquals("curl", finding.getPackageName());
        // Should extract V3Score from NVD vendor
        assertEquals(8.2, finding.getCvssScore(), 0.01);
    }

    @Test
    @DisplayName("Parse vulnerability with complex CVSS object (RedHat vendor)")
    public void testParseComplexCVSSObjectRedHat() throws Exception {
        String json = "{\n" +
            "  \"SchemaVersion\": 2,\n" +
            "  \"Results\": [\n" +
            "    {\n" +
            "      \"Target\": \"alpine:3.18\",\n" +
            "      \"Type\": \"os-pkgs\",\n" +
            "      \"Vulnerabilities\": [\n" +
            "        {\n" +
            "          \"VulnerabilityID\": \"CVE-2024-99999\",\n" +
            "          \"PkgName\": \"glibc\",\n" +
            "          \"InstalledVersion\": \"2.31\",\n" +
            "          \"FixedVersion\": \"2.32\",\n" +
            "          \"Title\": \"glibc Vulnerability\",\n" +
            "          \"Description\": \"A vulnerability in glibc\",\n" +
            "          \"Severity\": \"MEDIUM\",\n" +
            "          \"PrimaryURL\": \"https://nvd.nist.gov/vuln/detail/CVE-2024-99999\",\n" +
            "          \"CweID\": \"CWE-200\",\n" +
            "          \"CVSS\": {\n" +
            "            \"redhat\": {\n" +
            "              \"V3Score\": 6.5,\n" +
            "              \"V3Vector\": \"CVSS:3.1/AV:N/AC:L/PR:L/UI:N/S:U/C:H/I:N/A:N\"\n" +
            "            }\n" +
            "          }\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";

        List<Finding> findings = trivyParser.parse(json, scanId, reportId);

        assertNotNull(findings);
        assertEquals(1, findings.size());
        
        Finding finding = findings.get(0);
        assertEquals("CVE-2024-99999", finding.getRuleId());
        assertEquals("glibc", finding.getPackageName());
        // Should extract V3Score from RedHat vendor
        assertEquals(6.5, finding.getCvssScore(), 0.01);
    }

    @Test
    @DisplayName("Parse vulnerability with multiple CVSS vendors (priority: NVD > RedHat)")
    public void testParseComplexCVSSMultipleVendors() throws Exception {
        String json = "{\n" +
            "  \"SchemaVersion\": 2,\n" +
            "  \"Results\": [\n" +
            "    {\n" +
            "      \"Target\": \"alpine:3.18\",\n" +
            "      \"Type\": \"os-pkgs\",\n" +
            "      \"Vulnerabilities\": [\n" +
            "        {\n" +
            "          \"VulnerabilityID\": \"CVE-2024-77777\",\n" +
            "          \"PkgName\": \"apache2\",\n" +
            "          \"InstalledVersion\": \"2.4.41\",\n" +
            "          \"FixedVersion\": \"2.4.42\",\n" +
            "          \"Title\": \"Apache Vulnerability\",\n" +
            "          \"Description\": \"A vulnerability in Apache\",\n" +
            "          \"Severity\": \"CRITICAL\",\n" +
            "          \"PrimaryURL\": \"https://nvd.nist.gov/vuln/detail/CVE-2024-77777\",\n" +
            "          \"CweID\": \"CWE-94\",\n" +
            "          \"CVSS\": {\n" +
            "            \"nvd\": {\n" +
            "              \"V3Score\": 9.8,\n" +
            "              \"V3Vector\": \"CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H\"\n" +
            "            },\n" +
            "            \"redhat\": {\n" +
            "              \"V3Score\": 8.8,\n" +
            "              \"V3Vector\": \"CVSS:3.1/AV:N/AC:L/PR:L/UI:N/S:U/C:H/I:H/A:H\"\n" +
            "            }\n" +
            "          }\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";

        List<Finding> findings = trivyParser.parse(json, scanId, reportId);

        assertNotNull(findings);
        assertEquals(1, findings.size());
        
        Finding finding = findings.get(0);
        assertEquals("CVE-2024-77777", finding.getRuleId());
        // Should prefer NVD V3 score (9.8) over RedHat (8.8)
        assertEquals(9.8, finding.getCvssScore(), 0.01);
    }

    @Test
    @DisplayName("Parse vulnerability with missing CVSS field")
    public void testParseMissingCVSS() throws Exception {
        String json = "{\n" +
            "  \"SchemaVersion\": 2,\n" +
            "  \"Results\": [\n" +
            "    {\n" +
            "      \"Target\": \"alpine:3.18\",\n" +
            "      \"Type\": \"os-pkgs\",\n" +
            "      \"Vulnerabilities\": [\n" +
            "        {\n" +
            "          \"VulnerabilityID\": \"CVE-2023-55555\",\n" +
            "          \"PkgName\": \"bash\",\n" +
            "          \"InstalledVersion\": \"5.0\",\n" +
            "          \"FixedVersion\": \"5.1\",\n" +
            "          \"Title\": \"Bash Vulnerability\",\n" +
            "          \"Description\": \"A vulnerability in bash\",\n" +
            "          \"Severity\": \"MEDIUM\",\n" +
            "          \"PrimaryURL\": \"https://nvd.nist.gov/vuln/detail/CVE-2023-55555\",\n" +
            "          \"CweID\": \"CWE-78\"\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";

        List<Finding> findings = trivyParser.parse(json, scanId, reportId);

        assertNotNull(findings);
        assertEquals(1, findings.size());
        
        Finding finding = findings.get(0);
        assertEquals("CVE-2023-55555", finding.getRuleId());
        assertNull(finding.getCvssScore(), "CVSS score should be null when not present");
    }

    @Test
    @DisplayName("Parse empty results returns empty findings list")
    public void testParseEmptyResults() throws Exception {
        String json = "{\n" +
            "  \"SchemaVersion\": 2,\n" +
            "  \"Results\": []\n" +
            "}";

        List<Finding> findings = trivyParser.parse(json, scanId, reportId);

        assertNotNull(findings);
        assertEquals(0, findings.size());
    }

    @Test
    @DisplayName("Parse real Trivy JSON with object-valued CVSS")
    public void testParseRealTrivyJSONWithCVSSObject() throws Exception {
        // This JSON mimics the actual structure from real Trivy reports
        String json = "{\n" +
            "  \"SchemaVersion\": 2,\n" +
            "  \"ArtifactName\": \"secureops-demo:5\",\n" +
            "  \"ArtifactType\": \"container_image\",\n" +
            "  \"Results\": [\n" +
            "    {\n" +
            "      \"Target\": \"secureops-demo:5 (alpine 3.24.2)\",\n" +
            "      \"Class\": \"os-pkgs\",\n" +
            "      \"Type\": \"alpine\",\n" +
            "      \"Vulnerabilities\": [\n" +
            "        {\n" +
            "          \"VulnerabilityID\": \"CVE-2024-1086\",\n" +
            "          \"PkgName\": \"linux-image-generic\",\n" +
            "          \"InstalledVersion\": \"5.4.0-42-generic\",\n" +
            "          \"FixedVersion\": \"5.4.0-43-generic\",\n" +
            "          \"Title\": \"Linux Kernel Privilege Escalation\",\n" +
            "          \"Description\": \"A use-after-free vulnerability in the Linux kernel\",\n" +
            "          \"Severity\": \"CRITICAL\",\n" +
            "          \"PrimaryURL\": \"https://nvd.nist.gov/vuln/detail/CVE-2024-1086\",\n" +
            "          \"CweID\": \"CWE-416\",\n" +
            "          \"CVSS\": {\n" +
            "            \"nvd\": {\n" +
            "              \"V3Vector\": \"CVSS:3.1/AV:L/AC:L/PR:L/UI:N/S:U/C:H/I:H/A:H\",\n" +
            "              \"V3Score\": 7.8,\n" +
            "              \"V2Score\": 4.9,\n" +
            "              \"V2Vector\": \"AV:L/AC:L/Au:S/C:P/I:P/A:P\"\n" +
            "            },\n" +
            "            \"redhat\": {\n" +
            "              \"V3Vector\": \"CVSS:3.1/AV:L/AC:L/PR:L/UI:N/S:U/C:H/I:H/A:H\",\n" +
            "              \"V3Score\": 7.8\n" +
            "            }\n" +
            "          }\n" +
            "        },\n" +
            "        {\n" +
            "          \"VulnerabilityID\": \"CVE-2023-46604\",\n" +
            "          \"PkgName\": \"apache2\",\n" +
            "          \"InstalledVersion\": \"2.4.41-1ubuntu1.5\",\n" +
            "          \"FixedVersion\": \"2.4.41-1ubuntu1.6\",\n" +
            "          \"Title\": \"Apache HTTP Server Expression Parser\",\n" +
            "          \"Description\": \"A flaw in the expression parser\",\n" +
            "          \"Severity\": \"CRITICAL\",\n" +
            "          \"PrimaryURL\": \"https://nvd.nist.gov/vuln/detail/CVE-2023-46604\",\n" +
            "          \"CweID\": \"CWE-94\",\n" +
            "          \"CVSS\": {\n" +
            "            \"redhat\": {\n" +
            "              \"V3Score\": 8.2\n" +
            "            }\n" +
            "          }\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";

        List<Finding> findings = trivyParser.parse(json, scanId, reportId);

        assertNotNull(findings);
        assertEquals(2, findings.size());
        
        // First vulnerability: NVD score should take precedence
        Finding finding1 = findings.get(0);
        assertEquals("CVE-2024-1086", finding1.getRuleId());
        assertEquals(Severity.CRITICAL, finding1.getSeverity());
        assertEquals(7.8, finding1.getCvssScore(), 0.01);
        
        // Second vulnerability: Only RedHat score available
        Finding finding2 = findings.get(1);
        assertEquals("CVE-2023-46604", finding2.getRuleId());
        assertEquals(Severity.CRITICAL, finding2.getSeverity());
        assertEquals(8.2, finding2.getCvssScore(), 0.01);
    }

    @Test
    @DisplayName("Parser detects Trivy JSON correctly")
    public void testCanParseTrivy() throws Exception {
        String trivyJson = "{\n" +
            "  \"SchemaVersion\": 2,\n" +
            "  \"Results\": []\n" +
            "}";

        boolean canParse = trivyParser.canParse(trivyJson);
        assertTrue(canParse);
    }

    @Test
    @DisplayName("Parser rejects non-Trivy JSON")
    public void testCannotParseNonTrivy() throws Exception {
        String nonTrivyJson = "{\n" +
            "  \"results\": [],\n" +
            "  \"metadata\": {}\n" +
            "}";

        boolean canParse = trivyParser.canParse(nonTrivyJson);
        assertFalse(canParse);
    }

}
