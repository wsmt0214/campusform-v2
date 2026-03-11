package com.campusform.server.global.exception;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 전역 예외 처리 핸들러
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * @Valid 어노테이션으로 검증 실패 시 발생하는 예외를 처리합니다.
     *        모든 제약을 한 번에 검증하여 MethodArgumentNotValidException 예외를 발생
     *        MethodArgumentNotValidException 는 필드 에러, 전역 에러, 메시지 등을 가지고 있음
     */
    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        // 각 필드의 검증 오류를 맵에 저장
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_ERROR", "입력 데이터 검증에 실패했습니다.", errors));
    }

    /**
     * 인증이 없거나 인증 컨텍스트가 유효하지 않을 때의 예외 처리
     */
    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("UNAUTHORIZED", ex.getMessage(), null));
    }

    /**
     * BaseException을 상속한 모든 도메인 예외를 단일 핸들러로 처리
     */
    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex) {
        log.warn("[{}] {}", ex.getErrorCode(), ex.getDetailMessage());
        return ResponseEntity.status(ex.getHttpStatus())
                .body(new ErrorResponse(ex.getErrorCode(), ex.getMessage(), null));
    }

    /**
     * 잘못된 인자 — 존재하지 않는 리소스 요청, 중복 등
     */
    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("ILLEGAL_ARGUMENT", ex.getMessage(), null));
    }

    /**
     * 잘못된 상태 — 이미 처리된 요청, 상태 전이 불가 등
     */
    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("ILLEGAL_STATE", ex.getMessage(), null));
    }

    /**
     * JPA EntityNotFoundException — 연관 엔티티 미존재 시
     */
    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("NOT_FOUND", ex.getMessage(), null));
    }

    /**
     * 위에서 처리하지 않은 예외 — 서버 내부 오류
     */
    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("처리되지 않은 예외 발생", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.", null));
    }

    /**
     * 에러 응답 DTO
     */
    @Getter
    @AllArgsConstructor
    public static class ErrorResponse {
        private String code; // 에러 코드
        private String message; // 에러 메시지
        private Map<String, String> details; // 상세 에러 정보
    }
}
