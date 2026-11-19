package com.c0nnect.nutripick.global.error;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 커스텀 예외에 담긴 오류 코드와 추가 정보를 공통 오류 응답으로 변환
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return createResponse(errorCode, exception.getDetails());
    }

    // Bean Validation 제약 조건 위반을 공통 오류 응답으로 변환
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

    // 예상하지 못한 예외만 기록하고 일반 서버 오류 응답으로 변환
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception exception) {
        log.error("Unhandled exception while processing request", exception);
        return createResponse(ErrorCode.INTERNAL_SERVER_ERROR, null);
    }

    // Spring MVC가 전달한 상태 코드와 헤더를 유지하면서 공통 오류 응답 본문을 반환
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception,
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {
        if (statusCode.is5xxServerError()) {
            log.error("Spring MVC request failed with a server error", exception);
        }

        ErrorResponse errorResponse = ErrorResponse.from(
                errorCodeFor(statusCode),
                detailsFor(exception)
        );

        return super.handleExceptionInternal(
                exception,
                errorResponse,
                headers,
                statusCode,
                request
        );
    }

    // Spring MVC의 HTTP 상태 코드에 맞는 NutriPick 오류 코드를 선택
    private ErrorCode errorCodeFor(HttpStatusCode statusCode) {
        return switch (statusCode.value()) {
            case 400 -> ErrorCode.INVALID_REQUEST;
            case 401 -> ErrorCode.UNAUTHORIZED;
            case 403 -> ErrorCode.FORBIDDEN;
            case 404 -> ErrorCode.NOT_FOUND;
            case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
            case 406 -> ErrorCode.NOT_ACCEPTABLE;
            case 409 -> ErrorCode.DUPLICATE_RESOURCE;
            case 410 -> ErrorCode.EMAIL_VERIFICATION_TOKEN_EXPIRED;
            case 413 -> ErrorCode.PAYLOAD_TOO_LARGE;
            case 415 -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
            case 422 -> ErrorCode.UNPROCESSABLE_ENTITY;
            case 429 -> ErrorCode.RATE_LIMIT_EXCEEDED;
            case 500 -> ErrorCode.INTERNAL_SERVER_ERROR;
            case 502 -> ErrorCode.PUBLIC_API_ERROR;
            case 503 -> ErrorCode.SERVICE_UNAVAILABLE;
            default -> statusCode.is4xxClientError()
                    ? ErrorCode.INVALID_REQUEST
                    : ErrorCode.INTERNAL_SERVER_ERROR;
        };
    }

    // 요청 검증 예외의 필드·파라미터 정보를 details 형식으로 정리
    private Object detailsFor(Exception exception) {
        if (exception instanceof MethodArgumentNotValidException validationException) {
            List<Map<String, String>> fieldErrors = validationException.getBindingResult()
                    .getFieldErrors()
                    .stream()
                    .map(this::toFieldError)
                    .toList();
            return Map.of("fieldErrors", fieldErrors);
        }

        if (exception instanceof HandlerMethodValidationException validationException) {
            if (validationException.isForReturnValue()) {
                return null;
            }

            List<Map<String, String>> violations = validationException.getAllErrors()
                    .stream()
                    .map(this::toValidationError)
                    .toList();
            return Map.of("violations", violations);
        }

        if (exception instanceof MissingServletRequestParameterException missingParameter) {
            return Map.of("parameter", missingParameter.getParameterName());
        }

        if (exception instanceof MethodArgumentTypeMismatchException typeMismatch) {
            return Map.of("parameter", typeMismatch.getName());
        }

        return null;
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
