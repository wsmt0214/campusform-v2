package com.campusform.server.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 도메인 예외의 공통 추상 클래스
 */
public abstract class BaseException extends RuntimeException {

    protected BaseException(String message) {
        super(message);
    }

    protected BaseException(String message, Throwable cause) {
        super(message, cause);
    }

    /* HTTP 응답 상태 코드 */
    public abstract HttpStatus getHttpStatus();

    /* 클라이언트에게 전달할 에러 코드 */
    public abstract String getErrorCode();

    /* 로깅용 상세 메시지 — 기본은 getMessage()이며 필요 시 오버라이드 */
    public String getDetailMessage() {
        return getMessage();
    }
}
