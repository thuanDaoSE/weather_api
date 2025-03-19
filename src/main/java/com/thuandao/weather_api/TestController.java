package com.thuandao.weather_api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.HashMap;
import java.util.Map;

import com.thuandao.weather_api.dto.WeatherRequest;
import com.thuandao.weather_api.dto.WeatherResponse;
import com.thuandao.weather_api.service.WeatherService;
import com.thuandao.weather_api.dto.ApiResponse;

@RestController
@RequestMapping("/test")
public class TestController {

    private final WeatherService weatherService;

    public TestController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/ping")
    public Map<String, String> ping() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "pong");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return response;
    }

    @GetMapping("/weather")
    public ApiResponse<WeatherResponse> getWeather(@RequestParam String location) {
        try {
            WeatherRequest request = new WeatherRequest(location, null);
            WeatherResponse response = weatherService.getWeather(request);
            return new ApiResponse<>("SUCCESS", "Weather data retrieved successfully", response);
        } catch (Exception e) {
            return new ApiResponse<>("ERROR", e.getMessage(), null);
        }
    }
}