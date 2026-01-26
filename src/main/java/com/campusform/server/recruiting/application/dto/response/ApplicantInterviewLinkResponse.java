package com.campusform.server.recruiting.application.dto.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 지원자 링크 조회 응답 DTO
 */
@Getter
@RequiredArgsConstructor
public class ApplicantInterviewLinkResponse {

    private final String token;
    private final String url;

    public static ApplicantInterviewLinkResponse of(String token) {
        // TODO: 실제 프론트엔드 URL로 변경 필요
        String url = "/submit?token=" + token;
        return new ApplicantInterviewLinkResponse(token, url);
    }
}
