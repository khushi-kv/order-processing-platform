package com.orderplatform.user.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/** Settings under {@code app.refresh-token}: how long a login session lasts without being used. */
@Validated
@ConfigurationProperties("app.refresh-token")
public record RefreshTokenProperties(@NotNull Duration expiration) {
}
