package com.orderplatform.user.service;

import com.orderplatform.user.TestUsers;
import com.orderplatform.user.dto.LoginRequest;
import com.orderplatform.user.dto.LoginResponse;
import com.orderplatform.user.dto.RegisterRequest;
import com.orderplatform.user.dto.UserResponse;
import com.orderplatform.user.entity.Role;
import com.orderplatform.user.entity.User;
import com.orderplatform.user.exception.DuplicateResourceException;
import com.orderplatform.user.exception.InvalidCredentialsException;
import com.orderplatform.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerCreatesCustomerWithHashedPasswordAndNormalizedEmail() {
        when(userRepository.existsByEmail("khushi@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret123")).thenReturn("$2a$10$hash");
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        UserResponse response = authService.register(new RegisterRequest("  Khushi ", " KHUSHI@Test.com ", "Secret123"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Khushi");
        assertThat(saved.getValue().getEmail()).isEqualTo("khushi@test.com");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("$2a$10$hash");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(response.role()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    void registerRejectsAnEmailThatIsAlreadyTaken() {
        when(userRepository.existsByEmail("khushi@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("Khushi", "Khushi@test.com", "Secret123")))
                .isInstanceOf(DuplicateResourceException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginReturnsBearerTokenForCorrectPassword() {
        User user = TestUsers.customer(1L);
        when(userRepository.findByEmail("khushi@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Secret123", user.getPasswordHash())).thenReturn(true);
        when(jwtService.issueToken(user)).thenReturn("signed.jwt.token");
        when(jwtService.expiresInSeconds()).thenReturn(3600L);

        LoginResponse response = authService.login(new LoginRequest("KHUSHI@test.com", "Secret123"));

        assertThat(response.accessToken()).isEqualTo("signed.jwt.token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = TestUsers.customer(1L);
        when(userRepository.findByEmail("khushi@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Wrong123", user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("khushi@test.com", "Wrong123")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
        verify(jwtService, never()).issueToken(any());
    }

    @Test
    void loginRejectsUnknownEmailWithTheSameMessage() {
        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@test.com", "Secret123")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }
}
