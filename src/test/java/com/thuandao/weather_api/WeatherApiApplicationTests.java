package com.thuandao.weather_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.thuandao.weather_api.controller.WeatherController;
import com.thuandao.weather_api.dto.WeatherResponse;
import com.thuandao.weather_api.dto.WeatherResponse.DayForecast;
import com.thuandao.weather_api.service.WeatherService;

import reactor.core.publisher.Mono;

@SpringBootTest
@AutoConfigureMockMvc
class WeatherApiApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private WeatherController weatherController;

	@MockBean
	private WebClient weatherWebClient;

	@MockBean
	private RedisTemplate<String, WeatherResponse> weatherRedisTemplate;

	@SuppressWarnings("unchecked")
	@Test
	void contextLoads() {
		assertThat(weatherController).isNotNull();
	}

	@SuppressWarnings("unchecked")
	@Test
	void testIntegrationWithMockedApi() throws Exception {
		// Mock Redis
		ValueOperations<String, WeatherResponse> valueOps = Mockito.mock(ValueOperations.class);
		when(weatherRedisTemplate.opsForValue()).thenReturn(valueOps);
		when(valueOps.get(anyString())).thenReturn(null); // Force API call

		// Setup WebClient mock
		RequestHeadersUriSpec requestHeadersUriSpec = Mockito.mock(RequestHeadersUriSpec.class);
		RequestHeadersSpec requestHeadersSpec = Mockito.mock(RequestHeadersSpec.class);
		ResponseSpec responseSpec = Mockito.mock(ResponseSpec.class);

		when(weatherWebClient.get()).thenReturn(requestHeadersUriSpec);
		when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

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

		when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(jsonNode));

		// Test
		mockMvc.perform(get("/api/weather/New York"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result").value("SUCCESS"))
				.andExpect(jsonPath("$.data.location").value("New York"))
				.andExpect(jsonPath("$.data.currentTemp").value(22.5))
				.andExpect(jsonPath("$.data.conditions").value("Clear"));
	}

	@SuppressWarnings("unchecked")
	@Test
	void testCachingWorks() throws Exception {
		// Create cached weather response
		List<DayForecast> forecast = new ArrayList<>();
		forecast.add(DayForecast.builder()
				.date("2023-06-10")
				.tempMax(28.5)
				.tempMin(18.2)
				.conditions("Clear")
				.precipProbability(0.0)
				.build());

		WeatherResponse cachedResponse = WeatherResponse.builder()
				.location("New York")
				.resolvedAddress("New York, NY, USA")
				.description("Clear conditions throughout the day.")
				.currentTemp(22.5)
				.conditions("Clear")
				.humidity(65.2)
				.windSpeed(5.4)
				.forecast(forecast)
				.source("Visual Crossing")
				.build();

		// Mock Redis to return cached value
		ValueOperations<String, WeatherResponse> valueOps = Mockito.mock(ValueOperations.class);
		when(weatherRedisTemplate.opsForValue()).thenReturn(valueOps);
		when(valueOps.get(anyString())).thenReturn(cachedResponse); // Return cached response

		// Test
		mockMvc.perform(get("/api/weather/New York"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result").value("SUCCESS"))
				.andExpect(jsonPath("$.data.location").value("New York"))
				.andExpect(jsonPath("$.data.currentTemp").value(22.5));

		// Verify WebClient was never called because cache was used
		verify(weatherWebClient, never()).get();
	}
}
