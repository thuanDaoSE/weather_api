package com.thuandao.weather_api.service.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.thuandao.weather_api.config.RateLimiterConfig;
import com.thuandao.weather_api.dto.WeatherRequest;
import com.thuandao.weather_api.dto.WeatherResponse;
import com.thuandao.weather_api.dto.WeatherResponse.DayForecast;
import com.thuandao.weather_api.exception.RateLimitExceededException;
import com.thuandao.weather_api.service.WeatherService;

import io.github.bucket4j.Bucket;

@Service
public class WeatherServiceImpl implements WeatherService {

    private static final Logger logger = LoggerFactory.getLogger(WeatherServiceImpl.class);

    @Autowired
    private WebClient weatherWebClient;

    @Autowired
    private RedisTemplate<String, WeatherResponse> weatherRedisTemplate;

    @Autowired
    private RateLimiterConfig rateLimiterConfig;

    @Autowired
    private ConcurrentHashMap<String, Bucket> rateLimitBuckets;

    @Value("${weather.api.url}")
    private String apiUrl;

    @Value("${weather.api.key}")
    private String apiKey;

    @Value("${weather.cache.ttl}")
    private long cacheTtl;

    @Override
    public WeatherResponse getWeather(WeatherRequest request) {
        String cacheKey = createCacheKey(request);

        // Check rate limit
        String clientIp = "default"; // In a real application, get from request
        Bucket bucket = rateLimitBuckets.computeIfAbsent(clientIp, k -> rateLimiterConfig.createNewBucket());

        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("Rate limit exceeded. Please try again later.");
        }

        // Check cache
        WeatherResponse cachedResponse = weatherRedisTemplate.opsForValue().get(cacheKey);
        if (cachedResponse != null) {
            logger.info("Cache hit for key: {}", cacheKey);
            return cachedResponse;
        }

        logger.info("Cache miss for key: {}, fetching from API", cacheKey);
        // Call third-party API
        WeatherResponse response = fetchFromApi(request);

        // Cache the result
        weatherRedisTemplate.opsForValue().set(cacheKey, response, Duration.ofSeconds(cacheTtl));

        return response;
    }

    private String createCacheKey(WeatherRequest request) {
        if (request.date() != null && !request.date().trim().isEmpty()) {
            return "weather:" + request.location() + ":" + request.date();
        }
        return "weather:" + request.location() + ":current";
    }

    private WeatherResponse fetchFromApi(WeatherRequest request) {
        String requestUrl = buildApiUrl(request);

        JsonNode response = weatherWebClient.get()
                .uri(requestUrl)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        return mapToWeatherResponse(response, request.location());
    }

    private String buildApiUrl(WeatherRequest request) {
        StringBuilder urlBuilder = new StringBuilder(apiUrl);
        urlBuilder.append("/").append(request.location());

        if (request.date() != null && !request.date().trim().isEmpty()) {
            urlBuilder.append("/").append(request.date());
        }

        urlBuilder.append("?unitGroup=metric&include=days,current&key=").append(apiKey);
        return urlBuilder.toString();
    }

    private WeatherResponse mapToWeatherResponse(JsonNode response, String location) {
        JsonNode currentConditions = response.get("currentConditions");

        // Build forecast list
        List<DayForecast> forecast = new ArrayList<>();
        JsonNode days = response.get("days");
        if (days != null && days.isArray()) {
            for (int i = 0; i < Math.min(days.size(), 5); i++) {
                JsonNode day = days.get(i);
                forecast.add(DayForecast.builder()
                        .date(day.get("datetime").asText())
                        .tempMax(day.get("tempmax").asDouble())
                        .tempMin(day.get("tempmin").asDouble())
                        .conditions(day.get("conditions").asText())
                        .precipProbability(day.get("precipprob").asDouble())
                        .build());
            }
        }

        return WeatherResponse.builder()
                .location(location)
                .resolvedAddress(response.get("resolvedAddress").asText())
                .description(response.get("description").asText())
                .currentTemp(currentConditions.get("temp").asDouble())
                .conditions(currentConditions.get("conditions").asText())
                .humidity(currentConditions.get("humidity").asDouble())
                .windSpeed(currentConditions.get("windspeed").asDouble())
                .forecast(forecast)
                .source("Visual Crossing")
                .build();
    }
}