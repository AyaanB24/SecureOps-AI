package com.secureops.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * FILE: src/main/java/com/secureops/common/config/PasswordEncoderConfig.java
 * PURPOSE: Standalone configuration for PasswordEncoder bean.
 * WHY IT EXISTS: Ensures PasswordEncoder is always available for autowiring, even if other security config is deferred.
 * DEPENDENCIES: Uses BCrypt password encoding.
 */
@Configuration
@Slf4j
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        log.info("Initializing BCryptPasswordEncoder with strength 10");
        return new BCryptPasswordEncoder(10);
    }
}
