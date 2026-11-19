package com.c0nnect.nutripick.global.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import com.c0nnect.nutripick.global.error.SecurityExceptionHandler;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;

@SpringBootTest
@Import(ApiSecurityConfigTest.TestSecurityRules.class)
class ApiSecurityConfigTest {
    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void unauthenticatedRequestReturnsCommon401Response() throws Exception {
        mockMvc.perform(get("/security-test"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void accessDeniedReturnsCommon403Response() throws Exception {
        mockMvc.perform(get("/security-test").with(user("member")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestSecurityRules {
        @Bean
        @Order(1)
        SecurityFilterChain securityTestFilterChain(
                HttpSecurity http,
                SecurityExceptionHandler securityExceptionHandler) throws Exception {
            return http
                    .securityMatcher("/security-test/**")
                    .authorizeHttpRequests(authorize -> authorize.anyRequest().hasRole("ADMIN"))
                    .exceptionHandling(exception -> exception
                            .authenticationEntryPoint(securityExceptionHandler)
                            .accessDeniedHandler(securityExceptionHandler)
                    )
                    .build();
        }
    }
}
