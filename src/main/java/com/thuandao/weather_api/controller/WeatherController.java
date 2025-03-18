package com.thuandao.weather_api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thuandao.weather_api.dto.ApiResponse;
import com.thuandao.weather_api.dto.WeatherRequest;
import com.thuandao.weather_api.dto.WeatherResponse;
import com.thuandao.weather_api.service.WeatherService;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    @Autowired
    private WeatherService weatherService;

    @GetMapping("/{location}")
    public ResponseEntity<ApiResponse<WeatherResponse>> getWeather(
            @PathVariable String location,
            @RequestParam(required = false) String date) {

        try {
            WeatherRequest request = new WeatherRequest(location, date);
            WeatherResponse response = weatherService.getWeather(request);
            return ResponseEntity.ok(ApiResponse.success("Weather data retrieved successfully", response));
        } catch (Exception e) {
            throw e;
        }
    }
}