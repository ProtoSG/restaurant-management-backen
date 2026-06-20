package com.restaurant_management.restaurant_management_backend.shared.config;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
public class CacheConfig {

  @Bean
  public CacheManager cacheManager() {
    var dailySummary = build("analytics-daily",   2, TimeUnit.MINUTES, 50);
    var overview     = build("analytics-overview", 2, TimeUnit.MINUTES, 50);
    var intraday     = build("analytics-intraday", 1, TimeUnit.MINUTES, 50);
    var earnings     = build("analytics-earnings", 5, TimeUnit.MINUTES, 50);
    var topProducts  = build("analytics-top",      5, TimeUnit.MINUTES, 50);
    var weekly       = build("analytics-weekly",   5, TimeUnit.MINUTES, 50);

    var manager = new SimpleCacheManager();
    manager.setCaches(List.of(dailySummary, overview, intraday, earnings, topProducts, weekly));
    return manager;
  }

  private CaffeineCache build(String name, long ttl, TimeUnit unit, int maxSize) {
    return new CaffeineCache(name,
        Caffeine.newBuilder()
            .expireAfterWrite(ttl, unit)
            .maximumSize(maxSize)
            .build());
  }
}
