package com.epsilon.dto;

import lombok.NoArgsConstructor;
import lombok.Data;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/*
 * Standard API response wrapper.
 * Provides consistent response structure across all endpoints.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(T data){
        return new ApiResponse<>(true, "Success", data, LocalDateTime.now());
    }
    
    public static <T> ApiResponse<T> success(String message, T data){
        return new ApiResponse<>(true, message, data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> error(String message){
        return new ApiResponse<>(false, message, null, LocalDateTime.now());
    }
}
