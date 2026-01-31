package com.campusform.server.project.domain.exception;

/**
 * Google OAuth 토큰이 만료되었고 Refresh Token으로도 갱신할 수 없을 때 발생하는 예외입니다.
 * 전역 예외 처리(GlobalExceptionHandler)에서 처리됩니다.
 */
public class TokenExpiredException extends RuntimeException {

    public TokenExpiredException(String message) {
        super(message);
    }
}
