package com.campusform.server.project.domain.exception;

import org.springframework.http.HttpStatus;

import com.campusform.server.global.exception.BaseException;

/**
 * Google OAuth 토큰이 만료되었고 Refresh Token으로도 갱신할 수 없을 때 발생하는 예외
 * HTTP 401 Unauthorized로 매핑됨
 */
public class TokenExpiredException extends BaseException {

    public TokenExpiredException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.UNAUTHORIZED;
    }

    @Override
    public String getErrorCode() {
        return "TOKEN_EXPIRED";
    }
}
