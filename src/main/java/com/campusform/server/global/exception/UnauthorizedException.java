package com.campusform.server.global.exception;

/**
 * 인증이 없거나(로그인 필요), 인증 컨텍스트가 유효하지 않을 때 사용하는 예외입니다.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
