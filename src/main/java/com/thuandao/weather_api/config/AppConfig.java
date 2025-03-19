package com.thuandao.weather_api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;

@Configuration
public class AppConfig {
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    @Value("${weather.api.url}")
    private String apiUrl;

    @Value("${weather.api.key}")
    private String apiKey;

    @Bean
    public WebClient weatherWebClient() {
        return WebClient.builder()
                .build();
    }

    @PostConstruct
    public void validateProperties() {
        logger.info("Validating application properties...");

        if (!StringUtils.hasText(apiUrl)) {
            throw new IllegalStateException("Required property 'weather.api.url' is missing or empty");
        }

        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("Required property 'weather.api.key' is missing or empty");
        }

        logger.info("Application properties validation completed successfully");
    }
}