package com.thuandao.weather_api.service.impl;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.thuandao.weather_api.dto.WeatherRequest;
import com.thuandao.weather_api.dto.WeatherResponse;
import com.thuandao.weather_api.exception.RateLimitExceededException;
import com.thuandao.weather_api.exception.WeatherServiceException;
import com.thuandao.weather_api.service.WeatherService;

import io.github.bucket4j.Bucket;

@Service
public class WeatherServiceImpl implements WeatherService {

    private static final Logger logger = LoggerFactory.getLogger(WeatherServiceImpl.class);

    @Autowired
    private WebClient weatherWebClient;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private Map<String, Bucket> rateLimitBuckets;

    @Value("${weather.api.key}")
    private String apiKey;

    @Value("${weather.api.url}")
    private String apiUrl;

    @Value("${cache.weather.ttl:3600}")
    private long cacheTtl;

    @Override
    @Cacheable(value = "weather", key = "#request.location()")
    public WeatherResponse getWeather(WeatherRequest request) {
        String location = request.location();
        String cacheKey = buildCacheKey(request);

        // Check rate limit
        Bucket bucket = rateLimitBuckets.computeIfAbsent(
                request.location(),
                k -> rateLimitBuckets.get("default"));

        if (!bucket.tryConsume(1)) {
            logger.warn("Rate limit exceeded for location: {}", location);
            throw new RateLimitExceededException("Rate limit exceeded. Please try again later.");
        }

        // Try to get from cache
        try {
            Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
            if (cachedValue instanceof WeatherResponse) {
                WeatherResponse cachedResponse = (WeatherResponse) cachedValue;
                logger.info("Cache hit for location: {}", location);
                return cachedResponse;
            }
        } catch (Exception e) {
            logger.warn("Error accessing cache: {}", e.getMessage());
            // Continue to API call on cache error
        }

        logger.info("Cache miss for location: {}, fetching from API", location);

        // Fetch from API
        try {
            WeatherResponse response = fetchFromApi(request);

            // Cache the result
            try {
                redisTemplate.opsForValue().set(cacheKey, response, Duration.ofSeconds(cacheTtl));
                logger.debug("Successfully cached response for location: {}", location);
            } catch (Exception e) {
                logger.warn("Failed to cache response: {}", e.getMessage());
                // Continue even if caching fails
            }

            return response;
        } catch (Exception e) {
            logger.error("Error fetching weather data: {}", e.getMessage(), e);
            throw new WeatherServiceException("Failed to retrieve weather data: " + e.getMessage());
        }
    }

    private WeatherResponse fetchFromApi(WeatherRequest request) {
        String url = buildApiUrl(request);

        logger.debug("Requesting weather data from: {}", url);

        return weatherWebClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::mapToWeatherResponse)
                .block();
    }

    private String buildApiUrl(WeatherRequest request) {
        StringBuilder url = new StringBuilder(apiUrl)
                .append("/")
                .append(request.location());

        if (request.date() != null && !request.date().isEmpty()) {
            url.append("/").append(request.date());
        }

        url.append("?unitGroup=metric&include=days&key=").append(apiKey);

        return url.toString();
    }

    private String buildCacheKey(WeatherRequest request) {
        return "weather:" + request.location() +
                (request.date() != null ? ":" + request.date() : "");
    }

    @SuppressWarnings("unchecked")
    private WeatherResponse mapToWeatherResponse(Map<String, Object> response) {
        logger.debug("Mapping API response to WeatherResponse");

        // Extract days forecasts
        var days = (java.util.List<Map<String, Object>>) response.get("days");

        return new WeatherResponse(
                (String) response.get("address"),
                (String) response.get("resolvedAddress"),
                (String) response.get("description"),
                getDoubleValue(response, "currentConditions.temp"),
                (String) getNestedValue(response, "currentConditions.conditions"),
                getDoubleValue(response, "currentConditions.humidity"),
                getDoubleValue(response, "currentConditions.windspeed"),
                days.stream()
                        .map(day -> new WeatherResponse.DayForecast(
                                (String) day.get("datetime"),
                                getDoubleValue(day, "tempmax"),
                                getDoubleValue(day, "tempmin"),
                                (String) day.get("conditions"),
                                getDoubleValue(day, "precipprob")))
                        .toList(),
                "Visual Crossing Weather API");
    }

    private Double getDoubleValue(Map<String, Object> data, String path) {
        Object value = getNestedValue(data, path);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    private Object getNestedValue(Map<String, Object> data, String path) {
        String[] parts = path.split("\\.");
        Object current = data;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
                if (current == null) {
                    return null;
                }
            } else {
                return null;
            }
        }

        return current;
    }
}