package com.orderplatform.user.dto;

import jakarta.validation.constraints.NotBlank;

/** No password rules here: login only checks the password, rules apply at registration. */
public record LoginRequest(
        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {
}
