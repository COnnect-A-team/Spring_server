package com.c0nnect.nutripick.domain.user.auth.dto.response;

public record SignupResponse(
        Long id,
        String email,
        String name,
        String accessToken,
        String refreshToken
) {
}
