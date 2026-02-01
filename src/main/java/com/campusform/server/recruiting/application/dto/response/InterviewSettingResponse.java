package com.campusform.server.recruiting.application.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Step1: 면접 정보 설정 조회 응답 DTO
 */
@Schema(description = "면접 정보 설정 조회 응답")
public record InterviewSettingResponse(
        @Schema(description = "면접 정보 설정 여부", example = "true")
        boolean configured,
        @Schema(description = "면접 시작 날짜", example = "2024-08-01")
        LocalDate startDate,
        @Schema(description = "면접 종료 날짜", example = "2024-08-05")
        LocalDate endDate,
        @Schema(description = "실제 면접이 진행되는 날짜 목록")
        List<LocalDate> interviewDates,
        @Schema(description = "하루 면접 시작 시간", example = "10:00")
        LocalTime startTime,
        @Schema(description = "하루 면접 종료 시간", example = "18:00")
        LocalTime endTime,
        @Schema(description = "한 슬롯(시간)당 최대 배정 가능 지원자 수", example = "3")
        Integer maxApplicantsPerSlot,
        @Schema(description = "한 슬롯(시간)당 배정될 최소 면접관 수", example = "2")
        Integer minInterviewersPerSlot,
        @Schema(description = "한 슬롯(시간)당 배정될 최대 면접관 수", example = "3")
        Integer maxInterviewersPerSlot,
        @Schema(description = "개별 면접 시간(분)", example = "20")
        Integer slotDurationMin,
        @Schema(description = "면접과 면접 사이의 쉬는 시간(분)", example = "5")
        Integer slotBreakMin,
        @Schema(description = "지원자 시간 제출 페이지의 고유 토큰")
        String investigationLinkToken) {

    public static InterviewSettingResponse unconfigured() {
        return new InterviewSettingResponse(false,
                null,
                null,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
