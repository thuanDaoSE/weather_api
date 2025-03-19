package com.thuandao.weather_api.service;

import com.thuandao.weather_api.dto.AuthRequest;
import com.thuandao.weather_api.dto.AuthResponse;
import com.thuandao.weather_api.dto.RegisterRequest;

public interface UserService {

    AuthResponse register(RegisterRequest request);

    AuthResponse authenticate(AuthRequest request);
}