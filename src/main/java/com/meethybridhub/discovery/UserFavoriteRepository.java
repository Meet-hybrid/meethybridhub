package com.meethybridhub.discovery;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserFavoriteRepository extends JpaRepository<UserFavorite, Long> {

    List<UserFavorite> findByUserIdAndEntityTypeOrderByCreatedAtDesc(Long userId, FavoriteEntityType entityType);

    Optional<UserFavorite> findByUserIdAndEntityTypeAndEntityId(Long userId, FavoriteEntityType entityType, Long entityId);

    boolean existsByUserIdAndEntityTypeAndEntityId(Long userId, FavoriteEntityType entityType, Long entityId);

    void deleteByUserIdAndEntityTypeAndEntityId(Long userId, FavoriteEntityType entityType, Long entityId);
}
