package com.campusform.server.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 인증이 없거나(로그인 필요), 인증 컨텍스트가 유효하지 않을 때 사용하는 예외
 * HTTP 401 Unauthorized로 매핑됨
 */
public class UnauthorizedException extends BaseException {

    public UnauthorizedException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.UNAUTHORIZED;
    }

    @Override
    public String getErrorCode() {
        return "UNAUTHORIZED";
    }
}
