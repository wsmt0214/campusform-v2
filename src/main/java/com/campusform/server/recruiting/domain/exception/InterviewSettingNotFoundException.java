package com.campusform.server.recruiting.domain.exception;

import org.springframework.http.HttpStatus;

import com.campusform.server.global.exception.BaseException;

/**
 * 면접 설정을 찾을 수 없을 때 발생하는 예외
 * HTTP 404 Not Found로 매핑됨
 */
public class InterviewSettingNotFoundException extends BaseException {

    public InterviewSettingNotFoundException(Long projectId) {
        super("면접 정보 설정을 먼저 완료해야 합니다. projectId=" + projectId);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }

    @Override
    public String getErrorCode() {
        return "INTERVIEW_SETTING_NOT_FOUND";
    }
}
