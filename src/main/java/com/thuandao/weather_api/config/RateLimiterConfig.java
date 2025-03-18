package com.thuandao.weather_api.config;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;

@Configuration
public class RateLimiterConfig {

    @Value("${weather.ratelimit.capacity}")
    private long capacity;

    @Value("${weather.ratelimit.refill-tokens}")
    private long refillTokens;

    @Value("${weather.ratelimit.refill-time-unit}")
    private String refillTimeUnit;

    @Bean
    public ConcurrentHashMap<String, Bucket> rateLimitBuckets() {
        return new ConcurrentHashMap<>();
    }

    public Bucket createNewBucket() {
        Duration duration = switch (refillTimeUnit.toUpperCase()) {
            case "SECONDS" -> Duration.ofSeconds(1);
            case "MINUTES" -> Duration.ofMinutes(1);
            case "HOURS" -> Duration.ofHours(1);
            default -> Duration.ofMinutes(1);
        };

        Bandwidth limit = Bandwidth.classic(capacity, Refill.greedy(refillTokens, duration));
        return Bucket.builder().addLimit(limit).build();
    }
}