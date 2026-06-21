package com.artgallery.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Uniform envelope for every REST response (success or error).
 *
 * <p>Shape: {@code { success, status, message, data, errors, timestamp }}.</p>
 *
 * @param <T> the payload type
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        int status,
        String message,
        T data,
        List<String> errors,
        Instant timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, 200, null, data, null, Instant.now());
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, 200, message, data, null, Instant.now());
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return new ApiResponse<>(true, 201, message, data, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(int status, String message, List<String> errors) {
        return new ApiResponse<>(false, status, message, null, errors, Instant.now());
    }

    public static <T> ApiResponse<T> error(int status, String message, T data, List<String> errors) {
        return new ApiResponse<>(false, status, message, data, errors, Instant.now());
    }
}
