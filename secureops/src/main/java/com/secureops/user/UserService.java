package com.secureops.user;

import com.secureops.auth.exception.DuplicateEmailException;
import com.secureops.auth.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/user/UserService.java
 * PURPOSE: Business logic for user management.
 * WHY IT EXISTS: Encapsulates user operations (retrieval, validation) for use by controllers and other services.
 * DEPENDENCIES: Uses UserRepository for persistence, will use PasswordEncoder in Phase 3.
 *
 * SECURITY NOTES:
 * - Password hash is never logged or exposed
 * - Email is normalized before storage and queries
 * - Methods validate access and return safe responses
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Find a user by email address.
     * Normalizes email before lookup (lowercase and trimmed).
     *
     * @param email User's email address
     * @return Optional containing User if found, empty otherwise
     */
    public Optional<User> findByEmail(String email) {
        String normalizedEmail = email.toLowerCase().trim();
        log.debug("Looking up user by email: {}", normalizedEmail);
        return userRepository.findByEmail(normalizedEmail);
    }

    /**
     * Find a user by UUID.
     *
     * @param id User's UUID
     * @return Optional containing User if found, empty otherwise
     */
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    /**
     * Check if an email address is already registered.
     * Used for duplicate prevention during registration.
     *
     * @param email Email address to check
     * @return true if user exists with this email, false otherwise
     */
    public boolean emailExists(String email) {
        String normalizedEmail = email.toLowerCase().trim();
        return userRepository.existsByEmail(normalizedEmail);
    }

    /**
     * Save a new user to the database.
     * This method is used internally; public registration will be handled in Phase 3.
     *
     * IMPORTANT: Caller must ensure password is already hashed with BCrypt.
     *
     * @param user User entity with hashed password
     * @return Saved user with generated UUID
     */
    public User saveUser(User user) {
        // Normalize email before saving
        user.setEmail(user.getEmail().toLowerCase().trim());
        log.info("Creating new user: {}", user.getEmail());
        return userRepository.save(user);
    }

    /**
     * Get a user's public profile (safe to expose through API).
     * This method should be used to construct API response DTOs,
     * ensuring password hash is never exposed.
     *
     * @param user User entity
     * @return User object (caller should map to DTO, excluding password)
     */
    public User getUserProfile(User user) {
        if (user == null) {
            return null;
        }
        // In Phase 6, this will return a UserResponse DTO instead
        // For now, just return the user; callers must not expose password
        return user;
    }

    /**
     * Verify a user account is enabled.
     *
     * @param user User entity
     * @return true if account is enabled, false otherwise
     */
    public boolean isUserEnabled(User user) {
        return user != null && user.getEnabled() != null && user.getEnabled();
    }

    /**
     * Register a new user with email, name, password, and USER role (always).
     * This method enforces public registration rules: no ADMIN self-registration.
     *
     * SECURITY:
     * - Email normalized before checking existence
     * - Password hashed with BCrypt before storage
     * - Role always set to USER (client-supplied role ignored)
     * - Disabled users cannot be created via registration (enabled=true always)
     *
     * @param email User's email address
     * @param name User's full name
     * @param plainPassword Plaintext password (will be hashed)
     * @return Registered User entity (password hash not exposed)
     * @throws DuplicateEmailException if email already registered
     */
    public User registerUser(String email, String name, String plainPassword) {
        String normalizedEmail = email.toLowerCase().trim();

        // Check for duplicate email
        if (emailExists(normalizedEmail)) {
            log.warn("Registration attempt with duplicate email: {}", normalizedEmail);
            throw new DuplicateEmailException("Email is already registered");
        }

        // Hash password using BCrypt
        String hashedPassword = passwordEncoder.encode(plainPassword);
        log.debug("Password hashed for registration: {}", normalizedEmail);

        // Create user with USER role (never ADMIN via registration)
        User user = new User(normalizedEmail, name, hashedPassword, UserRole.USER);
        User savedUser = userRepository.save(user);
        log.info("New user registered: {}", normalizedEmail);
        return savedUser;
    }

    /**
     * Authenticate a user with email and password.
     *
     * SECURITY:
     * - Email normalized before lookup
     * - Password compared against hash using BCrypt
     * - Disabled accounts rejected
     * - Nonexistent accounts and wrong passwords treated identically (prevents user enumeration)
     *
     * @param email User's email address
     * @param plainPassword Plaintext password to verify
     * @return Authenticated User entity
     * @throws InvalidCredentialsException if email not found, password wrong, or account disabled
     */
    public User authenticateUser(String email, String plainPassword) {
        String normalizedEmail = email.toLowerCase().trim();

        Optional<User> optionalUser = findByEmail(normalizedEmail);
        if (optionalUser.isEmpty()) {
            log.warn("Login attempt with nonexistent email: {}", normalizedEmail);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        User user = optionalUser.get();

        // Verify password hash
        if (!passwordEncoder.matches(plainPassword, user.getPasswordHash())) {
            log.warn("Login attempt with wrong password for: {}", normalizedEmail);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // Check if account is enabled
        if (!isUserEnabled(user)) {
            log.warn("Login attempt for disabled account: {}", normalizedEmail);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        log.info("User authenticated successfully: {}", normalizedEmail);
        return user;
    }
}
