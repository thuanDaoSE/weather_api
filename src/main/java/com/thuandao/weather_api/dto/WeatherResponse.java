package com.thuandao.weather_api.dto;

import java.util.List;

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
    
    public WeatherResponse() {}
    
    public WeatherResponse(String location, String resolvedAddress, String description, Double currentTemp,
                         String conditions, Double humidity, Double windSpeed, List<DayForecast> forecast,
                         String source) {
        this.location = location;
        this.resolvedAddress = resolvedAddress;
        this.description = description;
        this.currentTemp = currentTemp;
        this.conditions = conditions;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.forecast = forecast;
        this.source = source;
    }

    // Static factory method as alternative to builder
    public static WeatherResponse create(String location, String resolvedAddress, String description,
            Double currentTemp, String conditions, Double humidity, Double windSpeed,
            List<DayForecast> forecast, String source) {
        return new WeatherResponse(location, resolvedAddress, description, currentTemp, 
                conditions, humidity, windSpeed, forecast, source);
    }
    
    // Getters and setters
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public String getResolvedAddress() { return resolvedAddress; }
    public void setResolvedAddress(String resolvedAddress) { this.resolvedAddress = resolvedAddress; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Double getCurrentTemp() { return currentTemp; }
    public void setCurrentTemp(Double currentTemp) { this.currentTemp = currentTemp; }
    
    public String getConditions() { return conditions; }
    public void setConditions(String conditions) { this.conditions = conditions; }
    
    public Double getHumidity() { return humidity; }
    public void setHumidity(Double humidity) { this.humidity = humidity; }
    
    public Double getWindSpeed() { return windSpeed; }
    public void setWindSpeed(Double windSpeed) { this.windSpeed = windSpeed; }
    
    public List<DayForecast> getForecast() { return forecast; }
    public void setForecast(List<DayForecast> forecast) { this.forecast = forecast; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public static class DayForecast {
        private String date;
        private Double tempMax;
        private Double tempMin;
        private String conditions;
        private Double precipProbability;
        
        public DayForecast() {}
        
        public DayForecast(String date, Double tempMax, Double tempMin, String conditions, Double precipProbability) {
            this.date = date;
            this.tempMax = tempMax;
            this.tempMin = tempMin;
            this.conditions = conditions;
            this.precipProbability = precipProbability;
        }

        // Static factory method as alternative to builder
        public static DayForecast create(String date, Double tempMax, Double tempMin, String conditions, Double precipProbability) {
            return new DayForecast(date, tempMax, tempMin, conditions, precipProbability);
        }
        
        // Getters and setters
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        
        public Double getTempMax() { return tempMax; }
        public void setTempMax(Double tempMax) { this.tempMax = tempMax; }
        
        public Double getTempMin() { return tempMin; }
        public void setTempMin(Double tempMin) { this.tempMin = tempMin; }
        
        public String getConditions() { return conditions; }
        public void setConditions(String conditions) { this.conditions = conditions; }
        
        public Double getPrecipProbability() { return precipProbability; }
        public void setPrecipProbability(Double precipProbability) { this.precipProbability = precipProbability; }
    }
}