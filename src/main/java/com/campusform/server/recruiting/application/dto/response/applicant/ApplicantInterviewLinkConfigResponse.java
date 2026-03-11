package com.campusform.server.recruiting.application.dto.response.applicant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 지원자 페이지 설정 조회 응답 DTO
 */
@Schema(description = "지원자 시간 제출 페이지 설정 응답")
@Getter
@RequiredArgsConstructor
public class ApplicantInterviewLinkConfigResponse {

    @Schema(description = "페이지 활성화 여부", example = "true")
    private final Boolean enabled;
    @Schema(description = "페이지 안내 문구", example = "면접 가능 시간을 선택해주세요.")
    private final String guidanceText;

    public static ApplicantInterviewLinkConfigResponse of(Boolean enabled, String guidanceText) {
        return new ApplicantInterviewLinkConfigResponse(enabled, guidanceText);
    }
}
