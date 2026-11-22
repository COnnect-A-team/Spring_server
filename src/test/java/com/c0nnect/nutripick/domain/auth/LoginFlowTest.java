package com.c0nnect.nutripick.domain.auth;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.c0nnect.nutripick.domain.user.entity.User;
import com.c0nnect.nutripick.domain.auth.dto.request.LoginRequest;
import com.c0nnect.nutripick.domain.auth.store.RefreshTokenStore;
import com.c0nnect.nutripick.domain.user.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
class LoginFlowTest {
    private static final String PASSWORD = "correct-password";

    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired JsonMapper json;
    @MockitoBean
    RefreshTokenStore refreshTokens;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void loginAccessTokenReadsOnlyItsOwnerAndRefreshCannotAuthenticateRequests() throws Exception {
        User owner = createUser(true);
        User other = createUser(true);
        when(refreshTokens.issue(owner.getId())).thenReturn("opaque-refresh-token");

        String response = login(owner.getEmail(), PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.user.id").value(owner.getId()))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        var tokens = json.readTree(response);
        mvc.perform(get("/api/v1/users/me").param("userId", other.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.get("accessToken").asString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(owner.getId()))
                .andExpect(jsonPath("$.email").value(owner.getEmail()))
                .andExpect(jsonPath("$.emailVerified").value(true));
        mvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.get("refreshToken").asString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void onlyVerifiedAccountsWithMatchingPasswordsCanReceiveTokens() throws Exception {
        User unverified = createUser(false);
        login(unverified.getEmail(), PASSWORD).andExpect(status().isForbidden());
        login(unverified.getEmail(), "incorrect-password").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        login("unknown@example.test", "incorrect-password").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        verify(refreshTokens, never()).issue(anyLong());
    }

    private User createUser(boolean verified) {
        User user = User.createLocal(UUID.randomUUID() + "@example.test", passwords.encode(PASSWORD), "테스트");
        if (verified) {
            user.verifyEmail();
        }
        return users.saveAndFlush(user);
    }

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(new LoginRequest(email, password))));
    }
}
