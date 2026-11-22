package com.c0nnect.nutripick.domain.auth.service.verification;

import com.c0nnect.nutripick.domain.user.entity.User;
import com.c0nnect.nutripick.domain.user.repository.UserRepository;
import com.c0nnect.nutripick.global.error.CustomException;
import com.c0nnect.nutripick.global.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class EmailVerificationService {
    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
    private final UserRepository users;
    private final VerificationCodeStore codes;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;

    EmailVerificationService(UserRepository users, VerificationCodeStore codes,
                             ObjectProvider<JavaMailSender> mailSender,
                             @Value("${nutripick.mail.from:}") String from) {
        this.users = users;
        this.codes = codes;
        this.mailSender = mailSender;
        this.from = from;
    }

    public void send(String email) {
        User user = users.findByEmail(email).orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        if (user.isEmailVerified()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || from.isBlank()) {
            throw new CustomException(ErrorCode.EMAIL_SEND_FAILED);
        }

        long userId = user.getId();
        VerificationCodeStore.Delivery delivery = codes.reserve(userId);
        deliverVerificationCode(sender, user, delivery);
        codes.activate(userId, delivery);
    }

    @Transactional
    public String confirm(String email, String token) {
        // Serializing confirmations on the account also prevents a replay if Redis cleanup is delayed.
        User user = users.findForVerificationByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        if (user.isEmailVerified()) {
            throw new CustomException(ErrorCode.INVALID_EMAIL_VERIFICATION_CODE);
        }
        long userId = user.getId();
        String generation = codes.match(userId, token);
        user.verifyEmail();
        consumeCodeAfterCommit(userId, generation);
        return user.getEmail();
    }

    private void deliverVerificationCode(JavaMailSender sender, User user, VerificationCodeStore.Delivery delivery) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(user.getEmail());
        message.setSubject("NutriPick 이메일 인증번호");
        message.setText("이메일 인증번호는 " + delivery.code() + "입니다. 10분 이내에 입력해 주세요.");
        try {
            sender.send(message);
        } catch (MailException exception) {
            try {
                codes.cancel(user.getId(), delivery);
            } catch (DataAccessException unavailable) {
                log.warn("메일 발송 실패 후 Redis 발송 예약을 해제하지 못했습니다.");
            }
            throw new CustomException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    private void consumeCodeAfterCommit(long userId, String generation) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    codes.consume(userId, generation);
                } catch (DataAccessException exception) {
                    // The verified account rejects replays and Redis expires the entry in ten minutes.
                    log.warn("이메일 인증 완료 후 Redis 인증번호 정리에 실패했습니다.");
                }
            }
        });
    }
}
