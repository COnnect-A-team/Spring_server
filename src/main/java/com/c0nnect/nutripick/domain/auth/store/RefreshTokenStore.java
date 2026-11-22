package com.c0nnect.nutripick.domain.auth.store;

import com.c0nnect.nutripick.global.error.CustomException;
import com.c0nnect.nutripick.global.error.ErrorCode;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshTokenStore {
    private static final Duration LIFETIME = Duration.ofDays(14);
    private final SecureRandom random = new SecureRandom();
    private final StringRedisTemplate redis;

    public String issue(long userId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        try {
            redis.opsForValue().set("auth:refresh-token:" + token, Long.toString(userId), LIFETIME);
        } catch (DataAccessException unavailable) {
            throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE);
        }
        return token;
    }
}
