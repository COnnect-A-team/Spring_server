package com.c0nnect.nutripick.domain.user.auth.service.impl;

import com.c0nnect.nutripick.domain.user.auth.dto.request.SignupRequest;
import com.c0nnect.nutripick.domain.user.auth.dto.response.SignupResponse;
import com.c0nnect.nutripick.domain.user.auth.repository.UserRepository;
import com.c0nnect.nutripick.domain.user.auth.service.SignupService;
import com.c0nnect.nutripick.global.error.CustomException;
import com.c0nnect.nutripick.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SignupServiceImpl implements SignupService {
    private final UserRepository userRepository;

    public SignupResponse signup(SignupRequest request){

        if(userRepository.existsByEmail(request.email())){
            throw new CustomException(ErrorCode.DUPLICATE_RESOURCE);
        }

    }
}
