package com.thuandao.weather_api.service;

import com.thuandao.weather_api.dto.WeatherRequest;
import com.thuandao.weather_api.dto.WeatherResponse;

public interface WeatherService {

    /**
     * Gets weather data for the specified location
     * 
     * @param request The weather request containing location and optional date
     * @return The weather response with current conditions and forecast
     */
    WeatherResponse getWeather(WeatherRequest request);
}