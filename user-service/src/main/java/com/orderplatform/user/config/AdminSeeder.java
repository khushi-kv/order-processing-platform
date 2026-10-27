package com.orderplatform.user.config;

import com.orderplatform.user.entity.Role;
import com.orderplatform.user.entity.User;
import com.orderplatform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first ADMIN at startup, the only way an admin can exist (registration always gives CUSTOMER).
 * Safe to run on every start: an existing account with that email is left untouched.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final AdminProperties admin;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!admin.isConfigured()) {
            log.info("ADMIN_EMAIL or ADMIN_PASSWORD not set; skipping admin seed");
            return;
        }
        if (admin.password().length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("ADMIN_PASSWORD must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        String email = User.normalizeEmail(admin.email());
        if (userRepository.existsByEmail(email)) {
            log.info("Admin seed skipped; {} already exists", email);
            return;
        }
        userRepository.save(new User(admin.name(), email, passwordEncoder.encode(admin.password()), Role.ADMIN));
        log.info("Seeded admin account {}", email);
    }
}
