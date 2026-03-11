package com.campusform.server.recruiting.application.dto.response.interview;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 토큰으로 조회하는 면접 공개 페이지 설정 응답 DTO
 * 인증 없이 공개 API에서 사용됩니다.
 */
@Schema(description = "면접 공개 페이지 설정 조회 응답 (토큰 기반)")
@Getter
@RequiredArgsConstructor
public class PublicInterviewConfigResponse {

    @Schema(description = "프로젝트 제목", example = "2024년 2학기 신입 부원 모집")
    private final String projectTitle;

    @Schema(description = "면접 가능 시간 제출 페이지 안내 문구", example = "면접 가능 시간을 선택해주세요.")
    private final String guidanceText;

    public static PublicInterviewConfigResponse of(String projectTitle, String guidanceText) {
        return new PublicInterviewConfigResponse(
                projectTitle != null ? projectTitle : "",
                guidanceText != null ? guidanceText : "");
    }
}
