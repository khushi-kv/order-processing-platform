package com.orderplatform.user.service;

import com.orderplatform.user.dto.UpdateProfileRequest;
import com.orderplatform.user.dto.UserResponse;
import com.orderplatform.user.entity.User;
import com.orderplatform.user.exception.ResourceNotFoundException;
import com.orderplatform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return UserResponse.from(findUser(userId));
    }

    /** No save() call: the entity is managed, so the change is written when the transaction commits. */
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        user.setName(request.name().trim());
        return UserResponse.from(user);
    }

    // A valid token can outlive its user (e.g. the account was deleted), so this can still be 404
    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
}
