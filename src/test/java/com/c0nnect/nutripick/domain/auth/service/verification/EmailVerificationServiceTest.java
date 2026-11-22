package com.c0nnect.nutripick.domain.auth.service.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.c0nnect.nutripick.domain.user.entity.User;
import com.c0nnect.nutripick.domain.user.repository.UserRepository;
import com.c0nnect.nutripick.global.error.CustomException;
import com.c0nnect.nutripick.global.error.ErrorCode;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = "nutripick.mail.from=no-reply@example.test")
class EmailVerificationServiceTest {
    @Autowired private EmailVerificationService service;
    @Autowired private UserRepository users;
    @Autowired private TransactionTemplate transactions;
    @MockitoBean private VerificationCodeStore codes;
    @MockitoBean private JavaMailSender sender;
    private User user;

    @BeforeEach
    void createUser() {
        user = users.save(User.createLocal(UUID.randomUUID() + "@example.test", "test-hash", "사용자"));
    }

    @Test
    void replacementIsActivatedAfterMailWasAccepted() {
        var delivery = new VerificationCodeStore.Delivery("generation", "000123");
        when(codes.reserve(user.getId())).thenReturn(delivery);

        service.send(user.getEmail());

        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        var order = inOrder(sender, codes);
        order.verify(sender).send(message.capture());
        order.verify(codes).activate(user.getId(), delivery);
        assertThat(message.getValue().getTo()).containsExactly(user.getEmail());
        assertThat(message.getValue().getText()).contains("000123");
    }

    @Test
    void failedDeliveryReleasesOnlyItsOwnReservation() {
        var delivery = new VerificationCodeStore.Delivery("generation", "000123");
        when(codes.reserve(user.getId())).thenReturn(delivery);
        doThrow(new MailSendException("failed")).when(sender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> service.send(user.getEmail())).isInstanceOfSatisfying(CustomException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.EMAIL_SEND_FAILED));

        verify(codes).cancel(user.getId(), delivery);
        verify(codes, never()).activate(anyLong(), any());
    }

    @Test
    void cleanupOutageDoesNotMaskTheMailError() {
        var delivery = new VerificationCodeStore.Delivery("generation", "000123");
        when(codes.reserve(user.getId())).thenReturn(delivery);
        doThrow(new MailSendException("failed")).when(sender).send(any(SimpleMailMessage.class));
        doThrow(new RedisConnectionFailureException("offline")).when(codes).cancel(user.getId(), delivery);

        assertThatThrownBy(() -> service.send(user.getEmail())).isInstanceOfSatisfying(CustomException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.EMAIL_SEND_FAILED));
    }

    @Test
    void rolledBackConfirmationLeavesTheCodeAvailableForRetry() {
        when(codes.match(user.getId(), "012345")).thenReturn("generation");
        transactions.executeWithoutResult(tx -> {
            service.confirm(user.getEmail(), "012345");
            tx.setRollbackOnly();
        });

        verify(codes, never()).consume(anyLong(), anyString());
        assertThat(users.findByEmail(user.getEmail()).orElseThrow().isEmailVerified()).isFalse();
        service.confirm(user.getEmail(), "012345");
        verify(codes).consume(user.getId(), "generation");
        assertThat(users.findByEmail(user.getEmail()).orElseThrow().isEmailVerified()).isTrue();
        assertThatThrownBy(() -> service.confirm(user.getEmail(), "012345")).isInstanceOfSatisfying(CustomException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVALID_EMAIL_VERIFICATION_CODE));
    }
}
