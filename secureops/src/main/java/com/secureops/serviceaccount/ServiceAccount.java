package com.secureops.serviceaccount;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/serviceaccount/ServiceAccount.java
 * PURPOSE: Service account entity for CI/CD tools like Jenkins to authenticate securely.
 * WHY IT EXISTS: Allows Jenkins and other automated tools to authenticate without user credentials or JWT login.
 * SECURITY NOTES:
 * - Not a regular user; separate authentication mechanism
 * - API keys stored as hashed values
 * - Immutable after creation (keys cannot be changed, only revoked)
 * - Scoped permissions for least privilege
 * - Audit logging for all API key usage
 */
@Entity
@Table(name = "service_account", indexes = {
    @Index(name = "idx_service_account_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "name", nullable = false, length = 255, unique = true)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "api_key_hash", nullable = false, length = 255)
    private String apiKeyHash;

    @Column(name = "scope", nullable = false)
    @Enumerated(EnumType.STRING)
    private ServiceAccountScope scope;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    public ServiceAccount(String name, String description, String apiKeyHash, ServiceAccountScope scope, String createdBy) {
        this.name = name;
        this.description = description;
        this.apiKeyHash = apiKeyHash;
        this.scope = scope;
        this.enabled = true;
        this.createdAt = LocalDateTime.now();
        this.createdBy = createdBy;
    }

    /**
     * Check if this service account is currently valid.
     * Valid means: enabled AND (no expiration OR expiration in future)
     */
    public boolean isValid() {
        if (!enabled) {
            return false;
        }
        if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) {
            return false;
        }
        return true;
    }
}
