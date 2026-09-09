package com.meethybridhub.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findBySlug(String slug);

    boolean existsBySlug(String slug);


    Optional<Store> findByOwnerIdAndStatus(Long ownerId, StoreStatus status);

    List<Store> findByOwnerId(Long ownerId);


    List<Store> findByStatus(StoreStatus status);
}
