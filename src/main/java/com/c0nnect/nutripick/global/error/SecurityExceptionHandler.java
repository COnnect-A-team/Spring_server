package com.c0nnect.nutripick.global.error;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final JsonMapper jsonMapper;

    public SecurityExceptionHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    // 인증되지 않았거나 인증에 실패한 요청을 공통 401 오류 응답으로 변환
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        writeErrorResponse(response, ErrorCode.UNAUTHORIZED);
    }

    // 인증된 사용자가 권한이 없는 자원에 접근한 경우 공통 403 오류 응답으로 변환
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception) throws IOException {
        writeErrorResponse(response, ErrorCode.FORBIDDEN);
    }

    private void writeErrorResponse(HttpServletResponse response, ErrorCode errorCode)
            throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(
                response.getOutputStream(),
                ErrorResponse.from(errorCode, null)
        );
    }
}
