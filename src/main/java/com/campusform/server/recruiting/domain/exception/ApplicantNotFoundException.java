package com.campusform.server.recruiting.domain.exception;

import org.springframework.http.HttpStatus;

import com.campusform.server.global.exception.BaseException;

/**
 * 지원자를 찾을 수 없을 때 발생하는 예외
 * HTTP 404 Not Found로 매핑됨
 */
public class ApplicantNotFoundException extends BaseException {

    public ApplicantNotFoundException(Long applicantId) {
        super("지원자를 찾을 수 없습니다. applicantId=" + applicantId);
    }

    public ApplicantNotFoundException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }

    @Override
    public String getErrorCode() {
        return "APPLICANT_NOT_FOUND";
    }
}
