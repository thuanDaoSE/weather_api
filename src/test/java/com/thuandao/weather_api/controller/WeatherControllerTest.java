package com.thuandao.weather_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.thuandao.weather_api.dto.WeatherRequest;
import com.thuandao.weather_api.dto.WeatherResponse;
import com.thuandao.weather_api.dto.WeatherResponse.DayForecast;
import com.thuandao.weather_api.exception.RateLimitExceededException;
import com.thuandao.weather_api.service.WeatherService;

@WebMvcTest(WeatherController.class)
public class WeatherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WeatherService weatherService;

    private WeatherResponse mockWeatherResponse;

    @BeforeEach
    void setUp() {
        List<DayForecast> forecast = new ArrayList<>();
        forecast.add(DayForecast.builder()
                .date("2023-06-10")
                .tempMax(28.5)
                .tempMin(18.2)
                .conditions("Clear")
                .precipProbability(0.0)
                .build());

        mockWeatherResponse = WeatherResponse.builder()
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
    }

    @Test
    void testGetWeatherSuccess() throws Exception {
        when(weatherService.getWeather(any(WeatherRequest.class))).thenReturn(mockWeatherResponse);

        mockMvc.perform(get("/api/weather/New York"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.location").value("New York"))
                .andExpect(jsonPath("$.data.currentTemp").value(22.5))
                .andExpect(jsonPath("$.data.forecast[0].date").value("2023-06-10"));
    }

    @Test
    void testGetWeatherWithDate() throws Exception {
        when(weatherService.getWeather(any(WeatherRequest.class))).thenReturn(mockWeatherResponse);

        mockMvc.perform(get("/api/weather/New York?date=2023-06-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.location").value("New York"));
    }

    @Test
    void testGetWeatherRateLimitExceeded() throws Exception {
        when(weatherService.getWeather(any(WeatherRequest.class)))
                .thenThrow(new RateLimitExceededException("Rate limit exceeded"));

        mockMvc.perform(get("/api/weather/New York"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.result").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Rate limit exceeded"));
    }
}