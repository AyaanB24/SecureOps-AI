package com.secureops.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/user/User.java
 * PURPOSE: JPA entity representing a SecureOps user account.
 * WHY IT EXISTS: Provides user authentication and role-based authorization. Users own projects and have different permission levels.
 * DEPENDENCIES: Used by UserRepository and UserService. Referenced by other entities for ownership tracking.
 *
 * SECURITY NOTES:
 * - Password is stored only as BCrypt hash, never plaintext
 * - Password hash is NOT exposed through any API response
 * - Email is normalized and unique across the system
 * - Enabled flag allows account suspension without deletion
 */
@Entity
@Table(name = "\"user\"", indexes = {
    @Index(name = "idx_user_email", columnList = "email")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_email", columnNames = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * BCrypt password hash. Never stored as plaintext.
     * Must be generated using BCryptPasswordEncoder.
     * This field is marked as nullable=false but is set during account creation.
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /**
     * User role determining authorization level.
     * - ADMIN: Full administrative access
     * - USER: Regular user, restricted to own projects
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private UserRole role;

    /**
     * Account enabled status.
     * true: Account is active
     * false: Account is suspended (allows non-destructive deactivation)
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    // OAuth fields
    @Column(name = "github_id", unique = true, nullable = true, length = 255)
    private String githubId;

    @Column(name = "google_id", unique = true, nullable = true, length = 255)
    private String googleId;

    @Column(name = "profile_picture_url", nullable = true, length = 500)
    private String profilePictureUrl;

    @Column(name = "auth_provider", nullable = true, length = 50)
    private String authProvider; // 'email', 'github', 'google'

    /**
     * Constructor for creating new users (ID will be auto-generated).
     * Password hash must be set separately using BCrypt.
     *
     * @param email User's email address (will be normalized)
     * @param name User's full name
     * @param passwordHash BCrypt hashed password
     * @param role User's role (ADMIN or USER)
     */
    public User(String email, String name, String passwordHash, UserRole role) {
        this.email = email.toLowerCase().trim(); // Normalize email
        this.name = name;
        this.passwordHash = passwordHash;
        this.role = role;
        this.enabled = true;
        this.authProvider = "email";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Update the updatedAt timestamp before persisting changes.
     * This is called automatically by JPA @PreUpdate hook.
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // OAuth getters and setters
    public String getGithubId() {
        return githubId;
    }

    public void setGithubId(String githubId) {
        this.githubId = githubId;
    }

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }
}
