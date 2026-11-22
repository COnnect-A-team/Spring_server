package com.c0nnect.nutripick.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.Filter;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import javax.crypto.KeyGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@AutoConfigureMockMvc
@Import(BearerAccessControlTest.ProtectedController.class)
class BearerAccessControlTest {
    @Autowired MockMvc mvc;
    @Autowired AccessTokenIssuer issuer;
    @Autowired JwtEncoder encoder;
    @Autowired WebApplicationContext context;
    @Autowired @Qualifier("springSecurityFilterChain") Filter securityFilter;

    @ParameterizedTest
    @ValueSource(strings = {"signup", "email-verification/send", "email-verification/confirm",
            "login", "google", "password-reset/request", "password-reset/confirm"})
    void onlyPostToEachPublicRoutePassesSecurityAnonymously(String suffix) throws Exception {
        String path = "/api/v1/auth/" + suffix;
        var request = post(path).buildRequest(context.getServletContext());
        var response = new MockHttpServletResponse();
        securityFilter.doFilter(request, response, (req, res) -> res.getWriter().write("allowed"));
        assertThat(response.getContentAsString()).isEqualTo("allowed");
        expectUnauthorized(get(path));
    }

    @Test
    void protectedRoutesRejectMissingTokens() throws Exception {
        expectUnauthorized(post("/api/v1/auth/logout"));
        expectUnauthorized(get("/api/v1/users/me"));
        expectUnauthorized(get("/api/v1/foods"));
    }

    @Test
    void currentUserIdComesFromIssuedToken() throws Exception {
        var result = mvc.perform(get("/test/bearer/identity")
                        .queryParam("userId", "99")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + issuer.issue(7L)))
                .andExpect(status().isOk())
                .andExpect(content().string("7"))
                .andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @Test
    void existingSessionCannotAuthenticateProtectedRequest() throws Exception {
        var session = new MockHttpSession();
        var authentication = UsernamePasswordAuthenticationToken.authenticated("session-user", null, List.of());
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(authentication));
        expectUnauthorized(get("/test/bearer/identity").session(session));
    }

    @Test
    void accessDeniedForAuthenticatedUserReturnsCommonForbiddenResponse() throws Exception {
        mvc.perform(get("/test/bearer/denied")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + issuer.issue(7L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void tokenMustBeCurrentAndHaveAnExpiration() throws Exception {
        expectInvalidToken(sign(encoder, claims -> claims.expiresAt(Instant.now().minusSeconds(2))));
        expectInvalidToken(sign(encoder, claims -> claims.claims(map -> map.remove("exp"))));
        expectInvalidToken(sign(encoder, claims -> claims.notBefore(Instant.now().plusSeconds(30))));
    }

    @Test
    void onlyTrustedIssuerKeyAndAccessTokenPurposeAreAccepted() throws Exception {
        var otherKey = KeyGenerator.getInstance("HmacSHA256").generateKey();
        var otherEncoder = NimbusJwtEncoder.withSecretKey(otherKey).algorithm(MacAlgorithm.HS256).build();
        expectInvalidToken(sign(otherEncoder, claims -> {}));
        expectInvalidToken(sign(encoder, claims -> claims.issuer("other")));
        expectInvalidToken(sign(encoder, claims -> claims.claim("token_use", "refresh")));
        expectInvalidToken("invalid-token");
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "0", "-1", "9223372036854775808"})
    void tokenMustIdentifyAValidUserId(String subject) throws Exception {
        expectInvalidToken(sign(encoder, claims -> claims.subject(subject)));
    }

    private String sign(JwtEncoder signingEncoder, Consumer<JwtClaimsSet.Builder> change) {
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("nutripick").subject("7").claim("token_use", "access")
                .expiresAt(Instant.now().plusSeconds(60));
        change.accept(claims);
        var headers = JwsHeader.with(MacAlgorithm.HS256).build();
        return signingEncoder.encode(JwtEncoderParameters.from(headers, claims.build())).getTokenValue();
    }

    private void expectInvalidToken(String token) throws Exception {
        expectUnauthorized(get("/test/bearer/identity").header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private void expectUnauthorized(MockHttpServletRequestBuilder request) throws Exception {
        mvc.perform(request)
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @TestComponent
    @RestController
    static class ProtectedController {
        @GetMapping("/test/bearer/identity")
        Long identity(@CurrentUserId Long userId) {
            return userId;
        }

        @GetMapping("/test/bearer/denied")
        void denied() {
            throw new AccessDeniedException("Access denied");
        }
    }
}
