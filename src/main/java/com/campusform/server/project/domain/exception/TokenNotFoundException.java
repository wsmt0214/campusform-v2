package com.campusform.server.project.domain.exception;

/**
 * Google OAuth 토큰을 찾을 수 없을 때 발생하는 예외입니다.
 * 전역 예외 처리(GlobalExceptionHandler)에서 처리됩니다.
 */
public class TokenNotFoundException extends RuntimeException {

    public TokenNotFoundException(String message) {
        super(message);
    }
}
