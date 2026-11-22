package com.c0nnect.nutripick.global.security;

import java.time.Duration;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration(proxyBeanMethods = false)
public class JwtConfig {
    @Bean
    SecretKey jwtSigningKey(@Value("${nutripick.jwt.secret}") String encodedKey) {
        byte[] key = Base64.getDecoder().decode(encodedKey);
        if (key.length < 32) {
            throw new IllegalArgumentException("NUTRIPICK_JWT_SECRET must contain at least 32 bytes after Base64 decoding");
        }
        return new SecretKeySpec(key, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey key) {
        return NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey key) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator timestamps = new JwtTimestampValidator(Duration.ZERO);
        timestamps.setAllowEmptyExpiryClaim(false);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                timestamps,
                new JwtClaimValidator<>(JwtClaimNames.ISS, AccessTokenIssuer.ISSUER::equals),
                new JwtClaimValidator<>(AccessTokenIssuer.TOKEN_USE, AccessTokenIssuer.ACCESS::equals),
                new JwtClaimValidator<>(JwtClaimNames.SUB, JwtConfig::isUserId)));
        return decoder;
    }

    private static boolean isUserId(Object subject) {
        if (!(subject instanceof String value)) {
            return false;
        }
        try {
            long userId = Long.parseLong(value);
            return userId > 0 && Long.toString(userId).equals(value);
        } catch (NumberFormatException exception) {
            return false;
        }
    }
}
