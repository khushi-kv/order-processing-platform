package com.orderplatform.user.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.time.LocalDateTime;
import java.util.List;

/** The one JSON error shape every service returns (same as the inventory API). */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        List<String> details
) {

    public static ErrorResponse of(HttpStatusCode status, String message, List<String> details) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        String error = resolved != null ? resolved.getReasonPhrase() : "Error";
        return new ErrorResponse(LocalDateTime.now(), status.value(), error, message, details);
    }

    public static ErrorResponse of(HttpStatusCode status, String message) {
        return of(status, message, List.of());
    }
}
