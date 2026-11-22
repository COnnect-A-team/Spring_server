package com.c0nnect.nutripick.domain.user.dto.response;

import com.c0nnect.nutripick.domain.user.entity.User;

public record UserResponse(Long id, String email, String name, boolean emailVerified, boolean tutorialCompleted) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(),
                user.isEmailVerified(), user.isTutorialCompleted());
    }
}
