package com.c0nnect.nutripick.domain.auth.service;

import com.c0nnect.nutripick.domain.auth.dto.request.SignupRequest;
import com.c0nnect.nutripick.domain.auth.dto.response.SignupResponse;

public interface SignupService {
    SignupResponse signup(SignupRequest request);
}
