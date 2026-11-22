package com.c0nnect.nutripick.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.c0nnect.nutripick.domain.user.entity.User;
import com.c0nnect.nutripick.domain.user.repository.UserRepository;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class SignupIntegrationTest {
    @Autowired
    private WebApplicationContext context;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JdbcTemplate jdbc;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void signupCreatesAnUnverifiedAccountWithAHashedPassword() throws Exception {
        String email = "SignUp@example.com";
        ResultActions result = signup("SignUp@ExAmPlE.COM").andExpect(status().isCreated());

        User user = userRepository.findByEmail(email).orElseThrow();
        assertThat(user.isEmailVerified()).isFalse();
        assertThat(passwordEncoder.matches("secret123", user.getPasswordHash())).isTrue();
        result.andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.emailVerified").value(false))
                .andExpect(jsonPath("$.requiresEmailVerification").value(true));
    }

    @Test
    void simultaneousSignupsCreateOneAccountAndReturnOneConflict() throws Exception {
        CyclicBarrier start = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> concurrentSignup(start, "simultaneous-signup@Example.COM"));
            var second = executor.submit(() -> concurrentSignup(start, "simultaneous-signup@EXAMPLE.com"));
            assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
        }
    }

    @Test
    void lookupPreservesTheLocalPartAndRejectsAmbiguousLegacyAccounts() throws Exception {
        insertLegacyAccount("Legacy_User@ExAmPle.com");
        assertThat(userRepository.findByEmail("Legacy_User@EXAMPLE.COM").orElseThrow().getEmail())
                .isEqualTo("Legacy_User@ExAmPle.com");
        signup("Legacy_User@example.com").andExpect(status().isConflict());
        signup("legacy_User@example.com").andExpect(status().isCreated());

        insertLegacyAccount("Legacy_User@EXAMPLE.com");
        assertThat(userRepository.findByEmail("Legacy_User@ExAmPle.com")).isEmpty();
        signup("Legacy_User@example.com").andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/auth/email-verification/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"Legacy_User@EXAMPLE.com","token":"012345"}
                                """))
                .andExpect(status().isNotFound());
    }

    private int concurrentSignup(CyclicBarrier start, String email) throws Exception {
        start.await(10, TimeUnit.SECONDS);
        ResultActions result = signup(email);
        int status = result.andReturn().getResponse().getStatus();
        if (status == 409) {
            result.andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));
        }
        return status;
    }

    private void insertLegacyAccount(String email) {
        jdbc.update("""
                insert into users (email, name, email_verified, tutorial_completed, created_at, updated_at)
                values (?, 'legacy', false, false, current_timestamp, current_timestamp)
                """, email);
    }

    private ResultActions signup(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"secret123","name":"테스트"}
                        """.formatted(email)));
    }
}
