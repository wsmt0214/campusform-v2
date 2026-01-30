package com.campusform.server.project.domain.exception;

/**
 * 프로젝트 접근 권한이 없을 때(예: OWNER 전용 API를 ADMIN이 호출) 발생시키는 예외입니다.
 * 전역 예외 처리(GlobalExceptionHandler)에서 403으로 매핑합니다.
 */
public class ProjectAccessDeniedException extends RuntimeException {

    public ProjectAccessDeniedException(String message) {
        super(message);
    }
}
