package com.c0nnect.nutripick.domain.auth.dto.response;

public record ConfirmEmailVerificationResponse(String email, boolean emailVerified) {}
