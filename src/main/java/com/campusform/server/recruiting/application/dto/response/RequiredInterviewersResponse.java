package com.campusform.server.recruiting.application.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 필수 면접관 목록 조회 응답 DTO
 */
@Schema(description = "필수 면접관 목록 응답")
@Getter
@RequiredArgsConstructor
public class RequiredInterviewersResponse {

    @Schema(description = "필수 면접관으로 지정된 관리자들의 사용자 ID 목록", example = "[1, 2]")
    private final List<Long> adminIds;

    public static RequiredInterviewersResponse of(List<Long> adminIds) {
        return new RequiredInterviewersResponse(adminIds);
    }
}
