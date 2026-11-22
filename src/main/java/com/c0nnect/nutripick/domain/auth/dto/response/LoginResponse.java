package com.c0nnect.nutripick.domain.auth.dto.response;

import com.c0nnect.nutripick.domain.user.dto.response.UserResponse;

public record LoginResponse(String accessToken, String refreshToken, long expiresIn, UserResponse user) {}
