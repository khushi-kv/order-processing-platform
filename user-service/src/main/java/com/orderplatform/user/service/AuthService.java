package com.orderplatform.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orderplatform.user.dto.RegisterRequest;
import com.orderplatform.user.dto.UserResponse;
import com.orderplatform.user.entity.Role;
import com.orderplatform.user.entity.User;
import com.orderplatform.user.exception.DuplicateResourceException;
import com.orderplatform.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registration always creates a CUSTOMER; admins only come from the startup
     * seed.
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = User.normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email is already registered: " + email);
        }
        User user = new User(request.name().trim(), email, passwordEncoder.encode(request.password()), Role.CUSTOMER);
        return UserResponse.from(userRepository.save(user));
    }
}
