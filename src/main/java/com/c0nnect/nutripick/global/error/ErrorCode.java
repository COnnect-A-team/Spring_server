package com.c0nnect.nutripick.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청 값이 올바르지 않습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "대상 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "요청한 메서드는 이 경로에서 지원하지 않습니다."),
    NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE, "NOT_ACCEPTABLE", "요청한 응답 형식은 지원하지 않습니다."),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE","이미 존재하는 리소스 입니다."),
    DUPLICATE_COMPARISON_FOOD(HttpStatus.CONFLICT,"DUPLICATE_COMPARISON_FOOD","비교 목록에 이미 추가된 식품입니다."),
    UNPROCESSABLE_ENTITY(HttpStatus.UNPROCESSABLE_CONTENT,"UNPROCESSABLE_ENTITY","요청을 처리할 수 없습니다."),
    COMPARISON_LIST_LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_CONTENT,"COMPARISON_LIST_LIMIT_EXCEEDED","비교 목록은 최대 10개까지 가능합니다."),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS,"RATE_LIMIT_EXCEEDED","API 호출 제한을 초과했습니다."),
    EMAIL_VERIFICATION_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS,"EMAIL_VERIFICATION_RATE_LIMITED","이메일 인증번호 재발송 제한을 초과했습니다."),
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,"EMAIL_SEND_FAILED","이메일 발송 실패"),
    INVALID_EMAIL_VERIFICATION_CODE(HttpStatus.BAD_REQUEST,"INVALID_EMAIL_VERIFICATION_CODE","이메일 인증번호가 올바르지 않습니다."),
    EMAIL_VERIFICATION_TOKEN_EXPIRED(HttpStatus.GONE,"EMAIL_VERIFICATION_TOKEN_EXPIRED","이메일 인증 토큰이 만료되었습니다."),
    PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE,"PAYLOAD_TOO_LARGE","요청 본문 크기가 허용 범위를 초과했습니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"UNSUPPORTED_MEDIA_TYPE","요청의 Content-Type을 지원하지 않습니다."),
    PUBLIC_API_ERROR(HttpStatus.BAD_GATEWAY,"PUBLIC_API_ERROR","공공데이터 api 호출에 실패하였습니다."),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE,"SERVICE_UNAVAILABLE","서버가 일시적으로 요청을 처리할 수 없습니다."),
    AI_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE,"AI_SERVICE_UNAVAILABLE","AI서비스 이용이 불가합니다."),
    INSUFFICIENT_FOODS_FOR_RECOMMENDATION(HttpStatus.UNPROCESSABLE_CONTENT,"INSUFFICIENT_FOODS_FOR_RECOMMENDATION","추천에는 최소 2개의 식품이 필요합니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,"INTERNAL_SERVER_ERROR","예기치 않은 서버 내부 오류"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED,"UNAUTHORIZED","로그인 필요 또는 인증 토큰이 만료되었습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN,"FORBIDDEN","접근 권한이 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
