package com.secureops.ai;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * FILE: src/main/java/com/secureops/ai/AiConfiguration.java
 * PURPOSE: Configuration for AI-related beans (RestTemplate, etc.).
 * WHY IT EXISTS: Provides required beans for AI service integration.
 */
@Configuration
public class AiConfiguration {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
