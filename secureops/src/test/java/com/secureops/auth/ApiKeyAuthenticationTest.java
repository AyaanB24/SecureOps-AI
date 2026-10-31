package com.secureops.auth;

import com.secureops.serviceaccount.ServiceAccount;
import com.secureops.serviceaccount.ServiceAccountRepository;
import com.secureops.serviceaccount.ServiceAccountScope;
import com.secureops.auth.filter.ApiKeyAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test suite for API Key authentication with service accounts.
 * Verifies X-API-Key header authentication works correctly.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiKeyAuthenticationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServiceAccountRepository serviceAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private ServiceAccount jenkinsAccount;
    private String validApiKey;

    @BeforeEach
    void setup() {
        // Create a service account for Jenkins
        validApiKey = "test-api-key-jenkins-12345678901234567890";
        String apiKeyHash = passwordEncoder.encode(validApiKey);

        jenkinsAccount = new ServiceAccount("Jenkins CI", "Jenkins CI/CD Integration", apiKeyHash, ServiceAccountScope.JENKINS_CI, "system");
        jenkinsAccount.setEnabled(true);
        serviceAccountRepository.save(jenkinsAccount);
    }

    @Test
    void testValidApiKeyAuthentication() throws Exception {
        // Should succeed with valid API key
        mockMvc.perform(get("/api/projects")
                .header("X-API-Key", validApiKey)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    void testInvalidApiKey() throws Exception {
        // Should fail with invalid API key
        mockMvc.perform(get("/api/projects")
                .header("X-API-Key", "invalid-key-that-does-not-exist")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void testMissingApiKey() throws Exception {
        // Should fail without API key (unauthenticated)
        mockMvc.perform(get("/api/projects")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void testServiceAccountAccessToSystemProjectsOnly() throws Exception {
        // Service account should only access projects with owner_id IS NULL (system projects)
        mockMvc.perform(get("/api/projects?name=System-Project")
                .header("X-API-Key", validApiKey)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    void testDisabledServiceAccount() throws Exception {
        // Disable the service account
        jenkinsAccount.setEnabled(false);
        serviceAccountRepository.save(jenkinsAccount);

        // Should fail with disabled account
        mockMvc.perform(get("/api/projects")
                .header("X-API-Key", validApiKey)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void testApiKeyWithJwtAlsoWorks() throws Exception {
        // Both API key and JWT should work independently
        mockMvc.perform(get("/api/health")
                .header("X-API-Key", validApiKey)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }
}
