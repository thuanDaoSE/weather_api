package com.thuandao.weather_api.dto;

import java.util.List;

public record WeatherResponse(
        String location,
        String resolvedAddress,
        String description,
        Double currentTemp,
        String conditions,
        Double humidity,
        Double windSpeed,
        List<DayForecast> forecast,
        String source) {
    // Static factory method as alternative constructor
    public static WeatherResponse create(String location, String resolvedAddress, String description,
            Double currentTemp, String conditions, Double humidity, Double windSpeed,
            List<DayForecast> forecast, String source) {
        return new WeatherResponse(location, resolvedAddress, description, currentTemp,
                conditions, humidity, windSpeed, forecast, source);
    }

    public record DayForecast(
            String date,
            Double tempMax,
            Double tempMin,
            String conditions,
            Double precipProbability) {
        // Static factory method as alternative constructor
        public static DayForecast create(String date, Double tempMax, Double tempMin,
                String conditions, Double precipProbability) {
            return new DayForecast(date, tempMax, tempMin, conditions, precipProbability);
        }
    }
}