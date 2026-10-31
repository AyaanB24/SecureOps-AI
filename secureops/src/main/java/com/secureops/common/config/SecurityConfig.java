package com.secureops.common.config;

import com.secureops.auth.filter.JwtAuthenticationFilter;
import com.secureops.auth.filter.ApiKeyAuthenticationFilter;
import com.secureops.serviceaccount.ServiceAccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;
import java.util.Arrays;

/**
 * FILE: src/main/java/com/secureops/common/config/SecurityConfig.java
 * PURPOSE: Spring Security configuration with JWT and API Key authentication.
 * WHY IT EXISTS: Configures stateless authentication for web users (JWT) and CI/CD tools (API keys).
 * 
 * AUTHENTICATION MECHANISMS:
 * - JWT tokens for web users (login/register)
 * - API keys for CI/CD tools (Jenkins, GitHub Actions) via X-API-Key header
 * - Both stateless, no sessions
 *
 * FILTER CHAIN ORDER:
 * 1. ApiKeyAuthenticationFilter - checks X-API-Key header (for Jenkins/CI/CD)
 * 2. JwtAuthenticationFilter - checks Authorization: Bearer header (for web users)
 * 3. AnonymousAuthenticationFilter - allows unauthenticated public endpoints
 * 4. Spring Security authorization checks
 *
 * PROTECTED ENDPOINTS (require JWT or API Key):
 * - GET /api/projects (now accessible with API Key)
 * - GET /api/projects?name=X (now accessible with API Key)
 * - POST /api/projects
 * - /api/admin/** (admin only)
 */
@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {

    @Autowired(required = false)
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired(required = false)
    private ServiceAccountRepository serviceAccountRepository;

    @Autowired(required = false)
    private PasswordEncoder passwordEncoder;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:5173",
            "http://localhost:3000",
            "http://localhost:8080",
            "http://127.0.0.1:5173",
            "http://127.0.0.1:3000"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring Spring Security filter chain with CORS, JWT, and API Key authentication");

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/health").permitAll()
                
                // Projects endpoints - for Jenkins integration via API key
                // Both GET and POST require authentication (JWT or API key)
                .requestMatchers(HttpMethod.GET, "/api/projects").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/projects/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/projects").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/projects/**").authenticated()
                
                // Jenkins/CI endpoints (require API Key OR JWT token)
                // - API Key in X-API-Key header (for Jenkins)
                // - JWT in Authorization header (for other services)
                .requestMatchers(HttpMethod.POST, "/api/projects/*/scans").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/projects/*/scans/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/scans/*/reports").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/scans/*/reports/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/scans/*/decision").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/scans/*/risk").authenticated()
                
                // Service account endpoints (authenticated users can manage their own)
                // - Users can create their own API keys
                // - Users can list their own service accounts
                .requestMatchers(HttpMethod.POST, "/api/admin/service-accounts").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/admin/service-accounts").authenticated()
                // - Users can delete their own service accounts
                .requestMatchers(HttpMethod.DELETE, "/api/admin/service-accounts/**").authenticated()
                // - Only admins can regenerate/enable/disable
                .requestMatchers(HttpMethod.POST, "/api/admin/service-accounts/**").hasRole("ADMIN")
                
                // Other admin endpoints (admin role only)
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // All other /api/** endpoints require authentication
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Unauthorized: JWT token or API key required\"}");
                })
            );

        // Add API Key authentication filter (checks X-API-Key header)
        if (serviceAccountRepository != null && passwordEncoder != null) {
            log.info("Adding API Key authentication filter");
            ApiKeyAuthenticationFilter apiKeyFilter = new ApiKeyAuthenticationFilter(
                serviceAccountRepository,
                passwordEncoder
            );
            http.addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class);
        }

        // Add JWT filter
        if (jwtAuthenticationFilter != null) {
            log.info("Adding JWT authentication filter");
            http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        } else {
            log.warn("JWT authentication filter not available (missing JWT secret)");
        }

        return http.build();
    }
}
