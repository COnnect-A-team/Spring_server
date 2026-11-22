package com.c0nnect.nutripick.domain.auth.service.verification;

import com.c0nnect.nutripick.global.error.CustomException;
import com.c0nnect.nutripick.global.error.ErrorCode;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class VerificationCodeStore {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_TTL_SECONDS = "600";
    private static final String COOLDOWN_SECONDS = "60";
    private static final String MAX_ATTEMPTS = "5";

    // The previous code stays usable until the SMTP server accepts the replacement.
    private static final DefaultRedisScript<Long> RESERVE = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[2]) == 1 then return 0 end
            if redis.call('HGET', KEYS[1], 'code') == ARGV[2] then return -1 end
            redis.call('SET', KEYS[2], ARGV[1], 'EX', ARGV[3])
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> ACTIVATE = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[2]) ~= ARGV[1] then return 0 end
            redis.call('HSET', KEYS[1], 'generation', ARGV[1], 'code', ARGV[2], 'attempts', 0)
            redis.call('EXPIRE', KEYS[1], ARGV[3])
            redis.call('SET', KEYS[2], 'cooldown', 'EX', ARGV[4])
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> CANCEL = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) end
            return 0
            """, Long.class);
    private static final DefaultRedisScript<String> MATCH = new DefaultRedisScript<>("""
            local code = redis.call('HGET', KEYS[1], 'code')
            if not code then return 'expired' end
            if tonumber(redis.call('HGET', KEYS[1], 'attempts')) >= tonumber(ARGV[2]) then return 'invalid' end
            if code ~= ARGV[1] then
                redis.call('HINCRBY', KEYS[1], 'attempts', 1)
                return 'invalid'
            end
            return redis.call('HGET', KEYS[1], 'generation')
            """, String.class);
    private static final DefaultRedisScript<Long> CONSUME = new DefaultRedisScript<>("""
            if redis.call('HGET', KEYS[1], 'generation') == ARGV[1] then return redis.call('DEL', KEYS[1]) end
            return 0
            """, Long.class);

    private final StringRedisTemplate redis;

    record Delivery(String generation, String code) {}

    Delivery reserve(long userId) {
        String generation = UUID.randomUUID().toString();
        List<String> keys = keys(userId);
        while (true) {
            String code = String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
            long result = execute(RESERVE, keys, generation, code, COOLDOWN_SECONDS);
            if (result == 1) {
                return new Delivery(generation, code);
            }
            if (result == 0) {
                throw new CustomException(ErrorCode.EMAIL_VERIFICATION_RATE_LIMITED);
            }
        }
    }

    void activate(long userId, Delivery delivery) {
        long result = execute(ACTIVATE, keys(userId), delivery.generation(), delivery.code(),
                CODE_TTL_SECONDS, COOLDOWN_SECONDS);
        if (result == 0) {
            throw new CustomException(ErrorCode.EMAIL_VERIFICATION_RATE_LIMITED);
        }
    }

    void cancel(long userId, Delivery delivery) {
        redis.execute(CANCEL, List.of(deliveryKey(userId)), delivery.generation());
    }

    String match(long userId, String code) {
        String result = execute(MATCH, List.of(activeKey(userId)), code, MAX_ATTEMPTS);
        if ("expired".equals(result)) throw new CustomException(ErrorCode.EMAIL_VERIFICATION_TOKEN_EXPIRED);
        if ("invalid".equals(result)) throw new CustomException(ErrorCode.INVALID_EMAIL_VERIFICATION_CODE);
        return result;
    }

    void consume(long userId, String generation) {
        redis.execute(CONSUME, List.of(activeKey(userId)), generation);
    }

    private <T> T execute(DefaultRedisScript<T> script, List<String> keys, String... arguments) {
        try {
            T result = redis.execute(script, keys, (Object[]) arguments);
            if (result == null) throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE);
            return result;
        } catch (DataAccessException unavailable) {
            throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    // The hash tag keeps both keys in the same Redis Cluster slot.
    private static List<String> keys(long userId) {
        return List.of(activeKey(userId), deliveryKey(userId));
    }

    private static String activeKey(long userId) {
        return "auth:email-verification:{" + userId + "}:active";
    }

    private static String deliveryKey(long userId) {
        return "auth:email-verification:{" + userId + "}:delivery";
    }
}
