package com.meethybridhub.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;


@NoRepositoryBean
public interface TenantRepository<T extends TenantEntity, ID> extends JpaRepository<T, ID> {

    List<T> findAllByStoreId(Long storeId);

    Optional<T> findByIdAndStoreId(ID id, Long storeId);

    boolean existsByIdAndStoreId(ID id, Long storeId);

    long countByStoreId(Long storeId);
}
