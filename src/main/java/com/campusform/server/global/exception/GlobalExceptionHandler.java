package com.campusform.server.global.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.campusform.server.notification.domain.exception.NotificationAccessDeniedException;
import com.campusform.server.notification.domain.exception.NotificationNotFoundException;
import com.campusform.server.project.domain.exception.ProjectAccessDeniedException;
import com.campusform.server.project.domain.exception.TokenExpiredException;
import com.campusform.server.project.domain.exception.TokenNotFoundException;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 전역 예외 처리 핸들러
 * 
 * @RestControllerAdvice -> 예외를 한 곳에서 처리
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * @Valid 어노테이션으로 검증 실패 시 발생하는 예외를 처리합니다.
     *        각 필드의 검증 오류 메시지를 모아서 반환합니다.
     * 
     * @Valid는 모든 제약을 한 번에 검증하여 MethodArgumentNotValidException 예외를 발생시킵니다.
     *         MethodArgumentNotValidException 는 필드 에러, 전역 에러, 메시지 등을 가지고 있음
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

        ErrorResponse response = new ErrorResponse("Validation Error", "입력 데이터 검증에 실패했습니다.", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 인증이 없거나 인증 컨텍스트가 유효하지 않을 때의 예외 처리
     * 예: 로그인 안했는데 보호 API 호출, principal에 userId 누락 등
     */
    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(UnauthorizedException ex) {
        ErrorResponse response = new ErrorResponse("Unauthorized", ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * 잘못된 인자 예외 처리
     * 예: 존재하지 않는 프로젝트 조회, 중복된 관리자 추가 등
     */
    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorResponse response = new ErrorResponse("Illegal Argument Error", ex.getMessage(), null);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 잘못된 상태 예외 처리
     * 예: 이미 추가된 관리자, 프로젝트 상태 변경 불가 등
     */
    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex) {
        ErrorResponse response = new ErrorResponse("Illegal State Error", ex.getMessage(), null);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * 알림을 찾을 수 없을 때 발생하는 예외 처리
     */
    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleNotificationNotFoundException(NotificationNotFoundException ex) {
        ErrorResponse response = new ErrorResponse("Not Found", ex.getMessage(), null);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * 알림 접근 권한이 없을 때 발생하는 예외 처리
     */
    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleNotificationAccessDeniedException(NotificationAccessDeniedException ex) {
        log.warn("알림 접근 거부: {}", ex.getDetailMessage());
        ErrorResponse response = new ErrorResponse("Forbidden", ex.getMessage(), null);

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * 프로젝트 권한(OWNER 전용 등) 예외 처리
     */
    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleProjectAccessDeniedException(ProjectAccessDeniedException ex) {
        ErrorResponse response = new ErrorResponse("Forbidden", ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Google OAuth 토큰을 찾을 수 없을 때 발생하는 예외 처리
     */
    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleTokenNotFoundException(TokenNotFoundException ex) {
        ErrorResponse response = new ErrorResponse("Token Not Found", ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Google OAuth 토큰이 만료되었을 때 발생하는 예외 처리
     */
    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleTokenExpiredException(TokenExpiredException ex) {
        ErrorResponse response = new ErrorResponse("Token Expired", ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * 위에서 처리하지 않은 모든 예외를 처리합니다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("예외 발생 ", ex);

        ErrorResponse response = new ErrorResponse("Internal Server Error", "서버 내부 오류가 발생했습니다." + ex.getMessage(),
                null);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
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
