package com.secureops.finding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * FILE: src/main/java/com/secureops/finding/FingerprintService.java
 * PURPOSE: Generate deterministic fingerprints for findings (deduplication).
 * WHY IT EXISTS: Enables future finding deduplication across scans.
 * 
 * FINGERPRINT STRATEGY:
 * A fingerprint is a SHA-256 hash of stable finding properties.
 * Same vulnerability in same location = same fingerprint across scans.
 * 
 * Components:
 * - tool: Security tool name (TRIVY, SEMGREP, etc.)
 * - ruleId: Unique identifier from tool (CVE-xxx, rule-id, etc.)
 * - filePath: Source file path (for SAST findings)
 * - lineNumber: Source line number (for SAST findings)
 * - packageName: Package/library name (for dependency findings)
 * 
 * Example:
 * Scan 1: CVE-2023-12345 in package libssl1.1 v1.1.1 → fingerprint = sha256(...)
 * Scan 2: Same CVE in same package version → fingerprint = sha256(...) [IDENTICAL]
 * → Deduplication can detect this is the same vulnerability
 * 
 * WHY FINGERPRINTS ARE USEFUL:
 * 1. Deduplication: Same vulnerability across multiple scans won't be duplicated
 * 2. Tracking: See vulnerability persistence across scan history
 * 3. Remediation: Verify if a vulnerability has actually been fixed
 * 4. Trending: Analyze vulnerability lifecycle in your application
 */
@Service
@Slf4j
public class FingerprintService {

    /**
     * Generate a deterministic fingerprint for a finding.
     * Based on stable properties that define a unique vulnerability.
     *
     * @param tool Security tool name
     * @param ruleId Rule/CVE identifier
     * @param filePath Source file path (nullable)
     * @param lineNumber Source line number (nullable)
     * @param packageName Package name (nullable)
     * @return SHA-256 fingerprint hash
     */
    public String generateFingerprint(String tool, String ruleId, String filePath,
                                      Integer lineNumber, String packageName) {
        try {
            // Build fingerprint input from stable properties
            StringBuilder sb = new StringBuilder();
            sb.append(tool).append("|");
            sb.append(ruleId).append("|");
            sb.append(filePath != null ? filePath : "").append("|");
            sb.append(lineNumber != null ? lineNumber : "").append("|");
            sb.append(packageName != null ? packageName : "");

            String fingerprintInput = sb.toString();
            log.debug("Fingerprint input: {}", fingerprintInput);

            // Generate SHA-256 hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fingerprintInput.getBytes(StandardCharsets.UTF_8));

            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            String fingerprint = hexString.toString();
            log.debug("Generated fingerprint: {}", fingerprint);
            return fingerprint;

        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("Failed to generate fingerprint: " + e.getMessage());
        }
    }

}
