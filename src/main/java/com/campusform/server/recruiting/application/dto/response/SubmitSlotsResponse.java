package com.campusform.server.recruiting.application.dto.response;

/**
 * 지원자 면접 가능 슬롯 제출 응답 DTO
 */
public record SubmitSlotsResponse(String message) {

    /**
     * 슬롯 제출 성공 응답 생성
     */
    public static SubmitSlotsResponse success() {
        return new SubmitSlotsResponse("면접 가능 시간이 성공적으로 제출되었습니다.");
    }
}
