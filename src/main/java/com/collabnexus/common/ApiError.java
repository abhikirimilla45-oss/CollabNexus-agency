package com.collabnexus.common;

import java.time.Instant;

public record ApiError(
        boolean success,
        String error,
        String message,
        String path,
        Instant timestamp
) {

    public static ApiError of(String error, String message, String path) {
        return new ApiError(false, error, message, path, Instant.now());
    }
}
