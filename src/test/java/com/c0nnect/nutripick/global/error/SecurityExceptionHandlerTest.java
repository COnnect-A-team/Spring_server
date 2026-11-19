package com.c0nnect.nutripick.global.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import tools.jackson.databind.json.JsonMapper;

class SecurityExceptionHandlerTest {
    private SecurityExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new SecurityExceptionHandler(JsonMapper.builder().build());
        request = new MockHttpServletRequest();
    }

    @Test
    void authenticationFailureReturnsCommon401Response() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.commence(request, response, new BadCredentialsException("invalid credentials"));

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertTrue(response.getContentAsString().contains("\"code\":\"UNAUTHORIZED\""));
    }

    @Test
    void accessDeniedReturnsCommon403Response() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("forbidden"));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertTrue(response.getContentAsString().contains("\"code\":\"FORBIDDEN\""));
    }
}
