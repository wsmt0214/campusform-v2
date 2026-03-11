package com.campusform.server.project.domain.exception;

import org.springframework.http.HttpStatus;

import com.campusform.server.global.exception.BaseException;

/**
 * 프로젝트를 찾을 수 없을 때 발생하는 예외
 * HTTP 404 Not Found로 매핑됨
 */
public class ProjectNotFoundException extends BaseException {

    public ProjectNotFoundException(Long projectId) {
        super("프로젝트를 찾을 수 없습니다. projectId=" + projectId);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }

    @Override
    public String getErrorCode() {
        return "PROJECT_NOT_FOUND";
    }
}
