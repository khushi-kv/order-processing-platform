package com.orderplatform.user.service;

import com.orderplatform.user.dto.LoginRequest;
import com.orderplatform.user.dto.LoginResponse;
import com.orderplatform.user.dto.RefreshRequest;
import com.orderplatform.user.dto.RegisterRequest;
import com.orderplatform.user.dto.UserResponse;
import com.orderplatform.user.entity.Role;
import com.orderplatform.user.entity.User;
import com.orderplatform.user.exception.DuplicateResourceException;
import com.orderplatform.user.exception.InvalidCredentialsException;
import com.orderplatform.user.exception.InvalidRefreshTokenException;
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
    private final RefreshTokenService refreshTokenService;
    // Checked when the email is unknown, so that case costs one BCrypt check like a wrong password
    private final String dummyPasswordHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
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
    @Transactional
    public LoginResponse login(LoginRequest request) {
        Optional<User> found = userRepository.findByEmail(User.normalizeEmail(request.email()));
        String hash = found.map(User::getPasswordHash).orElse(dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);
        User user = found.filter(u -> passwordMatches).orElseThrow(InvalidCredentialsException::new);
        return issueTokens(user);
    }

    /** Swaps a refresh token for a new access + refresh pair; the old refresh token stops working. */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public LoginResponse refresh(RefreshRequest request) {
        return issueTokens(refreshTokenService.rotate(request.refreshToken()));
    }

    private LoginResponse issueTokens(User user) {
        return LoginResponse.bearer(jwtService.issueToken(user), refreshTokenService.issue(user),
                jwtService.expiresInSeconds());
    }
}
