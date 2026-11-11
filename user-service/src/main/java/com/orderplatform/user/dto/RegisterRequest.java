package com.orderplatform.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** No role field: a client can never choose its own role. */
public record RegisterRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email,

        // BCrypt only uses the first 72 bytes, so longer passwords are rejected rather than silently cut
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters long")
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[A-Z]).*$",
                message = "Password must contain at least one number and one uppercase letter")
        String password
) {
}
