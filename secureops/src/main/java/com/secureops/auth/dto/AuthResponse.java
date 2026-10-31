package com.secureops.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/auth/dto/AuthResponse.java
 * PURPOSE: Safe response DTO for authentication endpoints (registration, login, current user).
 * WHY IT EXISTS: Excludes password hash; returns only public user data, JWT token, and status.
 * DEPENDENCIES: Used by AuthController for all auth responses.
 *
 * SECURITY NOTE: Never includes password_hash or plaintext passwords.
 * Phase 4: Added JWT token field for stateless authentication.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private UUID id;

    private String email;

    private String name;

    private String role;

    private Boolean enabled;

    private String token;  // JWT token (Phase 4) - only in login/register responses, not in /me

    private String message;
}
