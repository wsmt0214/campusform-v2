package com.campusform.server.project.domain.exception;

import org.springframework.http.HttpStatus;

import com.campusform.server.global.exception.BaseException;

/**
 * 프로젝트 접근 권한이 없을 때(예: OWNER 전용 API를 ADMIN이 호출) 발생시키는 예외
 * HTTP 403 Forbidden으로 매핑됨
 */
public class ProjectAccessDeniedException extends BaseException {

    public ProjectAccessDeniedException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.FORBIDDEN;
    }

    @Override
    public String getErrorCode() {
        return "PROJECT_ACCESS_DENIED";
    }
}
