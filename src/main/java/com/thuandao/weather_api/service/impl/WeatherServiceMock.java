package com.thuandao.weather_api.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.thuandao.weather_api.dto.WeatherRequest;
import com.thuandao.weather_api.dto.WeatherResponse;
import com.thuandao.weather_api.dto.WeatherResponse.DayForecast;
import com.thuandao.weather_api.service.WeatherService;

@Service
@Primary
public class WeatherServiceMock implements WeatherService {
    private static final Logger logger = LoggerFactory.getLogger(WeatherServiceMock.class);

    @Override
    public WeatherResponse getWeather(WeatherRequest request) {
        logger.info("Mock weather service providing data for location: {}", request.location());

        // Create mock forecast data
        List<DayForecast> forecast = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            // Create forecast with direct constructor since we're using records
            forecast.add(new DayForecast(
                    "2024-03-" + (20 + i),
                    25.0 + (i * 1.5),
                    15.0 + i,
                    i % 2 == 0 ? "Partly Cloudy" : "Sunny",
                    i * 10.0));
        }

        // Create response with direct constructor since we're using records
        return new WeatherResponse(
                request.location(),
                request.location() + ", Mock Country",
                "Mock weather data for demonstration purposes",
                22.5,
                "Partly Cloudy",
                65.0,
                12.5,
                forecast,
                "Mock Data");
    }
}