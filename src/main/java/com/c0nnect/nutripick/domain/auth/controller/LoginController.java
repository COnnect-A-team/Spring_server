package com.c0nnect.nutripick.domain.auth.controller;

import com.c0nnect.nutripick.domain.auth.dto.request.LoginRequest;
import com.c0nnect.nutripick.domain.auth.dto.response.LoginResponse;
import com.c0nnect.nutripick.domain.auth.service.LoginService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LoginController {
    private final LoginService loginService;

    @PostMapping("/api/v1/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return loginService.login(request);
    }
}
