package com.c0nnect.nutripick.domain.auth.service.impl;

import com.c0nnect.nutripick.domain.auth.dto.request.SignupRequest;
import com.c0nnect.nutripick.domain.auth.dto.response.SignupResponse;
import com.c0nnect.nutripick.domain.user.entity.User;
import com.c0nnect.nutripick.domain.user.repository.UserRepository;
import com.c0nnect.nutripick.domain.auth.service.SignupService;
import com.c0nnect.nutripick.global.error.CustomException;
import com.c0nnect.nutripick.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SignupServiceImpl implements SignupService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.DUPLICATE_RESOURCE);
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = User.createLocal(request.email(), passwordHash, request.name());
        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            // The repository's insert transaction has rolled back. A concurrent signup may have won.
            if (userRepository.existsByEmail(request.email())) {
                throw new CustomException(ErrorCode.DUPLICATE_RESOURCE);
            }
            throw exception;
        }
        return new SignupResponse(user.getId(), user.getEmail(), user.isEmailVerified(), !user.isEmailVerified());
    }
}
