package com.secureops.auth.filter;

import com.secureops.serviceaccount.ServiceAccount;
import com.secureops.serviceaccount.ServiceAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * FILE: src/main/java/com/secureops/auth/filter/ApiKeyAuthenticationFilter.java
 * PURPOSE: Authenticates requests using API key header (X-API-Key).
 * WHY IT EXISTS: Allows CI/CD tools (Jenkins, GitHub Actions) to authenticate without JWT token.
 * SECURITY NOTES:
 * - API keys compared using BCrypt for timing-attack resistance
 * - Service account must be enabled and not expired
 * - Authentication set with ROLE_SERVICE_ACCOUNT authority
 * - Non-blocking: continues to next filter if no API key provided
 * - Logs usage for audit trail
 *
 * CHAIN ORDER:
 * 1. Try API Key authentication (this filter) - for Jenkins/CI/CD
 * 2. Try JWT authentication (JwtAuthenticationFilter) - for web users
 * 3. AnonymousAuthenticationFilter - if neither succeeds
 * 4. Spring Security decision: authenticate() or throw 401
 */
@Slf4j
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ServiceAccountRepository serviceAccountRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String API_KEY_HEADER = "X-API-Key";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        try {
            String apiKey = extractApiKeyFromRequest(request);

            if (apiKey != null && !apiKey.isEmpty()) {
                log.debug("API key provided in request, attempting authentication");

                // Try to authenticate using API key
                authenticateWithApiKey(apiKey);
            }

        } catch (Exception e) {
            log.debug("API key authentication failed: {}", e.getMessage());
            // Continue to next filter (JWT or anonymous)
        }

        // Continue filter chain regardless of success or failure
        filterChain.doFilter(request, response);
    }

    /**
     * Extract API key from X-API-Key header.
     * Example: X-API-Key: abc123def456
     */
    private String extractApiKeyFromRequest(HttpServletRequest request) {
        String header = request.getHeader(API_KEY_HEADER);
        if (header != null && !header.isEmpty()) {
            return header.trim();
        }
        return null;
    }

    /**
     * Authenticate request using API key.
     * Validates that:
     * 1. API key matches a service account's hashed key
     * 2. Service account is enabled
     * 3. Service account is not expired
     * 4. Sets SecurityContext with SERVICE_ACCOUNT authority
     *
     * @param apiKey The API key provided in X-API-Key header
     */
    private void authenticateWithApiKey(String apiKey) {
        log.debug("Attempting to authenticate with API key (length: {})", apiKey.length());
        
        // Query all service accounts (unfortunately must check each one)
        // In production, would maintain API key -> account mapping for O(1) lookup
        var allAccounts = serviceAccountRepository.findAllEnabled();
        log.debug("Found {} enabled service accounts", allAccounts.size());

        for (ServiceAccount account : allAccounts) {
            try {
                // Compare hashed API key using BCrypt
                boolean matches = passwordEncoder.matches(apiKey, account.getApiKeyHash());
                log.debug("Checking API key against account: {} - Match: {}", account.getName(), matches);
                
                if (matches) {
                    log.debug("API key matched service account: {}", account.getName());

                    // Validate account is still valid (not expired)
                    if (!account.isValid()) {
                        log.warn("Service account {} is expired or disabled", account.getName());
                        return;
                    }

                    // Update last used timestamp
                    account.setLastUsedAt(LocalDateTime.now());
                    serviceAccountRepository.save(account);

                    // Create authentication token
                    // Principal should be the USER ID who created this service account
                    // (not the service account ID), so that project ownership checks work
                    List<GrantedAuthority> authorities = new ArrayList<>();
                    authorities.add(new SimpleGrantedAuthority("ROLE_SERVICE_ACCOUNT"));
                    authorities.add(new SimpleGrantedAuthority("SCOPE_" + account.getScope().name()));

                    UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                            account.getCreatedBy(),       // Principal: USER ID (who created this service account)
                            null,                         // Credentials: none (stateless)
                            authorities                   // Authorities: SERVICE_ACCOUNT + SCOPE
                        );

                    // Set in SecurityContext for this request
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.info("API key authentication successful for service account: {} (user: {})", account.getName(), account.getCreatedBy());
                    return;
                }
            } catch (Exception e) {
                log.debug("Error checking API key against account {}: {}", account.getName(), e.getMessage());
                // Continue to next account
            }
        }

        log.debug("API key did not match any service account");
    }
}
