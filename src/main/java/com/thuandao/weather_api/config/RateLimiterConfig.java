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

    @Value("${weather.ratelimit.capacity:10}")
    private int capacity;

    @Value("${weather.ratelimit.refill-tokens:1}")
    private int refillTokens;

    @Value("${weather.ratelimit.refill-time-unit:MINUTES}")
    private String refillTimeUnit;

    @Bean
    public ConcurrentHashMap<String, Bucket> rateLimitBuckets() {
        return new ConcurrentHashMap<>();
    }

    public Bucket createNewBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(capacity, Refill.intervally(refillTokens, Duration.ofMinutes(1))))
                .build();
    }
}