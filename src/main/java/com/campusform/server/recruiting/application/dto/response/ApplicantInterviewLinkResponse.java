package com.campusform.server.recruiting.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 지원자 링크 조회 응답 DTO
 */
@Schema(description = "지원자 면접 시간 제출 링크 응답")
@Getter
@RequiredArgsConstructor
public class ApplicantInterviewLinkResponse {

    @Schema(description = "지원자별 고유 토큰", example = "550e8400-e29b-41d4-a716-446655440000")
    private final String token;
    @Schema(description = "완성된 제출 URL", example = "/submit?token=550e8400-e29b-41d4-a716-446655440000")
    private final String url;

    public static ApplicantInterviewLinkResponse of(String token) {
        // TODO: 실제 프론트엔드 URL로 변경 필요
        String url = "/submit?token=" + token;
        return new ApplicantInterviewLinkResponse(token, url);
    }
}
