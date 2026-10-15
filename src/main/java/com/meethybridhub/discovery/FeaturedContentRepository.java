package com.meethybridhub.discovery;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface FeaturedContentRepository extends JpaRepository<FeaturedContent, Long> {

    @Query("SELECT f FROM FeaturedContent f WHERE f.active = true AND f.contentType = :type " +
            "AND (f.startsAt IS NULL OR f.startsAt <= :now) " +
            "AND (f.endsAt IS NULL OR f.endsAt > :now) " +
            "ORDER BY f.sortOrder ASC")
    List<FeaturedContent> findActiveByType(FeaturedContentType type, Instant now);

    @Query("SELECT f FROM FeaturedContent f WHERE f.active = true " +
            "AND (f.startsAt IS NULL OR f.startsAt <= :now) " +
            "AND (f.endsAt IS NULL OR f.endsAt > :now) " +
            "ORDER BY f.sortOrder ASC")
    List<FeaturedContent> findAllActive(Instant now);
}
