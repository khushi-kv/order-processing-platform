package com.orderplatform.user.dto;
import java.time.Instant;

import com.orderplatform.user.entity.Role;
import com.orderplatform.user.entity.User;

/** What the API returns about a user; the password hash never leaves the service. */
public record UserResponse(Long id, String name, String email, Role role, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
