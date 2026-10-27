package com.secureops.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * FILE: src/main/java/com/secureops/common/config/CorsConfig.java
 * PURPOSE: Configure Cross-Origin Resource Sharing (CORS) for the Spring Boot backend.
 * WHY IT EXISTS: Allows the React frontend (http://localhost:5173) to make requests to the backend (http://localhost:8081).
 * DEPENDENCIES: Spring Web MVC configuration.
 *
 * DEVELOPMENT CONFIGURATION:
 * - Allows requests from http://localhost:5173 (React dev server)
 * - Allows all common HTTP methods (GET, POST, PUT, DELETE, PATCH, OPTIONS)
 * - Allows credentials (cookies, authorization headers)
 * - Allows all headers
 *
 * PRODUCTION NOTE: Update allowed origins to your production domain before deploying.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173", "http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
