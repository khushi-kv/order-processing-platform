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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    // Checked when the email is unknown, so that case costs one BCrypt check like a wrong password
    private final String dummyPasswordHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyPasswordHash = passwordEncoder.encode("dummy-password-for-timing");
    }

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
     * Unknown email and wrong password fail the same way and take about the same time,
     * so neither the response nor its timing reveals which emails exist.
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Optional<User> found = userRepository.findByEmail(User.normalizeEmail(request.email()));
        String hash = found.map(User::getPasswordHash).orElse(dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);
        User user = found.filter(u -> passwordMatches).orElseThrow(InvalidCredentialsException::new);
        return LoginResponse.bearer(jwtService.issueToken(user), jwtService.expiresInSeconds());
    }
}
