package com.meethybridhub.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Store} entities. Stores themselves are platform-level
 * (they ARE the tenants), so this extends plain {@link JpaRepository} — no
 * tenant filtering applies to the tenants table.
 */
@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /** The store owned by a user (store owners have exactly one). */
    Optional<Store> findByOwnerIdAndStatus(Long ownerId, StoreStatus status);

    List<Store> findByOwnerId(Long ownerId);

    /** All stores in a given lifecycle state (admin store management). */
    List<Store> findByStatus(StoreStatus status);
}
