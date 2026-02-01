package com.campusform.server.recruiting.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 지원자 면접 가능 슬롯 제출 응답 DTO
 */
@Schema(description = "지원자 면접 가능 시간 제출 응답 (공개 API)")
public record SubmitSlotsResponse(
    @Schema(description = "결과 메시지", example = "면접 가능 시간이 성공적으로 제출되었습니다.")
    String message
) {

    /**
     * 슬롯 제출 성공 응답 생성
     */
    public static SubmitSlotsResponse success() {
        return new SubmitSlotsResponse("면접 가능 시간이 성공적으로 제출되었습니다.");
    }
}
