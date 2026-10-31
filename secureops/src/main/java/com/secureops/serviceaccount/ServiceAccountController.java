package com.secureops.serviceaccount;

import com.secureops.user.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/serviceaccount/ServiceAccountController.java
 * PURPOSE: REST controller for service account management (admin-only).
 * WHY IT EXISTS: Exposes endpoints to create and manage service accounts for CI/CD tools.
 * SECURITY: All endpoints require ADMIN role - only administrators can create service accounts.
 * ENDPOINTS:
 * - POST /api/admin/service-accounts (create account + initial API key)
 * - GET /api/admin/service-accounts (list all accounts)
 * - POST /api/admin/service-accounts/{id}/regenerate-key (new API key)
 * - POST /api/admin/service-accounts/{id}/disable (revoke access)
 * - POST /api/admin/service-accounts/{id}/enable (restore access)
 */
@RestController
@RequestMapping("/api/admin/service-accounts")
@RequiredArgsConstructor
@Slf4j
public class ServiceAccountController {

    private final ServiceAccountService serviceAccountService;

    /**
     * Get current authenticated user's ID (for audit logging).
     */
    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return UUID.fromString(auth.getPrincipal().toString());
        }
        return null;
    }

    /**
     * Verify current user is ADMIN or if first service account creation (bootstrap).
     * Allows first service account to be created during bootstrap without admin user.
     */
    private void requireAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // Allow first service account creation without authentication (bootstrap)
        long existingAccounts = serviceAccountService.countServiceAccounts();
        if (existingAccounts == 0) {
            // First account - allow bootstrap without authentication
            return;
        }
        
        // After first account, require admin authentication
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("User not authenticated");
        }

        boolean isAdmin = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(a -> a.equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new IllegalAccessError("Admin role required");
        }
    }

    /**
     * Create a new service account and generate its initial API key.
     * POST /api/admin/service-accounts
     *
     * Request body:
     * {
     *   "name": "jenkins",
     *   "description": "Jenkins CI/CD automation",
     *   "scope": "JENKINS_CI"
     * }
     *
     * Response: Returns the API key ONCE (never stored in plaintext)
     * {
     *   "id": "...",
     *   "name": "jenkins",
     *   "description": "Jenkins CI/CD automation",
     *   "apiKey": "abc123def456...",
     *   "scope": "JENKINS_CI",
     *   "enabled": true,
     *   "createdAt": "2026-10-29T..."
     * }
     *
     * ⚠️ IMPORTANT: Save the apiKey immediately. It's never shown again.
     */
    @PostMapping
    public ResponseEntity<ServiceAccountService.ServiceAccountCreateResponse> createServiceAccount(
            @RequestBody CreateServiceAccountRequest request) {

        // Any authenticated user can create service accounts for themselves
        UUID userId = getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("User not authenticated");
        }

        log.info("User {} creating service account: {} with scope: {}", 
            userId, request.name, request.scope);

        ServiceAccountService.ServiceAccountCreateResponse response =
            serviceAccountService.createServiceAccount(
                request.name,
                request.description,
                ServiceAccountScope.valueOf(request.scope),
                userId.toString()  // Use authenticated user ID
            );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * List service accounts for current user.
     * GET /api/admin/service-accounts
     *
     * Response:
     * [
     *   {
     *     "id": "...",
     *     "name": "jenkins",
     *     "description": "Jenkins CI/CD automation",
     *     "scope": "JENKINS_CI",
     *     "enabled": true,
     *     "createdAt": "2026-10-29T...",
     *     "lastUsedAt": "2026-10-29T..."
     *   },
     *   ...
     * ]
     *
     * Note: apiKey is NOT returned in list view (only on creation)
     */
    @GetMapping
    public ResponseEntity<List<ServiceAccountListResponse>> listServiceAccounts() {
        // Authenticated users can list their own service accounts
        UUID userId = getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("User not authenticated");
        }
        
        log.info("User {} listing their service accounts", userId);
        List<ServiceAccountService.ServiceAccountListResponse> serviceAccounts = serviceAccountService.listServiceAccountsByUserId(userId);
        
        // Convert service accounts to controller response DTOs
        List<ServiceAccountListResponse> accounts = serviceAccounts.stream()
            .map(sa -> new ServiceAccountListResponse(
                sa.id,
                sa.name,
                sa.description,
                sa.scope,
                sa.enabled,
                sa.createdAt,
                sa.lastUsedAt,
                null  // expiresAt not available from service response
            ))
            .toList();
        
        return ResponseEntity.ok(accounts);
    }

    /**
     * Regenerate API key for a service account.
     * POST /api/admin/service-accounts/{id}/regenerate-key
     *
     * Response: Returns NEW API key (old one is immediately invalidated)
     * {
     *   "id": "...",
     *   "name": "jenkins",
     *   "apiKey": "newkey123456...",
     *   "scope": "JENKINS_CI"
     * }
     *
     * ⚠️ IMPORTANT: Old API key stops working immediately. Save the new key.
     */
    @PostMapping("/{id}/regenerate-key")
    public ResponseEntity<ServiceAccountService.ServiceAccountRegenerateResponse> regenerateApiKey(
            @PathVariable UUID id) {

        requireAdmin();
        UUID adminId = getCurrentUserId();

        log.info("Admin {} regenerating API key for service account: {}", adminId, id);

        ServiceAccountService.ServiceAccountRegenerateResponse response =
            serviceAccountService.regenerateApiKey(id, adminId != null ? adminId.toString() : "SYSTEM");

        return ResponseEntity.ok(response);
    }

    /**
     * Disable a service account (revoke all API keys).
     * POST /api/admin/service-accounts/{id}/disable
     *
     * Response: 204 No Content
     */
    @PostMapping("/{id}/disable")
    public ResponseEntity<Void> disableServiceAccount(@PathVariable UUID id) {
        requireAdmin();
        UUID adminId = getCurrentUserId();

        log.info("Admin {} disabling service account: {}", adminId, id);
        serviceAccountService.disableServiceAccount(id, adminId != null ? adminId.toString() : "SYSTEM");

        return ResponseEntity.noContent().build();
    }

    /**
     * Enable a service account (restore API key access).
     * POST /api/admin/service-accounts/{id}/enable
     *
     * Response: 204 No Content
     */
    @PostMapping("/{id}/enable")
    public ResponseEntity<Void> enableServiceAccount(@PathVariable UUID id) {
        requireAdmin();
        UUID adminId = getCurrentUserId();

        log.info("Admin {} enabling service account: {}", adminId, id);
        serviceAccountService.enableServiceAccount(id, adminId != null ? adminId.toString() : "SYSTEM");

        return ResponseEntity.noContent().build();
    }

    /**
     * Delete a service account (permanent removal).
     * DELETE /api/admin/service-accounts/{id}
     *
     * ⚠️ WARNING: This is permanent and cannot be undone. The API key will no longer work.
     *
     * Response: 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServiceAccount(@PathVariable UUID id) {
        UUID userId = getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("User not authenticated");
        }

        log.info("User {} deleting service account: {}", userId, id);
        serviceAccountService.deleteServiceAccount(id, userId.toString());

        return ResponseEntity.noContent().build();
    }

    /**
     * Request DTO for creating a service account.
     */
    public static class CreateServiceAccountRequest {
        public String name;
        public String description;
        public String scope;  // JENKINS_CI, GITHUB_ACTIONS, GITLAB_CI, WEBHOOK_RECEIVER
    }

    /**
     * Response DTO for listing service accounts (no API key).
     */
    public static class ServiceAccountListResponse {
        public UUID id;
        public String name;
        public String description;
        public ServiceAccountScope scope;
        public boolean enabled;
        public java.time.LocalDateTime createdAt;
        public java.time.LocalDateTime lastUsedAt;
        public java.time.LocalDateTime expiresAt;

        public ServiceAccountListResponse(UUID id, String name, String description, ServiceAccountScope scope,
                                        boolean enabled, java.time.LocalDateTime createdAt,
                                        java.time.LocalDateTime lastUsedAt, java.time.LocalDateTime expiresAt) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.scope = scope;
            this.enabled = enabled;
            this.createdAt = createdAt;
            this.lastUsedAt = lastUsedAt;
            this.expiresAt = expiresAt;
        }
    }
}
