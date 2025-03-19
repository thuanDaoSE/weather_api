package com.thuandao.weather_api.config;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;

@Configuration
public class RateLimiterConfig {

    private static final int DEFAULT_CAPACITY = 10;
    private static final int DEFAULT_REFILL_TOKENS = 1;
    private static final Duration DEFAULT_REFILL_PERIOD = Duration.ofMinutes(1);

    @Bean
    public ConcurrentHashMap<String, Bucket> rateLimitBuckets() {
        return new ConcurrentHashMap<>();
    }

    public Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(DEFAULT_CAPACITY,
                Refill.intervally(DEFAULT_REFILL_TOKENS, DEFAULT_REFILL_PERIOD));
        return Bucket.builder().addLimit(limit).build();
    }
}