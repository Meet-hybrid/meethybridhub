package com.meethybridhub.admin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlatformConfigRepository extends JpaRepository<PlatformConfig, Long> {

    Optional<PlatformConfig> findByConfigKey(String configKey);
}
