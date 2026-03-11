package com.campusform.server.recruiting.domain.exception;

import org.springframework.http.HttpStatus;

import com.campusform.server.global.exception.BaseException;

/**
 * 면접 가능 시간 조회 링크의 토큰이 유효하지 않을 때 발생하는 예외
 * HTTP 400 Bad Request로 매핑됨
 */
public class InvalidInterviewTokenException extends BaseException {

    public InvalidInterviewTokenException() {
        super("유효하지 않은 토큰입니다");
    }

    public InvalidInterviewTokenException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public String getErrorCode() {
        return "INVALID_INTERVIEW_TOKEN";
    }
}
