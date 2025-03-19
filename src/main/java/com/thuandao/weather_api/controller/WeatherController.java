package com.thuandao.weather_api.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thuandao.weather_api.dto.ApiResponse;
import com.thuandao.weather_api.dto.WeatherRequest;
import com.thuandao.weather_api.dto.WeatherResponse;
import com.thuandao.weather_api.model.User;
import com.thuandao.weather_api.service.WeatherService;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private static final Logger logger = LoggerFactory.getLogger(WeatherController.class);

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/{location}")
    public ResponseEntity<ApiResponse<WeatherResponse>> getWeather(
            @PathVariable String location,
            @RequestParam(required = false) String date) {

        try {
            // Get authenticated user if available
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = "anonymous";

            if (authentication != null && authentication.isAuthenticated() &&
                    authentication.getPrincipal() != null &&
                    !"anonymousUser".equals(authentication.getPrincipal())) {
                if (authentication.getPrincipal() instanceof User) {
                    User user = (User) authentication.getPrincipal();
                    username = user.getUsername();
                } else if (authentication.getPrincipal() instanceof String) {
                    username = (String) authentication.getPrincipal();
                } else {
                    logger.debug("Principal is of type: {}", authentication.getPrincipal().getClass().getName());
                }
            }

            logger.info("Weather request received for location: {} by user: {}", location, username);

            WeatherRequest request = new WeatherRequest(location, date);
            WeatherResponse response = weatherService.getWeather(request);

            return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Weather data retrieved successfully", response));
        } catch (Exception e) {
            logger.error("Error fetching weather data: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>("ERROR", e.getMessage(), null));
        }
    }
}