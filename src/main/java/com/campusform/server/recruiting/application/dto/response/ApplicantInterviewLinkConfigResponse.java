package com.campusform.server.recruiting.application.dto.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 지원자 페이지 설정 조회 응답 DTO
 */
@Getter
@RequiredArgsConstructor
public class ApplicantInterviewLinkConfigResponse {

    private final Boolean enabled;
    private final String guidanceText;

    public static ApplicantInterviewLinkConfigResponse of(Boolean enabled, String guidanceText) {
        return new ApplicantInterviewLinkConfigResponse(enabled, guidanceText);
    }
}
