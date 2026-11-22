package com.c0nnect.nutripick.domain.auth.service;

import com.c0nnect.nutripick.domain.auth.dto.request.LoginRequest;
import com.c0nnect.nutripick.domain.auth.dto.response.LoginResponse;
import com.c0nnect.nutripick.domain.auth.store.RefreshTokenStore;
import com.c0nnect.nutripick.domain.user.entity.User;
import com.c0nnect.nutripick.domain.user.repository.UserRepository;
import com.c0nnect.nutripick.domain.user.dto.response.UserResponse;
import com.c0nnect.nutripick.global.error.CustomException;
import com.c0nnect.nutripick.global.error.ErrorCode;
import com.c0nnect.nutripick.global.security.AccessTokenIssuer;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final AccessTokenIssuer accessTokens;
    private final RefreshTokenStore refreshTokens;
    private final String dummyPasswordHash;

    public LoginService(UserRepository users, PasswordEncoder passwords,
                        AccessTokenIssuer accessTokens, RefreshTokenStore refreshTokens) {
        this.users = users;
        this.passwords = passwords;
        this.accessTokens = accessTokens;
        this.refreshTokens = refreshTokens;
        this.dummyPasswordHash = passwords.encode(UUID.randomUUID().toString());
    }

    public LoginResponse login(LoginRequest request) {
        User user = authenticate(request);
        if (!user.isEmailVerified()) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return new LoginResponse(accessTokens.issue(user.getId()), refreshTokens.issue(user.getId()),
                AccessTokenIssuer.LIFETIME.toSeconds(), UserResponse.from(user));
    }

    private User authenticate(LoginRequest request) {
        var user = users.findByEmail(request.email());
        // Keep the password check even if the address has no local credentials.
        String hash = user.map(User::getPasswordHash).orElse(dummyPasswordHash);
        if (!passwords.matches(request.password(), hash)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return user.filter(account -> account.getPasswordHash() != null)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
    }
}
