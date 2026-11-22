package com.c0nnect.nutripick.domain.user.service;

import com.c0nnect.nutripick.domain.user.dto.request.SaveOnboardingRequest;
import com.c0nnect.nutripick.domain.user.dto.response.NameResponse;
import com.c0nnect.nutripick.domain.user.dto.response.OnboardingResponse;
import com.c0nnect.nutripick.domain.user.dto.response.UserResponse;
import com.c0nnect.nutripick.domain.user.entity.User;
import com.c0nnect.nutripick.domain.user.repository.UserRepository;
import com.c0nnect.nutripick.global.error.CustomException;
import com.c0nnect.nutripick.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository users;

    public UserResponse getAccount(long userId) {
        return users.findById(userId).map(UserResponse::from)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
    }

    @Transactional
    public NameResponse changeName(long userId, String name) {
        User user = findForUpdate(userId);
        user.changeName(name);
        return new NameResponse(user.getName());
    }

    public OnboardingResponse getOnboarding(long userId) {
        return users.findWithPreferencesById(userId).map(OnboardingResponse::from)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
    }

    @Transactional
    public OnboardingResponse saveOnboarding(long userId, SaveOnboardingRequest request) {
        User user = findForUpdate(userId);
        user.updateOnboarding(request.name(), request.tutorialCompleted(), request.nutrientPreferences());
        return OnboardingResponse.from(user);
    }

    private User findForUpdate(long userId) {
        return users.findForUpdateById(userId).orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
    }
}
