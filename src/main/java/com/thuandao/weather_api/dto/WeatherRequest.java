package com.thuandao.weather_api.dto;

import java.util.Objects;

public record WeatherRequest(String location, String date) {
    public WeatherRequest {
        Objects.requireNonNull(location, "Location cannot be null");
        if (location.trim().isEmpty()) {
            throw new IllegalArgumentException("Location cannot be empty");
        }
    }
}