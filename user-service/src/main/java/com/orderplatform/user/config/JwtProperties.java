package com.orderplatform.user.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/** Settings under {@code app.jwt}; checked at startup so a weak or missing secret stops the service. */
@Validated
@ConfigurationProperties("app.jwt")
public record JwtProperties(
        // HS256 needs a key of at least 256 bits (32 bytes)
        @NotBlank @Size(min = 32, message = "must be at least 32 characters") String secret,
        @NotBlank String issuer,
        @NotNull Duration expiration
) {
}
