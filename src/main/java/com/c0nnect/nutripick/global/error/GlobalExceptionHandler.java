package com.c0nnect.nutripick.global.error;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 커스텀 예외 처리에 대한 해당 오류 코드의 응답으로 변환
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return createResponse(errorCode, exception.getDetails());
    }

    // 요청 검증 실패를 처리하고, 잘못된 필드와 사유를 응답
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception) {
        List<Map<String, String>> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldError)
                .toList();

        return createResponse(
                ErrorCode.INVALID_REQUEST,
                Map.of("fieldErrors", fieldErrors)
        );
    }

    // 컨트롤러 파라미터 또는 반환값의 검증 실패를 처리하고, 반환값 검증 실패는 서버 오류로 응답
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(
            HandlerMethodValidationException exception) {
        if (exception.isForReturnValue()) {
            log.error("Controller returned a value that failed validation", exception);
            return createResponse(ErrorCode.INTERNAL_SERVER_ERROR, null);
        }

        List<Map<String, String>> violations = exception.getAllErrors()
                .stream()
                .map(this::toValidationError)
                .toList();

        return createResponse(
                ErrorCode.INVALID_REQUEST,
                Map.of("violations", violations)
        );
    }

    // Bean Validation 제약 조건 위반을 처리하고, 위반 항목과 사유를 응답
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception) {
        List<Map<String, String>> violations = exception.getConstraintViolations()
                .stream()
                .map(this::toConstraintViolation)
                .toList();

        return createResponse(
                ErrorCode.INVALID_REQUEST,
                Map.of("violations", violations)
        );
    }

    // JSON 형식이 잘못됐거나 요청 본문을 지정된 형식으로 읽을 수 없는 경우 처리
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException exception) {
        return createResponse(ErrorCode.INVALID_REQUEST, null);
    }

    // 필수 요청 파라미터가 누락된 경우 공통 오류 응답으로 변환
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestParameter(
            MissingServletRequestParameterException exception) {
        return createResponse(
                ErrorCode.INVALID_REQUEST,
                Map.of("parameter", exception.getParameterName())
        );
    }

    // 요청 파라미터 값이 지정된 자료형으로 변환되지 않은 경우 공통 오류 응답으로 변환
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception) {
        return createResponse(
                ErrorCode.INVALID_REQUEST,
                Map.of("parameter", exception.getName())
        );
    }

    // 별도로 처리되지 않은 예외를 기록하고, 내부 정보를 노출하지 않는 일반 서버 오류를 응답
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception exception) {
        log.error("Unhandled exception while processing request", exception);
        return createResponse(ErrorCode.INTERNAL_SERVER_ERROR, null);
    }

    private Map<String, String> toFieldError(FieldError fieldError) {
        return Map.of(
                "field", fieldError.getField(),
                "message", Objects.requireNonNullElse(
                        fieldError.getDefaultMessage(), "값이 올바르지 않습니다."
                )
        );
    }

    private Map<String, String> toConstraintViolation(ConstraintViolation<?> violation) {
        return Map.of(
                "field", violation.getPropertyPath().toString(),
                "message", violation.getMessage()
        );
    }

    private Map<String, String> toValidationError(MessageSourceResolvable error) {
        return Map.of(
                "message", Objects.requireNonNullElse(
                        error.getDefaultMessage(), "값이 올바르지 않습니다."
                )
        );
    }

    private ResponseEntity<ErrorResponse> createResponse(ErrorCode errorCode, Object details) {
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.from(errorCode, details));
    }
}
