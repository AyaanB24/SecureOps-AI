package com.secureops.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FILE: src/main/java/com/secureops/auth/dto/LoginRequest.java
 * PURPOSE: Request DTO for login endpoint.
 * WHY IT EXISTS: Validates and deserializes login credentials from client.
 * DEPENDENCIES: Used by AuthController.login(); validated by Spring's @Valid annotation.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 1, max = 128, message = "Password must not exceed 128 characters")
    private String password;
}
