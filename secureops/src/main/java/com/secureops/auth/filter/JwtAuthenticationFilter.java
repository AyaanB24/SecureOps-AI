package com.secureops.auth.filter;

import com.secureops.auth.service.JwtService;
import com.secureops.user.UserService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/auth/filter/JwtAuthenticationFilter.java
 * PURPOSE: JWT authentication filter for Spring Security.
 * WHY IT EXISTS: Validates JWT tokens on each request and sets the SecurityContext for authenticated requests.
 * DEPENDENCIES: Uses JwtService for token validation and UserService for user lookup.
 *
 * SECURITY NOTES:
 * - Extracts JWT from Authorization header (Bearer token)
 * - Validates signature, expiration, and claims
 * - Sets SecurityContext for authenticated requests
 * - Logs authentication failures (without exposing secrets)
 * - Continues to next filter on validation failure (Spring Security decides access)
 * - Only loaded if app.jwt.secret is configured (allows graceful fallback in test/dev)
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.jwt.secret", havingValue = "", matchIfMissing = false)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserService userService;

    /**
     * Extract JWT token from Authorization header.
     * Expected format: "Bearer <token>"
     *
     * @param request HTTP request
     * @return JWT token or null if not found
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7); // Remove "Bearer " prefix
        }
        return null;
    }

    /**
     * Process JWT authentication on each request.
     * This filter runs ONCE per request (OncePerRequestFilter).
     *
     * FLOW:
     * 1. Extract JWT from Authorization header
     * 2. Validate JWT signature and expiration
     * 3. Extract user ID from JWT claims
     * 4. Load user from database (ensures user still exists and is enabled)
     * 5. Set SecurityContext for downstream processing
     * 6. If any step fails, continue to next filter (Spring Security decides access)
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param filterChain Next filter in chain
     * @throws ServletException on servlet errors
     * @throws IOException on I/O errors
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        try {
            String token = extractTokenFromRequest(request);

            if (token != null) {
                // Validate JWT signature, expiration, and structure
                jwtService.validateToken(token);
                log.debug("JWT token signature and expiration validated");

                // Extract user ID from JWT claims
                String userIdString = jwtService.extractSubject(token);
                UUID userId = UUID.fromString(userIdString);

                // Load user from database (ensures user still exists and is enabled)
                var user = userService.findById(userId);
                if (user.isPresent() && userService.isUserEnabled(user.get())) {
                    // Create authentication token with user authority
                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + user.get().getRole()));

                    UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                            user.get().getId().toString(),
                            null,
                            authorities
                        );

                    // Set in SecurityContext for this request
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("User authenticated: {}", user.get().getEmail());
                } else {
                    log.warn("User not found or disabled: {}", userIdString);
                }
            }
        } catch (JwtException ex) {
            log.warn("JWT authentication failed: {}", ex.getMessage());
            // Continue to next filter - Spring Security will enforce authorization
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid user ID in JWT: {}", ex.getMessage());
            // Continue to next filter
        }

        // Continue to next filter in the chain
        filterChain.doFilter(request, response);
    }
}
