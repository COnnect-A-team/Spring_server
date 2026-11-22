package com.c0nnect.nutripick.domain.auth.controller;

import com.c0nnect.nutripick.domain.auth.dto.request.ConfirmEmailVerificationRequest;
import com.c0nnect.nutripick.domain.auth.dto.request.SendEmailVerificationRequest;
import com.c0nnect.nutripick.domain.auth.dto.response.ConfirmEmailVerificationResponse;
import com.c0nnect.nutripick.domain.auth.dto.response.SendEmailVerificationResponse;
import com.c0nnect.nutripick.domain.auth.service.verification.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/email-verification")
@RequiredArgsConstructor
public class EmailVerificationController {
    private final EmailVerificationService verification;

    @PostMapping("/send")
    public SendEmailVerificationResponse send(@Valid @RequestBody SendEmailVerificationRequest request) {
        verification.send(request.email());
        return new SendEmailVerificationResponse(true);
    }

    @PostMapping("/confirm")
    public ConfirmEmailVerificationResponse confirm(@Valid @RequestBody ConfirmEmailVerificationRequest request) {
        String verifiedEmail = verification.confirm(request.email(), request.token());
        return new ConfirmEmailVerificationResponse(verifiedEmail, true);
    }
}
