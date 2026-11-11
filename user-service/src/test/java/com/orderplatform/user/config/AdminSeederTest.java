package com.orderplatform.user.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.orderplatform.user.entity.Role;
import com.orderplatform.user.entity.User;
import com.orderplatform.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminSeederTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminSeeder seeder(String email, String password) {
        return new AdminSeeder(new AdminProperties("Admin", email, password), userRepository, passwordEncoder);
    }

    @Test
    void createsTheAdminWhenTheEmailIsNew() {
        when(userRepository.existsByEmail("admin@test.com")).thenReturn(false);
        when(passwordEncoder.encode("AdminPass1")).thenReturn("$2a$10$hash");

        seeder(" Admin@Test.com ", "AdminPass1").run(null);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("admin@test.com");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.ADMIN);
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("$2a$10$hash");
    }

    @Test
    void leavesAnExistingAccountUntouched() {
        when(userRepository.existsByEmail("admin@test.com")).thenReturn(true);

        seeder("admin@test.com", "AdminPass1").run(null);

        verify(userRepository, never()).save(any());
    }

    @Test
    void skipsWhenTheVariablesAreNotSet() {
        seeder("", "").run(null);
        seeder(null, null).run(null);

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void refusesAShortPassword() {
        AdminSeeder seeder = seeder("admin@test.com", "short");

        assertThatThrownBy(() -> seeder.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 8 characters");
        verifyNoInteractions(userRepository);
    }
}
