package com.thuandao.weather_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.thuandao.weather_api.config.RateLimiterConfig;
import com.thuandao.weather_api.dto.WeatherRequest;
import com.thuandao.weather_api.dto.WeatherResponse;
import com.thuandao.weather_api.exception.RateLimitExceededException;
import com.thuandao.weather_api.service.impl.WeatherServiceImpl;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
public class WeatherServiceRateLimitTest {

    @Mock
    private WebClient weatherWebClient;

    @Mock
    private RedisTemplate<String, WeatherResponse> weatherRedisTemplate;

    @Mock
    private RateLimiterConfig rateLimiterConfig;

    @Mock
    private ConcurrentHashMap<String, Bucket> rateLimitBuckets;

    @Mock
    private ValueOperations<String, WeatherResponse> valueOperations;

    @InjectMocks
    private WeatherServiceImpl weatherService;

    private WeatherRequest request;
    private Bucket realBucket;
    private JsonNode mockResponse;

    @BeforeEach
    void setUp() throws Exception {
        request = new WeatherRequest("New York", null);

        // Setup Redis mock
        when(weatherRedisTemplate.opsForValue()).thenReturn(valueOperations);

        // Create a real bucket with very limited capacity for testing
        Bandwidth limit = Bandwidth.classic(2, Refill.greedy(1, java.time.Duration.ofHours(1)));
        realBucket = Bucket.builder().addLimit(limit).build();

        // Set up rate limiter
        when(rateLimitBuckets.computeIfAbsent(anyString(), any())).thenReturn(realBucket);

        // Set service properties
        ReflectionTestUtils.setField(weatherService, "apiUrl", "https://api.visualcrossing.com/weather");
        ReflectionTestUtils.setField(weatherService, "apiKey", "test_key");
        ReflectionTestUtils.setField(weatherService, "cacheTtl", 43200L);

        // Create mock JSON response
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonNode = mapper.createObjectNode();

        ObjectNode currentConditions = mapper.createObjectNode();
        currentConditions.put("temp", 22.5);
        currentConditions.put("conditions", "Clear");
        currentConditions.put("humidity", 65.2);
        currentConditions.put("windspeed", 5.4);

        ObjectNode day = mapper.createObjectNode();
        day.put("datetime", "2023-06-10");
        day.put("tempmax", 28.5);
        day.put("tempmin", 18.2);
        day.put("conditions", "Clear");
        day.put("precipprob", 0.0);

        jsonNode.put("resolvedAddress", "New York, NY, USA");
        jsonNode.put("description", "Clear conditions throughout the day.");
        jsonNode.set("currentConditions", currentConditions);
        jsonNode.set("days", mapper.createArrayNode().add(day));

        mockResponse = jsonNode;
    }

    @SuppressWarnings("unchecked")
    @Test
    void testRateLimitingWorks() {
        // Cache miss to force rate limit check
        when(valueOperations.get(anyString())).thenReturn(null);

        // Mock WebClient to avoid real API call
        RequestHeadersUriSpec uriSpec = mock(RequestHeadersUriSpec.class);
        RequestHeadersSpec headersSpec = mock(RequestHeadersSpec.class);
        ResponseSpec responseSpec = mock(ResponseSpec.class);

        when(weatherWebClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.just(mockResponse));

        // First request should work (consumed 1 token)
        weatherService.getWeather(request);

        // Second request should work (consumed 1 token)
        weatherService.getWeather(request);

        // Third request should fail (no tokens left)
        RateLimitExceededException exception = assertThrows(RateLimitExceededException.class, () -> {
            weatherService.getWeather(request);
        });

        assertThat(exception.getMessage()).contains("Rate limit exceeded");
    }
}