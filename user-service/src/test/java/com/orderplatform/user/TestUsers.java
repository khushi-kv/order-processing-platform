package com.orderplatform.user;

import com.orderplatform.user.entity.Role;
import com.orderplatform.user.entity.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

/** Builds User entities as if they had been loaded from the database (id and createdAt set). */
public final class TestUsers {

    private TestUsers() {
    }

    public static User user(long id, String email, Role role) {
        User user = new User("Khushi", email, "$2a$10$hash", role);
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "createdAt", Instant.parse("2026-10-26T10:00:00Z"));
        return user;
    }

    public static User customer(long id) {
        return user(id, "khushi@test.com", Role.CUSTOMER);
    }
}
