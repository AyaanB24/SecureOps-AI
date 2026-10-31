package com.secureops.serviceaccount;

import com.secureops.common.exception.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/serviceaccount/ServiceAccountService.java
 * PURPOSE: Business logic for service account management and API key generation.
 * WHY IT EXISTS: Encapsulates service account operations, API key generation, and validation.
 * DEPENDENCIES: Uses ServiceAccountRepository for persistence, BCryptPasswordEncoder for hashing.
 * SECURITY:
 * - API keys generated using SecureRandom (cryptographically secure)
 * - Keys hashed with BCrypt before storage
 * - Keys never logged or returned in responses (only once during creation)
 * - Service accounts have limited scope for least privilege
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ServiceAccountService {

    private final ServiceAccountRepository serviceAccountRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    private static final int API_KEY_LENGTH = 32; // 32 bytes = 256 bits
    private static final SecureRandom secureRandom = new SecureRandom();

    /**
     * Count total service accounts (for bootstrap check).
     */
    public long countServiceAccounts() {
        return serviceAccountRepository.count();
    }

    /**
     * Create a new service account and generate its initial API key.
     * Automatically disables any existing service account with the same name for this user.
     * Ensures only one active credential per name, per user.
     * The API key is returned only once - it's never stored in plaintext.
     *
     * @param name Friendly name for the account (e.g., "jenkins")
     * @param description Description of the account's purpose
     * @param scope Permission scope for the account
     * @param createdBy User ID creating this account (for audit and scoping)
     * @return ServiceAccountResponse containing the generated API key (one-time)
     */
    public ServiceAccountCreateResponse createServiceAccount(String name, String description, ServiceAccountScope scope, String createdBy) {
        log.info("Creating service account: {} with scope: {} for user: {}", name, scope, createdBy);

        // Check for existing account with same name for THIS USER and disable it (credential rotation)
        // This allows multiple users to have credentials with the same name (e.g., multiple teams with "jenkins")
        var existingAccount = serviceAccountRepository.findByNameAndCreatedBy(name, createdBy);
        if (existingAccount.isPresent()) {
            ServiceAccount oldAccount = existingAccount.get();
            if (oldAccount.isEnabled()) {
                oldAccount.setEnabled(false);
                serviceAccountRepository.save(oldAccount);
                log.info("Previous service account disabled for rotation: {} (ID: {}) for user: {}", name, oldAccount.getId(), createdBy);
            }
        }

        // Generate a new random API key
        String apiKey = generateApiKey();
        String apiKeyHash = passwordEncoder.encode(apiKey);

        // Create and save new account
        ServiceAccount account = new ServiceAccount(name, description, apiKeyHash, scope, createdBy);
        ServiceAccount saved = serviceAccountRepository.save(account);

        log.info("Service account created: {} (ID: {}) for user: {}", name, saved.getId(), createdBy);

        // Return response with the API key (only shown once)
        return new ServiceAccountCreateResponse(
            saved.getId(),
            saved.getName(),
            saved.getDescription(),
            apiKey,  // Return the plaintext key only this one time
            saved.getScope(),
            saved.isEnabled(),
            saved.getCreatedAt(),
            saved.getExpiresAt()
        );
    }

    /**
     * Get a service account by ID.
     */
    public ServiceAccount getServiceAccountById(UUID id) {
        return serviceAccountRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Service account not found: " + id));
    }

    /**
     * List all enabled service accounts.
     */
    public List<ServiceAccount> getAllServiceAccounts() {
        return serviceAccountRepository.findAllEnabled();
    }

    /**
     * List service accounts created by a specific user.
     */
    public List<ServiceAccountListResponse> listServiceAccountsByUserId(UUID userId) {
        List<ServiceAccount> accounts = serviceAccountRepository.findAll().stream()
            .filter(account -> account.getCreatedBy() != null && account.getCreatedBy().equals(userId.toString()))
            .toList();
        
        return accounts.stream()
            .map(account -> new ServiceAccountListResponse(
                account.getId(),
                account.getName(),
                account.getDescription(),
                account.getScope(),
                account.isEnabled(),
                account.getCreatedAt(),
                account.getLastUsedAt()
            ))
            .toList();
    }

    /**
     * List service accounts by scope (e.g., all JENKINS_CI accounts).
     */
    public List<ServiceAccount> getServiceAccountsByScope(ServiceAccountScope scope) {
        return serviceAccountRepository.findByScope(scope);
    }

    /**
     * Disable a service account (revokes all its API keys).
     * This is safer than deletion since it preserves audit trail.
     */
    public void disableServiceAccount(UUID id, String disabledBy) {
        ServiceAccount account = getServiceAccountById(id);
        account.setEnabled(false);
        serviceAccountRepository.save(account);
        log.info("Service account disabled: {} by {}", account.getName(), disabledBy);
    }

    /**
     * Enable a service account.
     */
    public void enableServiceAccount(UUID id, String enabledBy) {
        ServiceAccount account = getServiceAccountById(id);
        account.setEnabled(true);
        serviceAccountRepository.save(account);
        log.info("Service account enabled: {} by {}", account.getName(), enabledBy);
    }

    /**
     * Delete a service account completely (permanent removal).
     * Used when user wants to permanently remove a credential.
     * This is different from disable - deleted accounts cannot be recovered.
     *
     * @param id Service account ID
     * @param deletedBy User requesting deletion (for audit)
     * @throws IllegalArgumentException if account not found
     */
    public void deleteServiceAccount(UUID id, String deletedBy) {
        ServiceAccount account = getServiceAccountById(id);
        String accountName = account.getName();
        serviceAccountRepository.delete(account);
        log.info("Service account deleted: {} by {}", accountName, deletedBy);
    }

    /**
     * Regenerate a new API key for a service account.
     * Old key is immediately invalidated (replaced in hash).
     *
     * @param id Service account ID
     * @param regeneratedBy User requesting regeneration (for audit)
     * @return New API key (shown only once)
     */
    public ServiceAccountRegenerateResponse regenerateApiKey(UUID id, String regeneratedBy) {
        ServiceAccount account = getServiceAccountById(id);

        String newApiKey = generateApiKey();
        String newApiKeyHash = passwordEncoder.encode(newApiKey);

        account.setApiKeyHash(newApiKeyHash);
        serviceAccountRepository.save(account);

        log.info("API key regenerated for service account: {} by {}", account.getName(), regeneratedBy);

        return new ServiceAccountRegenerateResponse(
            account.getId(),
            account.getName(),
            newApiKey,  // Return only this once
            account.getScope()
        );
    }

    /**
     * Generate a cryptographically random API key.
     * 32 bytes (256 bits) encoded as Base64 = ~43 characters.
     *
     * @return Random API key as Base64 string
     */
    private String generateApiKey() {
        byte[] randomBytes = new byte[API_KEY_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return Base64.getEncoder().encodeToString(randomBytes);
    }

    /**
     * DTO: Response when creating a service account (includes one-time API key).
     */
    public static class ServiceAccountCreateResponse {
        public UUID id;
        public String name;
        public String description;
        public String apiKey;  // One-time only
        public ServiceAccountScope scope;
        public boolean enabled;
        public java.time.LocalDateTime createdAt;
        public java.time.LocalDateTime expiresAt;

        public ServiceAccountCreateResponse(UUID id, String name, String description, String apiKey,
                                          ServiceAccountScope scope, boolean enabled,
                                          java.time.LocalDateTime createdAt, java.time.LocalDateTime expiresAt) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.apiKey = apiKey;
            this.scope = scope;
            this.enabled = enabled;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
        }
    }

    /**
     * DTO: Response when regenerating an API key (includes new one-time key).
     */
    public static class ServiceAccountRegenerateResponse {
        public UUID id;
        public String name;
        public String apiKey;  // One-time only
        public ServiceAccountScope scope;

        public ServiceAccountRegenerateResponse(UUID id, String name, String apiKey, ServiceAccountScope scope) {
            this.id = id;
            this.name = name;
            this.apiKey = apiKey;
            this.scope = scope;
        }
    }

    /**
     * DTO: Response when listing service accounts (excludes API key).
     */
    public static class ServiceAccountListResponse {
        public UUID id;
        public String name;
        public String description;
        public ServiceAccountScope scope;
        public boolean enabled;
        public java.time.LocalDateTime createdAt;
        public java.time.LocalDateTime lastUsedAt;

        public ServiceAccountListResponse(UUID id, String name, String description, ServiceAccountScope scope,
                                         boolean enabled, java.time.LocalDateTime createdAt,
                                         java.time.LocalDateTime lastUsedAt) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.scope = scope;
            this.enabled = enabled;
            this.createdAt = createdAt;
            this.lastUsedAt = lastUsedAt;
        }
    }
}
