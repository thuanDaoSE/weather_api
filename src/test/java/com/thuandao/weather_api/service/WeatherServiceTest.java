package com.thuandao.weather_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.thuandao.weather_api.config.RateLimiterConfig;
import com.thuandao.weather_api.dto.WeatherRequest;
import com.thuandao.weather_api.dto.WeatherResponse;
import com.thuandao.weather_api.dto.WeatherResponse.DayForecast;
import com.thuandao.weather_api.exception.RateLimitExceededException;
import com.thuandao.weather_api.service.impl.WeatherServiceImpl;
import com.thuandao.weather_api.exception.WeatherServiceException;

import io.github.bucket4j.Bucket;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
public class WeatherServiceTest {

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
    private Bucket mockBucket;
    private JsonNode mockResponse;
    private WeatherResponse cachedResponse;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() throws Exception {
        request = new WeatherRequest("New York", null);
        mockBucket = mock(Bucket.class);

        // Setup Redis mock
        lenient().when(weatherRedisTemplate.opsForValue()).thenReturn(valueOperations);

        // Set up rate limiter - use lenient for all setup to avoid unnecessary stubbing
        // issues
        lenient().when(rateLimiterConfig.createNewBucket()).thenReturn(mockBucket);
        lenient().when(rateLimitBuckets.computeIfAbsent(anyString(), any())).thenReturn(mockBucket);

        // Set up mock response
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

        // Set service properties
        ReflectionTestUtils.setField(weatherService, "apiUrl", "https://api.visualcrossing.com/weather");
        ReflectionTestUtils.setField(weatherService, "apiKey", "test_key");
        ReflectionTestUtils.setField(weatherService, "cacheTtl", 43200L);

        // Create cached response
        List<DayForecast> forecast = new ArrayList<>();
        DayForecast dayForecast = new DayForecast();
        dayForecast.setDate("2023-06-10");
        dayForecast.setTempMax(28.5);
        dayForecast.setTempMin(18.2);
        dayForecast.setConditions("Clear");
        dayForecast.setPrecipProbability(0.0);
        forecast.add(dayForecast);

        cachedResponse = new WeatherResponse();
        cachedResponse.setLocation("New York");
        cachedResponse.setResolvedAddress("New York, NY, USA");
        cachedResponse.setDescription("Clear conditions throughout the day.");
        cachedResponse.setCurrentTemp(22.5);
        cachedResponse.setConditions("Clear");
        cachedResponse.setHumidity(65.2);
        cachedResponse.setWindSpeed(5.4);
        cachedResponse.setSource("Visual Crossing");
        cachedResponse.setForecast(forecast);
    }

    @Test
    void testGetWeatherFromCache() {
        // Setup
        when(mockBucket.tryConsume(1)).thenReturn(true);
        when(valueOperations.get(anyString())).thenReturn(cachedResponse);

        // Execute
        WeatherResponse result = weatherService.getWeather(request);

        // Verify
        assertNotNull(result);
        assertEquals("New York", result.getLocation());
        verify(weatherRedisTemplate.opsForValue(), times(1)).get(anyString());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testGetWeatherFromApi() {
        // Setup
        when(mockBucket.tryConsume(1)).thenReturn(true);
        when(valueOperations.get(anyString())).thenReturn(null);

        // Setup WebClient mocking
        RequestHeadersUriSpec uriSpec = mock(RequestHeadersUriSpec.class);
        RequestHeadersSpec headersSpec = mock(RequestHeadersSpec.class);
        ResponseSpec responseSpec = mock(ResponseSpec.class);

        when(weatherWebClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.just(mockResponse));

        // Execute
        WeatherResponse result = weatherService.getWeather(request);

        // Verify
        assertNotNull(result);
        assertEquals("New York", result.getLocation());
        verify(weatherRedisTemplate.opsForValue(), times(1)).set(anyString(), any(WeatherResponse.class),
                any(Duration.class));
    }

    @Test
    void testRateLimitExceeded() {
        // Setup
        when(mockBucket.tryConsume(1)).thenReturn(false);

        // No need for this as it's already set up in setUp with lenient()
        // when(rateLimitBuckets.computeIfAbsent(anyString(),
        // any())).thenReturn(mockBucket);

        // Execute & verify
        assertThrows(RateLimitExceededException.class, () -> {
            weatherService.getWeather(request);
        });
    }

    @Test
    void testApiErrorHandling() {
        // Setup
        when(mockBucket.tryConsume(1)).thenReturn(true);
        when(valueOperations.get(anyString())).thenReturn(null);

        // Setup WebClient mocking for error
        RequestHeadersUriSpec uriSpec = mock(RequestHeadersUriSpec.class);
        RequestHeadersSpec headersSpec = mock(RequestHeadersSpec.class);
        ResponseSpec responseSpec = mock(ResponseSpec.class);

        when(weatherWebClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(Class.class))).thenThrow(
                new WebClientResponseException(500, "Internal Server Error", null, null, null));

        // Execute & verify
        assertThrows(WeatherServiceException.class, () -> {
            weatherService.getWeather(request);
        });
    }

    @Test
    void testInvalidResponseHandling() {
        // Setup
        when(mockBucket.tryConsume(1)).thenReturn(true);
        when(valueOperations.get(anyString())).thenReturn(null);

        // Setup WebClient mocking with invalid response
        RequestHeadersUriSpec uriSpec = mock(RequestHeadersUriSpec.class);
        RequestHeadersSpec headersSpec = mock(RequestHeadersSpec.class);
        ResponseSpec responseSpec = mock(ResponseSpec.class);

        when(weatherWebClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        // Create empty JSON node without required fields
        ObjectMapper mapper = new ObjectMapper();
        JsonNode emptyNode = mapper.createObjectNode();
        when(responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.just(emptyNode));

        // Execute & verify
        assertThrows(WeatherServiceException.class, () -> {
            weatherService.getWeather(request);
        });
    }
}