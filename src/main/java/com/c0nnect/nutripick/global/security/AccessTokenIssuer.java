package com.c0nnect.nutripick.global.security;

import java.time.Duration;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
public class AccessTokenIssuer {
    public static final Duration LIFETIME = Duration.ofHours(1);
    static final String ISSUER = "nutripick";
    static final String TOKEN_USE = "token_use";
    static final String ACCESS = "access";

    private final JwtEncoder encoder;

    public AccessTokenIssuer(JwtEncoder encoder) {
        this.encoder = encoder;
    }

    public String issue(long userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(Long.toString(userId))
                .issuedAt(now)
                .expiresAt(now.plus(LIFETIME))
                .claim(TOKEN_USE, ACCESS)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
