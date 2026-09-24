package com.orderplatform.auth.service;

import com.orderplatform.auth.dto.RegisterRequest;
import com.orderplatform.auth.entity.AppUser;
import com.orderplatform.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public String registerUser(RegisterRequest request) {
        // 1. Check if user already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email is already taken!");
        }

        // 2. Hash the password securely
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // 3. Create and save the new user
        AppUser newUser = AppUser.builder()
                .email(request.getEmail())
                .password(hashedPassword)
                .role("ROLE_USER")
                .build();

        userRepository.save(newUser);

        return "User registered successfully!";
    }
}
