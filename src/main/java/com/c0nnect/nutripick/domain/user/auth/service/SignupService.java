package com.c0nnect.nutripick.domain.user.auth.service;

import com.c0nnect.nutripick.domain.user.auth.dto.request.SignupRequest;
import com.c0nnect.nutripick.domain.user.auth.dto.response.SignupResponse;
import com.c0nnect.nutripick.domain.user.auth.entity.User;
import org.springframework.stereotype.Service;

@Service
public interface SignupService {
    SignupResponse signup(SignupRequest request);
}
