package com.campusform.server.identity.domain.exception;

import org.springframework.http.HttpStatus;

import com.campusform.server.global.exception.BaseException;

/**
 * 사용자를 찾을 수 없을 때 발생하는 예외
 * HTTP 404 Not Found로 매핑됨
 */
public class UserNotFoundException extends BaseException {

    public UserNotFoundException(Long userId) {
        super("사용자를 찾을 수 없습니다. userId=" + userId);
    }

    public UserNotFoundException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }

    @Override
    public String getErrorCode() {
        return "USER_NOT_FOUND";
    }
}
