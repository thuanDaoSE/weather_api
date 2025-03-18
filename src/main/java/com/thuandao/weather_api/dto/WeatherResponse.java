package com.thuandao.weather_api.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WeatherResponse {
    private String location;
    private String resolvedAddress;
    private String description;
    private Double currentTemp;
    private String conditions;
    private Double humidity;
    private Double windSpeed;
    private List<DayForecast> forecast;
    private String source;

    @Data
    @Builder
    public static class DayForecast {
        private String date;
        private Double tempMax;
        private Double tempMin;
        private String conditions;
        private Double precipProbability;
    }
}