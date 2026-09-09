package com.meethybridhub.store;

import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface StoreDomainRepository extends TenantRepository<StoreDomain, Long> {

    Optional<StoreDomain> findByDomain(String domain);

    boolean existsByDomain(String domain);
}
