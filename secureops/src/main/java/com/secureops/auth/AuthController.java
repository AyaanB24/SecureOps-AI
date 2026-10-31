package com.secureops.auth;

import com.secureops.auth.dto.AuthResponse;
import com.secureops.auth.dto.LoginRequest;
import com.secureops.auth.dto.RegisterRequest;
import com.secureops.auth.dto.OAuthCallbackRequest;
import com.secureops.auth.service.JwtService;
import com.secureops.auth.service.OAuthService;
import com.secureops.user.User;
import com.secureops.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/auth/AuthController.java
 * PURPOSE: REST controller for authentication endpoints (registration, login, current user).
 * WHY IT EXISTS: Exposes authentication operations via HTTP; returns JWT tokens for stateless authentication.
 * DEPENDENCIES: Uses UserService for user registration/authentication, JwtService for token generation.
 *
 * SECURITY NOTES:
 * - Registration always assigns USER role (never ADMIN via public endpoint)
 * - Passwords never logged or returned in responses
 * - JWT tokens signed and expire after configured duration
 * - /api/auth/me returns current authenticated user (requires valid JWT)
 * - Passwords never exposed
 *
 * PHASE 4 CHANGES:
 * - Login and register now return JWT tokens
 * - New /api/auth/me endpoint for current user
 * - Tokens valid for 1 hour (configurable via app.jwt.expiration-ms)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final OAuthService oauthService;

    /**
     * Register a new user account.
     * POST /api/auth/register
     *
     * SECURITY:
     * - Email normalized and checked for duplicates
     * - Password hashed with BCrypt before storage
     * - Role always set to USER (client-supplied role ignored)
     * - Returns JWT token for immediate authentication
     *
     * @param request RegisterRequest containing email, name, password
     * @return ResponseEntity with AuthResponse (including JWT token) and HTTP 201 Created
     * @throws DuplicateEmailException if email already registered (HTTP 409 Conflict)
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("POST /api/auth/register - Registering new user: {}", request.getEmail());

        // Register user (UserService enforces USER role, hashes password)
        User registeredUser = userService.registerUser(request.getEmail(), request.getName(), request.getPassword());

        // Generate JWT token for new user
        String jwtToken = jwtService.generateToken(registeredUser.getId(), registeredUser.getEmail());
        log.debug("JWT token generated for new user: {}", registeredUser.getEmail());

        // Build response with JWT token
        AuthResponse response = AuthResponse.builder()
            .id(registeredUser.getId())
            .email(registeredUser.getEmail())
            .name(registeredUser.getName())
            .role(registeredUser.getRole().toString())
            .enabled(registeredUser.getEnabled())
            .token(jwtToken)
            .message("User registered successfully. Use token in Authorization header: Bearer " + jwtToken.substring(0, 20) + "...")
            .build();

        log.info("User registered successfully with JWT token: {}", registeredUser.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticate a user with email and password.
     * POST /api/auth/login
     *
     * SECURITY:
     * - Email normalized and looked up in database
     * - Password verified against BCrypt hash
     * - Disabled accounts rejected
     * - Invalid credentials return same generic message (prevents enumeration)
     * - Returns JWT token on success
     *
     * @param request LoginRequest containing email and password
     * @return ResponseEntity with AuthResponse (including JWT token) and HTTP 200 OK (on success)
     * @throws InvalidCredentialsException if email not found, password wrong, or account disabled (HTTP 401 Unauthorized)
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /api/auth/login - Login attempt for: {}", request.getEmail());

        // Authenticate user (UserService compares password hash, checks enabled status)
        User authenticatedUser = userService.authenticateUser(request.getEmail(), request.getPassword());

        // Generate JWT token for authenticated user
        String jwtToken = jwtService.generateToken(authenticatedUser.getId(), authenticatedUser.getEmail());
        log.debug("JWT token generated for user: {}", authenticatedUser.getEmail());

        // Build response with JWT token
        AuthResponse response = AuthResponse.builder()
            .id(authenticatedUser.getId())
            .email(authenticatedUser.getEmail())
            .name(authenticatedUser.getName())
            .role(authenticatedUser.getRole().toString())
            .enabled(authenticatedUser.getEnabled())
            .token(jwtToken)
            .message("Login successful. Use token in Authorization header: Bearer " + jwtToken.substring(0, 20) + "...")
            .build();

        log.info("User logged in successfully with JWT token: {}", authenticatedUser.getEmail());
        return ResponseEntity.ok(response);
    }

    /**
     * Get current authenticated user.
     * GET /api/auth/me
     *
     * SECURITY:
     * - Requires valid JWT token in Authorization header
     * - Returns currently authenticated user from SecurityContext
     * - Verifies user still exists and is enabled
     *
     * @return ResponseEntity with AuthResponse of current user and HTTP 200 OK
     * @throws Exception if no valid authentication found
     */
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser() {
        log.debug("GET /api/auth/me - Retrieving current authenticated user");

        // Get current authentication from SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Unauthorized access to /api/auth/me: No authentication");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Get principal (email) from authentication
        String email = authentication.getName();
        log.debug("Current user email: {}", email);

        // Load user from database
        var optionalUser = userService.findByEmail(email);
        if (optionalUser.isEmpty() || !userService.isUserEnabled(optionalUser.get())) {
            log.warn("Current user not found or disabled: {}", email);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = optionalUser.get();

        // Build response (no token in /me response)
        AuthResponse response = AuthResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .name(user.getName())
            .role(user.getRole().toString())
            .enabled(user.getEnabled())
            .message("Current user information retrieved")
            .build();

        return ResponseEntity.ok(response);
    }

    /**
     * GitHub OAuth callback endpoint.
     * POST /api/auth/github/callback
     */
    @PostMapping("/github/callback")
    public ResponseEntity<AuthResponse> githubCallback(
            @Valid @RequestBody OAuthCallbackRequest request) {

        log.info("GitHub OAuth callback received");

        try {
            String accessToken = oauthService.exchangeGitHubCodeForToken(request.getCode());
            OAuthService.GitHubUserInfo githubUser = oauthService.getGitHubUserInfo(accessToken);
            String rawEmail = oauthService.getGitHubUserEmail(accessToken);

            if (rawEmail == null || rawEmail.isEmpty()) {
                throw new IOException("Could not retrieve email from GitHub");
            }

            final String email = rawEmail.toLowerCase().trim();
            final OAuthService.GitHubUserInfo finalGithubUser = githubUser;

            User user = userService.findByEmail(email)
                    .orElseGet(() -> {
                        String randomPassword = UUID.randomUUID().toString();
                        User newUser = new User(
                                email,
                                finalGithubUser.name != null ? finalGithubUser.name : finalGithubUser.login,
                                randomPassword,
                                com.secureops.user.UserRole.USER
                        );
                        newUser.setGithubId(finalGithubUser.id.toString());
                        newUser.setProfilePictureUrl(finalGithubUser.avatar_url);
                        newUser.setAuthProvider("github");
                        log.info("Created new user from GitHub: {}", email);
                        return userService.saveUser(newUser);
                    });

            if (!userService.isUserEnabled(user)) {
                throw new IOException("Account disabled");
            }

            String jwtToken = jwtService.generateToken(user.getId(), user.getEmail());

            AuthResponse response = AuthResponse.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .role(user.getRole().toString())
                    .enabled(user.getEnabled())
                    .token(jwtToken)
                    .message("GitHub authentication successful")
                    .build();

            log.info("User authenticated via GitHub: {}", email);
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("GitHub OAuth error:", e);
            throw new com.secureops.auth.exception.InvalidCredentialsException("GitHub authentication failed");
        }
    }

    /**
     * Google OAuth callback endpoint.
     * POST /api/auth/google/callback
     */
    @PostMapping("/google/callback")
    public ResponseEntity<AuthResponse> googleCallback(
            @Valid @RequestBody OAuthCallbackRequest request,
            @RequestParam(required = false) String redirectUri) {

        log.info("Google OAuth callback received");

        try {
            String finalRedirectUri = redirectUri != null ? redirectUri : "http://localhost:5173/auth/google/callback";

            OAuthService.GoogleTokenResponse tokenResponse = oauthService.exchangeGoogleCodeForToken(request.getCode(), finalRedirectUri);
            OAuthService.GoogleUserInfo googleUser = oauthService.verifyGoogleIdToken(tokenResponse.id_token);

            String rawEmail = googleUser.email;
            if (rawEmail == null || rawEmail.isEmpty()) {
                throw new IOException("Could not retrieve email from Google");
            }

            final String email = rawEmail.toLowerCase().trim();
            final OAuthService.GoogleUserInfo finalGoogleUser = googleUser;

            User user = userService.findByEmail(email)
                    .orElseGet(() -> {
                        String randomPassword = UUID.randomUUID().toString();
                        User newUser = new User(
                                email,
                                finalGoogleUser.name != null ? finalGoogleUser.name : email.split("@")[0],
                                randomPassword,
                                com.secureops.user.UserRole.USER
                        );
                        newUser.setGoogleId(finalGoogleUser.id);
                        newUser.setProfilePictureUrl(finalGoogleUser.picture);
                        newUser.setAuthProvider("google");
                        log.info("Created new user from Google: {}", email);
                        return userService.saveUser(newUser);
                    });

            if (!userService.isUserEnabled(user)) {
                throw new IOException("Account disabled");
            }

            String jwtToken = jwtService.generateToken(user.getId(), user.getEmail());

            AuthResponse response = AuthResponse.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .role(user.getRole().toString())
                    .enabled(user.getEnabled())
                    .token(jwtToken)
                    .message("Google authentication successful")
                    .build();

            log.info("User authenticated via Google: {}", email);
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Google OAuth error:", e);
            throw new com.secureops.auth.exception.InvalidCredentialsException("Google authentication failed");
        }
    }
}
