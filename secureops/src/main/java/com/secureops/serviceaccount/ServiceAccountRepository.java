package com.secureops.serviceaccount;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/serviceaccount/ServiceAccountRepository.java
 * PURPOSE: Spring Data JPA repository for ServiceAccount entity.
 * WHY IT EXISTS: Provides CRUD operations for service accounts and API key lookups.
 * DEPENDENCIES: Extends JpaRepository; used by ServiceAccountService.
 */
@Repository
public interface ServiceAccountRepository extends JpaRepository<ServiceAccount, UUID> {

    /**
     * Find a service account by name.
     * Used to look up accounts by friendly name.
     */
    Optional<ServiceAccount> findByName(String name);

    /**
     * Find a service account by name and creator (user-scoped).
     * Used for credential rotation - ensures each user's credentials are independent.
     */
    Optional<ServiceAccount> findByNameAndCreatedBy(String name, String createdBy);

    /**
     * Find enabled service accounts.
     * Used for listing active accounts.
     */
    @Query("SELECT sa FROM ServiceAccount sa WHERE sa.enabled = true ORDER BY sa.createdAt DESC")
    List<ServiceAccount> findAllEnabled();

    /**
     * Find service accounts by scope.
     * Used for querying accounts with specific permissions.
     */
    @Query("SELECT sa FROM ServiceAccount sa WHERE sa.scope = :scope AND sa.enabled = true")
    List<ServiceAccount> findByScope(@Param("scope") ServiceAccountScope scope);
}
