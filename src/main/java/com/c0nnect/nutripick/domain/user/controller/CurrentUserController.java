package com.c0nnect.nutripick.domain.user.controller;

import com.c0nnect.nutripick.domain.user.dto.request.ChangeNameRequest;
import com.c0nnect.nutripick.domain.user.dto.request.SaveOnboardingRequest;
import com.c0nnect.nutripick.domain.user.dto.response.NameResponse;
import com.c0nnect.nutripick.domain.user.dto.response.OnboardingResponse;
import com.c0nnect.nutripick.domain.user.dto.response.UserResponse;
import com.c0nnect.nutripick.domain.user.service.UserService;
import com.c0nnect.nutripick.global.security.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class CurrentUserController {
    private final UserService users;

    @GetMapping
    public UserResponse me(@CurrentUserId Long userId) {
        return users.getAccount(userId);
    }

    @PatchMapping("/name")
    public NameResponse changeName(@CurrentUserId Long userId, @Valid @RequestBody ChangeNameRequest request) {
        return users.changeName(userId, request.name());
    }

    @GetMapping("/onboarding")
    public OnboardingResponse getOnboarding(@CurrentUserId Long userId) {
        return users.getOnboarding(userId);
    }

    @PutMapping("/onboarding")
    public OnboardingResponse saveOnboarding(
            @CurrentUserId Long userId, @Valid @RequestBody SaveOnboardingRequest request) {
        return users.saveOnboarding(userId, request);
    }
}
