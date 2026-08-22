package com.secureops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * FILE: src/main/java/com/secureops/SecureopsApplication.java
 * PURPOSE: Spring Boot application entry point for SecureOps backend.
 * WHY IT EXISTS: Bootstraps the entire application with Spring Boot's auto-configuration and classpath component scanning.
 * DEPENDENCIES: Depends on Spring Boot framework; all controllers and services are discovered automatically.
 */
@SpringBootApplication
public class SecureopsApplication {

	public static void main(String[] args) {
		SpringApplication.run(SecureopsApplication.class, args);
	}

}
