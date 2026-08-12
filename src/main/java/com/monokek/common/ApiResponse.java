package com.monokek.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response envelope mirroring the {@code {message, status, data}} shape
 * produced by the Laravel app's {@code App\Http\Helpers\Helpers} class, so
 * existing frontend clients keep working against this API unchanged.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(String status, String message, T data, Object errors) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("success", null, data, null);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>("success", message, data, null);
    }

    public static <T> ApiResponse<T> message(String message) {
        return new ApiResponse<>("success", message, null, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>("error", message, null, null);
    }

    public static <T> ApiResponse<T> validationError(String message, Object errors) {
        return new ApiResponse<>("error", message, null, errors);
    }
}
