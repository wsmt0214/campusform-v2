package com.campusform.server.recruiting.domain.exception;

import org.springframework.http.HttpStatus;

import com.campusform.server.global.exception.BaseException;

/**
 * 지원자/댓글 등의 상태 전이가 허용되지 않는 경우 발생하는 예외
 * HTTP 400 Bad Request로 매핑됨
 */
public class StatusChangeNotAllowedException extends BaseException {

    public StatusChangeNotAllowedException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public String getErrorCode() {
        return "STATUS_CHANGE_NOT_ALLOWED";
    }
}
