package com.orderplatform.user.service;

import com.orderplatform.user.TestUsers;
import com.orderplatform.user.dto.UpdateProfileRequest;
import com.orderplatform.user.dto.UserResponse;
import com.orderplatform.user.entity.User;
import com.orderplatform.user.exception.ResourceNotFoundException;
import com.orderplatform.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getProfileReturnsTheUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(TestUsers.customer(1L)));

        UserResponse response = userService.getProfile(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("khushi@test.com");
    }

    @Test
    void getProfileFailsWhenTheUserNoLongerExists() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found: 99");
    }

    @Test
    void updateProfileChangesTheTrimmedName() {
        User user = TestUsers.customer(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.updateProfile(1L, new UpdateProfileRequest("  Khushi Verma  "));

        assertThat(user.getName()).isEqualTo("Khushi Verma");
        assertThat(response.name()).isEqualTo("Khushi Verma");
    }
}
