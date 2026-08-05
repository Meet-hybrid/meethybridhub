package com.meethybridhub.store;

import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link StoreDomain}. Extends {@link TenantRepository}, so
 * every read is automatically scoped to a {@code store_id}.
 */
@Repository
public interface StoreDomainRepository extends TenantRepository<StoreDomain, Long> {

    Optional<StoreDomain> findByDomain(String domain);

    boolean existsByDomain(String domain);
}
