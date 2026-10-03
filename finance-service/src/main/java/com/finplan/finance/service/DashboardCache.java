package com.finplan.finance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/** Cache-aside for the dashboard. Redis being down must never break the API, so every call degrades to a miss. */
@Component
public class DashboardCache {
    private static final Logger log = LoggerFactory.getLogger(DashboardCache.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final Duration ttl;

    public DashboardCache(StringRedisTemplate redis, ObjectMapper mapper,
                          @Value("${finplan.dashboard.cache-ttl-seconds}") long ttlSeconds) {
        this.redis = redis;
        this.mapper = mapper;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    public Optional<DashboardService.DashboardResponse> get(UUID userId) {
        try {
            String json = redis.opsForValue().get(key(userId));
            return json == null ? Optional.empty() : Optional.of(mapper.readValue(json, DashboardService.DashboardResponse.class));
        } catch (Exception e) {
            log.warn("Dashboard cache read failed, treating as miss: {}", e.toString());
            return Optional.empty();
        }
    }

    public void put(UUID userId, DashboardService.DashboardResponse value) {
        try {
            redis.opsForValue().set(key(userId), mapper.writeValueAsString(value), ttl);
        } catch (Exception e) {
            log.warn("Dashboard cache write failed: {}", e.toString());
        }
    }

    public void evict(UUID userId) {
        try {
            redis.delete(key(userId));
        } catch (Exception e) {
            log.warn("Dashboard cache evict failed (entry expires in at most {}): {}", ttl, e.toString());
        }
    }

    private static String key(UUID userId) { return "dashboard:" + userId; }
}
