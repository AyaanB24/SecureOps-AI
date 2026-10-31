package com.secureops.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/user/UserRepository.java
 * PURPOSE: Spring Data JPA repository for User entity persistence.
 * WHY IT EXISTS: Provides database access methods for user authentication and authorization.
 * DEPENDENCIES: Used by UserService for CRUD operations and queries.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find a user by email address.
     * Email is case-insensitive due to normalization in User entity.
     *
     * @param email User's email (will be normalized during storage)
     * @return Optional containing User if found, empty otherwise
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user exists with the given email.
     * Used for validation before account creation.
     *
     * @param email User's email address
     * @return true if user exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Find a user by ID.
     * This is provided by JpaRepository but documented for completeness.
     *
     * @param id User's UUID
     * @return Optional containing User if found, empty otherwise
     */
    Optional<User> findById(UUID id);
}
