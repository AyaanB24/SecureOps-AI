package com.secureops.auth.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;

/**
 * FILE: src/main/java/com/secureops/auth/service/JwtService.java
 * PURPOSE: JWT token generation, validation, and claim extraction.
 * WHY IT EXISTS: Provides secure token-based authentication for stateless API communication.
 * DEPENDENCIES: Uses jjwt library for JWT operations.
 *
 * SECURITY NOTES:
 * - Secret key read from environment (never hardcoded)
 * - Token expiration enforced
 * - Signature verified on every validation
 * - Claims extracted and validated
 */
@Service
@Slf4j
public class JwtService {

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:3600000}")
    private long jwtExpirationMs;

    /**
     * Generate JWT token for an authenticated user.
     *
     * SECURITY:
     * - Includes user ID and email as claims
     * - Sets expiration time
     * - Signed with secret key
     *
     * @param userId User's UUID
     * @param email User's email
     * @return Signed JWT token (JWS format)
     */
    public String generateToken(UUID userId, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userId.toString());
        claims.put("email", email);

        return createToken(claims, userId.toString());
    }

    /**
     * Create JWT token with claims and expiration.
     * Uses HMAC-SHA256 algorithm (HS256).
     *
     * @param claims Token claims
     * @param subject User ID
     * @return Signed JWT token
     */
    private String createToken(Map<String, Object> claims, String subject) {
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            log.error("JWT secret not configured. Set app.jwt.secret environment variable.");
            throw new IllegalStateException("JWT secret is not configured");
        }

        long now = System.currentTimeMillis();
        Date issuedAt = new Date(now);
        Date expiresAt = new Date(now + jwtExpirationMs);

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        String token = Jwts.builder()
            .claims(claims)
            .subject(subject)
            .issuedAt(issuedAt)
            .expiration(expiresAt)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();

        log.debug("JWT token generated for user: {}", subject);
        return token;
    }

    /**
     * Validate JWT token signature, expiration, and structure.
     *
     * SECURITY:
     * - Verifies signature using secret key
     * - Checks expiration
     * - Throws exception on any validation failure
     *
     * @param token JWT token to validate
     * @return true if valid, throws exception otherwise
     * @throws JwtException on validation failure
     */
    public boolean validateToken(String token) {
        try {
            if (jwtSecret == null || jwtSecret.isEmpty()) {
                log.error("JWT secret not configured for validation");
                throw new IllegalStateException("JWT secret is not configured");
            }

            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);

            return true;
        } catch (SecurityException | MalformedJwtException | UnsupportedJwtException ex) {
            log.warn("JWT validation failed: Invalid token structure or signature");
            throw new JwtException("Invalid JWT token", ex);
        } catch (ExpiredJwtException ex) {
            log.warn("JWT validation failed: Token expired");
            throw new JwtException("JWT token has expired", ex);
        } catch (IllegalArgumentException ex) {
            log.warn("JWT validation failed: Empty or null token");
            throw new JwtException("JWT token is empty or null", ex);
        }
    }

    /**
     * Extract user ID (subject) from JWT token.
     * Does NOT validate the token; caller must validate first.
     *
     * @param token JWT token
     * @return User ID (UUID as string)
     * @throws JwtException if token is invalid
     */
    public String extractSubject(String token) {
        try {
            if (jwtSecret == null || jwtSecret.isEmpty()) {
                throw new IllegalStateException("JWT secret is not configured");
            }

            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            return claims.getSubject();
        } catch (JwtException ex) {
            log.warn("Failed to extract subject from token");
            throw ex;
        }
    }

    /**
     * Extract email claim from JWT token.
     * Does NOT validate the token; caller must validate first.
     *
     * @param token JWT token
     * @return Email from token claims
     * @throws JwtException if token is invalid
     */
    public String extractEmail(String token) {
        try {
            if (jwtSecret == null || jwtSecret.isEmpty()) {
                throw new IllegalStateException("JWT secret is not configured");
            }

            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            return claims.get("email", String.class);
        } catch (JwtException ex) {
            log.warn("Failed to extract email from token");
            throw ex;
        }
    }

    /**
     * Get JWT expiration time in milliseconds.
     * Used for client-side token refresh scheduling.
     *
     * @return Expiration time in milliseconds
     */
    public long getExpirationMs() {
        return jwtExpirationMs;
    }
}
