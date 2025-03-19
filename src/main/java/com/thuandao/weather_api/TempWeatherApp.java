package com.thuandao.weather_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ComponentScan(basePackages = "com.thuandao.weather_api", excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*Test.*"))
public class TempWeatherApp {
    public static void main(String[] args) {
        try {
            System.out.println("Starting application...");
            ConfigurableApplicationContext ctx = SpringApplication.run(TempWeatherApp.class, args);
            System.out.println("Application started successfully! Available beans:");

            // Print all bean names for debugging
            String[] beanNames = ctx.getBeanDefinitionNames();
            for (String beanName : beanNames) {
                System.out.println(beanName);
            }

            System.out.println("Total beans: " + beanNames.length);
            System.out.println("Application is running on port: " +
                    ctx.getEnvironment().getProperty("server.port"));

        } catch (Exception e) {
            System.err.println("Failed to start application: " + e.getMessage());
            e.printStackTrace();
        }
    }
}