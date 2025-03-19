package com.thuandao.weather_api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;

@Configuration
public class AppConfig {
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    @Value("${weather.api.url}")
    private String weatherApiUrl;

    @Value("${weather.api.key}")
    private String weatherApiKey;

    @PostConstruct
    public void validateProperties() {
        logger.info("Validating application properties...");
        if (weatherApiUrl == null || weatherApiUrl.isBlank()) {
            logger.warn("Weather API URL is not configured properly");
        }
        logger.info("Application properties validation completed successfully");
    }

    @Bean
    public WebClient weatherWebClient() {
        return WebClient.builder().build();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public String weatherApiUrl() {
        return weatherApiUrl;
    }

    @Bean
    public String weatherApiKey() {
        return weatherApiKey;
    }
}