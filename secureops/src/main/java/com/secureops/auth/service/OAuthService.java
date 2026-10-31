package com.secureops.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;

/**
 * OAuth service for handling GitHub and Google authentication
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthService {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${app.github.client-id}")
    private String githubClientId;

    @Value("${app.github.client-secret}")
    private String githubClientSecret;

    @Value("${app.google.client-id}")
    private String googleClientId;

    @Value("${app.google.client-secret}")
    private String googleClientSecret;

    // ==================== GITHUB OAUTH ====================

    /**
     * Exchange GitHub authorization code for access token
     */
    public String exchangeGitHubCodeForToken(String code) throws IOException {
        String url = "https://github.com/login/oauth/access_token";

        RequestBody body = new FormBody.Builder()
            .add("client_id", githubClientId)
            .add("client_secret", githubClientSecret)
            .add("code", code)
            .build();

        Request request = new Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Accept", "application/json")
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to exchange GitHub code for token: " + response.code());
            }

            JsonNode jsonNode = objectMapper.readTree(response.body().string());
            if (jsonNode.has("error")) {
                throw new IOException("GitHub error: " + jsonNode.get("error").asText());
            }
            return jsonNode.get("access_token").asText();
        }
    }

    /**
     * Get GitHub user info using access token
     */
    public GitHubUserInfo getGitHubUserInfo(String accessToken) throws IOException {
        String url = "https://api.github.com/user";

        Request request = new Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer " + accessToken)
            .addHeader("Accept", "application/vnd.github.v3+json")
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to get GitHub user info: " + response.code());
            }

            return objectMapper.readValue(response.body().string(), GitHubUserInfo.class);
        }
    }

    /**
     * Get GitHub user email
     */
    public String getGitHubUserEmail(String accessToken) throws IOException {
        String url = "https://api.github.com/user/emails";

        Request request = new Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer " + accessToken)
            .addHeader("Accept", "application/vnd.github.v3+json")
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to get GitHub user email: " + response.code());
            }

            JsonNode[] emails = objectMapper.readValue(
                response.body().string(),
                JsonNode[].class
            );

            // Get primary email or first verified email
            for (JsonNode email : emails) {
                if (email.get("primary").asBoolean() && email.get("verified").asBoolean()) {
                    return email.get("email").asText();
                }
            }

            // Return first verified email
            for (JsonNode email : emails) {
                if (email.get("verified").asBoolean()) {
                    return email.get("email").asText();
                }
            }

            // Return first email if none verified
            return emails.length > 0 ? emails[0].get("email").asText() : "";
        }
    }

    // ==================== GOOGLE OAUTH ====================

    /**
     * Exchange Google authorization code for tokens
     */
    public GoogleTokenResponse exchangeGoogleCodeForToken(String code, String redirectUri) throws IOException {
        String url = "https://oauth2.googleapis.com/token";

        RequestBody body = new FormBody.Builder()
            .add("client_id", googleClientId)
            .add("client_secret", googleClientSecret)
            .add("code", code)
            .add("grant_type", "authorization_code")
            .add("redirect_uri", redirectUri)
            .build();

        Request request = new Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Accept", "application/json")
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to exchange Google code for token: " + response.code());
            }

            return objectMapper.readValue(response.body().string(), GoogleTokenResponse.class);
        }
    }

    /**
     * Get Google user info using ID token (from JWT)
     */
    public GoogleUserInfo getGoogleUserInfo(String idToken) throws IOException {
        // Decode JWT (simplified - in production use JWT library)
        // For now, we'll call userinfo endpoint with access token
        // This is called after we have the full token response
        return null; // Will be implemented with userinfo endpoint
    }

    /**
     * Verify Google ID token and extract user info
     */
    public GoogleUserInfo verifyGoogleIdToken(String idToken) throws IOException {
        String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;

        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to verify Google ID token: " + response.code());
            }

            return objectMapper.readValue(response.body().string(), GoogleUserInfo.class);
        }
    }

    /**
     * Get Google user info using access token
     */
    public GoogleUserInfo getGoogleUserInfoByAccessToken(String accessToken) throws IOException {
        String url = "https://www.googleapis.com/oauth2/v1/userinfo?access_token=" + accessToken;

        Request request = new Request.Builder()
            .url(url)
            .get()
            .addHeader("Accept", "application/json")
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to get Google user info: " + response.code());
            }

            return objectMapper.readValue(response.body().string(), GoogleUserInfo.class);
        }
    }

    // ==================== DTOs ====================

    public static class GitHubUserInfo {
        public Long id;
        public String login;
        public String name;
        public String avatar_url;
        public String email;
    }

    public static class GoogleTokenResponse {
        public String access_token;
        public String id_token;
        public String token_type;
        public Integer expires_in;
        public String refresh_token;
    }

    public static class GoogleUserInfo {
        public String id;
        public String email;
        public String name;
        public String picture;
        public Boolean email_verified;
    }
}
