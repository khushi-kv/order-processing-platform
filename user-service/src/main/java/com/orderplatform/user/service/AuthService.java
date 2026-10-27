package com.orderplatform.user.service;

import com.orderplatform.user.dto.LoginRequest;
import com.orderplatform.user.dto.LoginResponse;
import com.orderplatform.user.dto.RegisterRequest;
import com.orderplatform.user.dto.UserResponse;
import com.orderplatform.user.entity.Role;
import com.orderplatform.user.entity.User;
import com.orderplatform.user.exception.DuplicateResourceException;
import com.orderplatform.user.exception.InvalidCredentialsException;
import com.orderplatform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

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

    /**
     * Unknown email and wrong password fail the same way, so the response never
     * reveals which emails exist.
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(User.normalizeEmail(request.email()))
                .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        return LoginResponse.bearer(jwtService.issueToken(user), jwtService.expiresInSeconds());
    }
}
