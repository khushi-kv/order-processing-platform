package com.orderplatform.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** First admin account, from ADMIN_EMAIL and ADMIN_PASSWORD; seeding is skipped when either is blank. */
@ConfigurationProperties("app.admin")
public record AdminProperties(String name, String email, String password) {

    public boolean isConfigured() {
        return email != null && !email.isBlank() && password != null && !password.isBlank();
    }
}
