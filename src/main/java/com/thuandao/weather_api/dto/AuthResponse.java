package com.thuandao.weather_api.dto;

public record AuthResponse(
        String token,
        String username,
        String role) {
}