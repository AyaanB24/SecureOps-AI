package com.secureops.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FILE: src/main/java/com/secureops/auth/dto/RegisterRequest.java
 * PURPOSE: Request DTO for user registration endpoint.
 * WHY IT EXISTS: Validates and deserializes registration input from client; enforces security constraints.
 * DEPENDENCIES: Used by AuthController.register(); validated by Spring's @Valid annotation.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 255, message = "Name must be between 1 and 255 characters")
    private String name;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    private String password;

    // Client-supplied role is intentionally ignored; always assigned USER by server
}
