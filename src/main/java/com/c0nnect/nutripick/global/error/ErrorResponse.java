package com.c0nnect.nutripick.global.error;

public record ErrorResponse(String code, String message, Object details) {
    public static ErrorResponse from(ErrorCode errorCode, Object details) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), details);
    }
}
