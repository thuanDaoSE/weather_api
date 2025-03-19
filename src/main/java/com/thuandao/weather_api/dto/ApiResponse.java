package com.thuandao.weather_api.dto;

public record ApiResponse<T>(
        String result,
        String message,
        T data) {
}