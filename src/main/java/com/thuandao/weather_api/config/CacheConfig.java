package com.thuandao.weather_api.config;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.thuandao.weather_api.dto.WeatherResponse;

import io.github.bucket4j.Bucket;

@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger logger = LoggerFactory.getLogger(CacheConfig.class);

    @Bean
    @Primary
    public CacheManager cacheManager() {
        // Fallback to in-memory cache if Redis is not available
        logger.info("Configuring in-memory cache manager as primary");
        return new ConcurrentMapCacheManager();
    }

    @Bean
    public RedisTemplate<String, WeatherResponse> weatherRedisTemplate(RedisConnectionFactory connectionFactory) {
        try {
            RedisTemplate<String, WeatherResponse> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);
            template.setKeySerializer(new StringRedisSerializer());
            template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
            template.afterPropertiesSet();
            logger.info("Successfully configured Redis template");
            return template;
        } catch (Exception e) {
            logger.warn("Failed to configure Redis template, creating fallback: {}", e.getMessage());
            // Create a dummy RedisTemplate that will be caught by try-catch in service
            RedisTemplate<String, WeatherResponse> fallbackTemplate = new RedisTemplate<>();
            return fallbackTemplate;
        }
    }
}