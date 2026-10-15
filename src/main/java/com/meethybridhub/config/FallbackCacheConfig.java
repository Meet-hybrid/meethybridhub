package com.meethybridhub.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConditionalOnMissingBean(name = "cacheManager")
public class FallbackCacheConfig {

    private static final String[] CACHE_NAMES = {
            "stores", "products", "categories", "users",
            "platformConfig", "featured", "commissionRules"
    };

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(CACHE_NAMES);
    }
}
