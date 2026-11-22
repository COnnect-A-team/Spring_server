package com.c0nnect.nutripick.domain.auth.dto.response;

public record SignupResponse(
        Long userId,
        String email,
        boolean emailVerified,
        boolean requiresEmailVerification
) {
}
