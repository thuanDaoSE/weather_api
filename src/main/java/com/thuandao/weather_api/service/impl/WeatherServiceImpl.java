package com.thuandao.weather_api.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.thuandao.weather_api.config.RateLimiterConfig;
import com.thuandao.weather_api.dto.WeatherRequest;
import com.thuandao.weather_api.dto.WeatherResponse;
import com.thuandao.weather_api.dto.WeatherResponse.DayForecast;
import com.thuandao.weather_api.exception.RateLimitExceededException;
import com.thuandao.weather_api.exception.WeatherServiceException;
import com.thuandao.weather_api.service.WeatherService;

import io.github.bucket4j.Bucket;

@Service
public class WeatherServiceImpl implements WeatherService {
    private static final Logger logger = LoggerFactory.getLogger(WeatherServiceImpl.class);
    private static final String CACHE_KEY_PREFIX = "weather:";
    private static final int MAX_FORECAST_DAYS = 5;
    private static final String API_SOURCE = "Visual Crossing";

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

    @Value("${weather.cache.ttl:43200}")
    private Long cacheTtl;

    @Override
    public WeatherResponse getWeather(WeatherRequest request) {
        String ip = "127.0.0.1"; // Replace with actual IP capture in a real app

        // Check rate limit
        Bucket bucket = rateLimitBuckets.computeIfAbsent(ip, k -> rateLimiterConfig.createNewBucket());
        if (!bucket.tryConsume(1)) {
            logger.warn("Rate limit exceeded for IP: {}", ip);
            throw new RateLimitExceededException("Rate limit exceeded. Please try again later.");
        }

        // Normalize location name for consistent caching
        String normalizedLocation = request.location().trim().toLowerCase();
        String cacheKey = CACHE_KEY_PREFIX + normalizedLocation;

        if (request.date() != null && !request.date().isEmpty()) {
            cacheKey += ":" + request.date();
        }

        try {
            // Try to get from cache
            WeatherResponse cachedResponse = weatherRedisTemplate.opsForValue().get(cacheKey);
            if (cachedResponse != null) {
                logger.info("Cache hit for location: {}", request.location());
                return cachedResponse;
            }

            logger.info("Cache miss for location: {}, fetching from API", request.location());
            WeatherResponse response = fetchFromApi(request);

            // Cache the result
            try {
                weatherRedisTemplate.opsForValue().set(cacheKey, response, Duration.ofSeconds(cacheTtl));
                logger.debug("Cached weather data for location: {}", request.location());
            } catch (Exception e) {
                logger.warn("Failed to cache weather data: {}", e.getMessage());
                // Don't fail the request if caching fails
            }

            return response;
        } catch (WebClientResponseException e) {
            logger.error("API error for location {}: {} - {}", request.location(), e.getStatusCode(), e.getMessage());
            throw new WeatherServiceException("Error fetching weather data: " + e.getMessage(), e);
        } catch (Exception e) {
            if (!(e instanceof RateLimitExceededException)) {
                logger.error("Unexpected error getting weather for {}: {}", request.location(), e.getMessage(), e);
            }
            throw e;
        }
    }

    private WeatherResponse fetchFromApi(WeatherRequest request) {
        String requestUrl = buildApiUrl(request);
        logger.debug("Fetching weather data from URL: {}", requestUrl);

        try {
            JsonNode response = weatherWebClient.get()
                    .uri(requestUrl)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            return mapToWeatherResponse(response, request.location());
        } catch (Exception e) {
            logger.error("Error fetching data from weather API: {}", e.getMessage());
            throw new WeatherServiceException("Failed to fetch weather data from external API", e);
        }
    }

    private String buildApiUrl(WeatherRequest request) {
        StringBuilder urlBuilder = new StringBuilder(apiUrl);
        urlBuilder.append("/").append(request.location());

        if (request.date() != null && !request.date().trim().isEmpty()) {
            urlBuilder.append("/").append(request.date());
        }

        urlBuilder.append("?unitGroup=us&key=").append(apiKey).append("&contentType=json");
        return urlBuilder.toString();
    }

    private WeatherResponse mapToWeatherResponse(JsonNode response, String location) {
        if (response == null || !response.has("currentConditions")) {
            logger.warn("Invalid response format from weather API");
            throw new WeatherServiceException("Invalid response from weather API");
        }

        JsonNode currentConditions = response.get("currentConditions");

        // Build forecast list
        List<DayForecast> forecast = new ArrayList<>();
        JsonNode days = response.get("days");
        if (days != null && days.isArray()) {
            for (int i = 0; i < Math.min(days.size(), MAX_FORECAST_DAYS); i++) {
                JsonNode day = days.get(i);
                DayForecast dayForecast = new DayForecast();
                dayForecast.setDate(day.get("datetime").asText());
                dayForecast.setTempMax(day.get("tempmax").asDouble());
                dayForecast.setTempMin(day.get("tempmin").asDouble());
                dayForecast.setConditions(day.get("conditions").asText());
                dayForecast.setPrecipProbability(day.get("precipprob").asDouble());
                forecast.add(dayForecast);
            }
        }

        WeatherResponse weatherResponse = new WeatherResponse();
        weatherResponse.setLocation(location);
        weatherResponse.setResolvedAddress(response.get("resolvedAddress").asText());
        weatherResponse.setDescription(response.get("description").asText());
        weatherResponse.setCurrentTemp(currentConditions.get("temp").asDouble());
        weatherResponse.setConditions(currentConditions.get("conditions").asText());
        weatherResponse.setHumidity(currentConditions.get("humidity").asDouble());
        weatherResponse.setWindSpeed(currentConditions.get("windspeed").asDouble());
        weatherResponse.setForecast(forecast);
        weatherResponse.setSource(API_SOURCE);

        return weatherResponse;
    }
}