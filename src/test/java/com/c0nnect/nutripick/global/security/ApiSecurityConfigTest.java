package com.c0nnect.nutripick.global.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

import com.c0nnect.nutripick.global.error.SecurityExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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

    @Test
    void nonexistentPathReturnsCommon404Response() throws Exception {
        mockMvc.perform(get("/mvc-review/not-found").with(user("member")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void unsupportedMethodReturnsCommon405ResponseAndAllowHeader() throws Exception {
        mockMvc.perform(post("/mvc-review/get-only")
                        .with(user("member"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(header().string("Allow", containsString("GET")));
    }

    @Test
    void unsupportedContentTypeReturnsCommon415ResponseAndAcceptHeader() throws Exception {
        mockMvc.perform(post("/mvc-review/json-only")
                        .with(user("member"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("not JSON"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(header().string("Accept", containsString("application/json")));
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

        @Bean
        MvcReviewController mvcReviewController() {
            return new MvcReviewController();
        }
    }

    @RestController
    static class MvcReviewController {
        @GetMapping("/mvc-review/get-only")
        String getOnly() {
            return "get only";
        }

        @PostMapping(path = "/mvc-review/json-only", consumes = MediaType.APPLICATION_JSON_VALUE)
        String jsonOnly(@RequestBody String body) {
            return body;
        }
    }
}
