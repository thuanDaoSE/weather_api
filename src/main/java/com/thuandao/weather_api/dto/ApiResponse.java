package com.thuandao.weather_api.dto;

public class ApiResponse<T> {
    private String result; // SUCCESS or ERROR
    private String message; // success or error message
    private T data; // return object from service class, if successful
    
    public ApiResponse() {}
    
    public ApiResponse(String result, String message, T data) {
        this.result = result;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<T>("SUCCESS", message, data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<T>("ERROR", message, null);
    }
    
    // Getters and setters
    public String getResult() {
        return result;
    }
    
    public void setResult(String result) {
        this.result = result;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
}